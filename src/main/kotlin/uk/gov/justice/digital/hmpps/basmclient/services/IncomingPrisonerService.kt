package uk.gov.justice.digital.hmpps.basmclient.services

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class IncomingPrisonerService(
  private val basmService: BasmService,
  private val prisonerSearchService: PrisonerSearchService,
  private val offenderSearchService: OffenderSearchService,
  private val prisonService: PrisonService
) {
  fun getInwardMovementsToPrisonBetweenDates(prisonId: String, fromDate: LocalDate, toDate: LocalDate): Movements {
    // lookup prison

    val prison = basmService.getPrison(prisonId)

    if (prison != null) {
      val admittedPrisoners = prisonService.admittedPrisoners(prisonId, fromDate, toDate)
        .map { it.offenderNo to it }.toMap()

      val movementData = basmService.getMovements(prison.id, fromDate, toDate)
      val includes = movementData.included.groupBy { it.type }.mapValues { (_, v) -> v.associateBy { it.id } }

      val nonMatched = HashMap(admittedPrisoners)

      val movements = movementData.data
        .filter {
          it.relationships.profile.data?.id != null && it.relationships.from_location.data?.id != null && it.relationships.to_location.data?.id != null
            && includes.get("profiles")?.get(it.relationships.profile.data.id)?.relationships?.person?.data?.id != null
        }
        .map {
          val fromLocationUuid = it.relationships.from_location.data?.id
          val toLocationUuid = it.relationships.to_location.data?.id
          val profileId = it.relationships.profile.data?.id!!
          val personId = includes.get("profiles")?.get(profileId)?.relationships?.person?.data?.id!!
          val personData = includes.get("people")?.get(personId)?.attributes!!

          val basmRecord = BasmRecord(
            personData,
            basmService.getLocation(fromLocationUuid!!)?.attributes?.title!!,
            basmService.getLocation(toLocationUuid!!)?.attributes?.title!!,
            it.attributes.date,
            it.attributes.date_from,
            it.attributes.date_to,
            it.attributes.time_due,
            it.attributes.move_type,
            it.attributes.reference,
            it.attributes.status,
            it.attributes.additional_information,
            it.attributes.created_at,
            it.attributes.updated_at
          )

          with(personData) {
            // Delius
            val matchedOffenders = offenderSearchService.matchProbationOffender(
              MatchRequest(
                first_names!!,
                last_name!!,
                date_of_birth!!,
                prison_number,
                true,
                police_national_computer,
                criminal_records_office
              )
            )

            // look in Nomis for record
            var matchedPrisoners = prisonerSearchService.matchPrisoner(
              SearchRequest(
                first_names,
                last_name,
                date_of_birth,
                prison_number,
                police_national_computer,
                criminal_records_office
              )
            )

            val matchedAdmission = findAdmissionMovement(matchedPrisoners.matches, admittedPrisoners)
            if (matchedAdmission != null) {
              nonMatched.remove(matchedAdmission.offenderNo)
            }

            PrisonMovement(
              true,
              matchedPrisoners.matches.isNotEmpty(),
              matchedOffenders.matches.isNotEmpty(),
              matchedAdmission != null,
              matchedPrisoners,
              basmRecord,
              matchedOffenders,
              matchedAdmission
            )
          }
        }


      val nonMatchedAdmissions = nonMatched.map {
        val offenderMatch = offenderSearchService.matchProbationOffender(
          MatchRequest(
            it.value.firstName,
            it.value.firstName,
            it.value.dateOfBirth,
            it.key,
            true,
            null,
            null
          )
        )

        val prisonerMatch = prisonerSearchService.matchPrisoner(
          SearchRequest(
            it.value.firstName,
            it.value.firstName,
            it.value.dateOfBirth,
            it.key,
            null,
            null
          )
        )
        PrisonMovement(
          false,
          prisonerMatch.matches.isNotEmpty(),
          offenderMatch.matches.isNotEmpty(),
          true,
          prisonerMatch,
          null,
          offenderMatch,
          it.value
        )
      }

      var numNotRecordedInBasm = 0
      var numNotAdmitted = 0
      var numNotFoundInNomis = 0
      var numNotFoundInDelius = 0
      var moveTypes: MutableMap<String, Int> = mutableMapOf()

      val allMovements = movements + nonMatchedAdmissions
      allMovements.forEach {
        if (!it.foundInBasm) numNotRecordedInBasm++
        if (!it.foundInDelius) numNotFoundInDelius++
        if (!it.foundInNomis) numNotFoundInNomis++
        if (!it.matchedAdmission) numNotAdmitted++
        if (it.basmRecord != null) {
          val moveType = moveTypes.get(it.basmRecord.moveType)
          if (moveType != null) {
            moveTypes.put(it.basmRecord.moveType, moveType.inc())
          } else {
            moveTypes.put(it.basmRecord.moveType, 1)
          }
        }
      }

      return Movements(
        prison.attributes.nomis_agency_id,
        Stats(
          allMovements.size,
          numNotRecordedInBasm,
          numNotAdmitted,
          numNotFoundInNomis,
          numNotFoundInDelius,
          moveTypes
        ),
        allMovements
      )
    }

    return Movements("Not Found", Stats(), listOf())
  }

  private fun findAdmissionMovement(
    matchedPrisoners: List<PrisonerMatch>,
    admittedPrisoners: Map<String, AdmissionMovement>
  ): AdmissionMovement? {
    var matchedAdmission: AdmissionMovement? = null
    if (matchedPrisoners.isNotEmpty()) {
      matchedAdmission = admittedPrisoners.get(matchedPrisoners[0].prisoner.prisonerNumber)
    }
    return matchedAdmission
  }

}

data class Stats(
  val totalRecords: Int = 0,
  val numNotRecordedInBasm: Int = 0,
  val numNotAdmitted: Int = 0,
  val numNotFoundInNomis: Int = 0,
  val numNotFoundInDelius: Int = 0,
  val moveTypes: Map<String, Int> = mapOf()
)

@JsonInclude(NON_NULL)
data class Movements(
  val prisonId: String? = null,
  val stats: Stats,
  val movements: List<PrisonMovement>
)

@JsonInclude(NON_NULL)
data class PrisonMovement(

  val foundInBasm: Boolean,
  val foundInNomis: Boolean,
  val foundInDelius: Boolean,
  val matchedAdmission: Boolean,

  val nomisRecords: PrisonerSearchResultContent,
  val basmRecord: BasmRecord?,
  val deliusRecord: OffenderMatches,
  val admissionMovement: AdmissionMovement?
)

@JsonInclude(NON_NULL)

data class BasmRecord(
  val person: PeopleAttributes?,
  val fromLocation: String,
  val toLocation: String,
  val date: LocalDate?,
  val dateFrom: String?,
  val dateTo: String?,
  val timeDue: String?,
  val moveType: String,
  val reference: String?,
  val status: String,
  val additionalInformation: String?,
  val createdAt: String?,
  val updatedAt: String?
)

@JsonInclude(NON_NULL)
data class PrisonerSearchRecord(
  val firstName: String?,
  val lastName: String?,
  val dateOfBirth: LocalDate?,
  val nomsId: String?,
  val pnc: String?,
  val cro: String?
)

@JsonInclude(NON_NULL)
data class OffenderSearchRecord(
  val firstName: String?,
  val lastName: String?,
  val dateOfBirth: LocalDate?,
  val crn: String?,
  val pnc: String?,
)
