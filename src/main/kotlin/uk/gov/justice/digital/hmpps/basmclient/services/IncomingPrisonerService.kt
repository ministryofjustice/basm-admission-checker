package uk.gov.justice.digital.hmpps.basmclient.services

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class IncomingPrisonerService(private val basmService: BasmService,
                              private val prisonerSearchService: PrisonerSearchService,
                              private val offenderSearchService: OffenderSearchService,
                              private val prisonService: PrisonService) {
  fun getInwardMovementsToPrisonBetweenDates(prisonId: String, fromDate: LocalDate, toDate: LocalDate): Movements {
    // lookup prison

    val prison = basmService.getPrison(prisonId)

    if (prison != null) {
      val admittedPrisoners = prisonService.admittedPrisoners(prisonId, fromDate, toDate)
        .map{ it.offenderNo to it }.toMap()

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
            var matchedPrisoners : List<PrisonerSearchResult> = listOf()
            if (police_national_computer != null) {
              matchedPrisoners = matchPrisoners(first_names, last_name, police_national_computer, date_of_birth)
            }
            val probationMatchedOffenders = matchedOffenders.matches
            if (matchedPrisoners.isEmpty() && probationMatchedOffenders.isNotEmpty() && probationMatchedOffenders[0].offender.otherIds.pncNumber != null) {
              // try with Delius PNC
              matchedPrisoners = matchPrisoners(first_names, last_name, probationMatchedOffenders[0].offender.otherIds.pncNumber, date_of_birth)
            }
            if (matchedPrisoners.isEmpty() && prison_number != null) {
              // try with NOMS ID
              matchedPrisoners = matchPrisoners(first_names, last_name, prison_number, date_of_birth)
            }
            if (matchedPrisoners.isEmpty() && criminal_records_office != null) {
              matchedPrisoners = matchPrisoners(first_names, last_name, criminal_records_office, date_of_birth)
            }
            if (matchedPrisoners.isEmpty()) {
              matchedPrisoners = matchPrisoners(first_names, last_name, null, null)
            }

            val matchedAdmission = findAdmissionMovement(matchedPrisoners, admittedPrisoners)
            if (matchedAdmission != null) {
              nonMatched.remove(matchedAdmission.offenderNo)
            }

            PrisonMovement(
              true,
              matchedPrisoners.isNotEmpty(),
              probationMatchedOffenders.isNotEmpty(),
              matchedAdmission != null,
              matchedPrisoners,
              basmRecord,
              probationMatchedOffenders,
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

        val prisonerMatch = matchPrisoners(null, null, it.key, null)
        PrisonMovement(
          false,
          true,
          offenderMatch.matches.isNotEmpty(),
          true,
          prisonerMatch,
          null,
          offenderMatch.matches,
          it.value
        )
      }


      return Movements(prison.attributes.nomis_agency_id, movements + nonMatchedAdmissions)
    }

    return Movements("NIN", listOf())

  }

  private fun findAdmissionMovement(matchedPrisoners: List<PrisonerSearchResult>,
    admittedPrisoners: Map<String, AdmissionMovement>
  ): AdmissionMovement? {
    var matchedAdmission: AdmissionMovement? = null
    if (matchedPrisoners.isNotEmpty()) {
      matchedAdmission = admittedPrisoners.get(matchedPrisoners[0].prisonerNumber)
    }
    return matchedAdmission
  }

  private fun matchPrisoners(firstNames : String?, lastName: String?, id : String?, dob :LocalDate?): List<PrisonerSearchResult> {
    return prisonerSearchService.matchPrisoner(
      SearchRequest(
        firstNames, lastName, id, dob
      )
    )
  }
}

@JsonInclude(NON_NULL)
data class Movements(
  val prisonId: String? = null,
  val movements: List<PrisonMovement>
)

@JsonInclude(NON_NULL)
data class PrisonMovement(

  val foundInBasm : Boolean,
  val foundInNomis : Boolean,
  val foundInDelius: Boolean,
  val matchedAdmission: Boolean,

  val nomisRecords: List<PrisonerSearchResult>?,
  val basmRecord : BasmRecord?,
  val deliusRecord: List<OffenderMatch>?,
  val admissionMovement: AdmissionMovement?
)

@JsonInclude(NON_NULL)

data class BasmRecord (
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
data class PrisonerSearchRecord (
  val firstName: String?,
  val lastName: String?,
  val dateOfBirth: LocalDate?,
  val nomsId: String?,
  val pnc : String?,
  val cro: String?
)

@JsonInclude(NON_NULL)
data class OffenderSearchRecord (
  val firstName: String?,
  val lastName: String?,
  val dateOfBirth: LocalDate?,
  val crn: String?,
  val pnc : String?,
)
