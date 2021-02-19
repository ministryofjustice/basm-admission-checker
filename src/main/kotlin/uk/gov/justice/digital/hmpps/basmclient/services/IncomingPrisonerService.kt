package uk.gov.justice.digital.hmpps.basmclient.services

import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class IncomingPrisonerService(private val basmService: BasmService, private val prisonerService: PrisonService) {
  fun getInwardMovementsToPrisonBetweenDates(prisonId: String, minusDays: LocalDate, now: LocalDate): Movements {
    TODO("Not yet implemented")
  }
}

data class Movements(
  val bookingNo: String,
  val activeFlag: Boolean,
  val offenderNo: String,
  val agencyId: String? = null,
  val locationDescription: String? = null,
  val firstName: String,
  val lastName: String,
  val dateOfBirth: LocalDate,
  val recall: Boolean? = null,
  val legalStatus: String? = null
)
