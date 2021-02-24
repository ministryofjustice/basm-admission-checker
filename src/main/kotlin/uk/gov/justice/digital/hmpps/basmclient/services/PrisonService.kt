package uk.gov.justice.digital.hmpps.basmclient.services

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Service
class PrisonService(@Qualifier("prisonApiWebClient") private val webClient: WebClient) {

  private val movementType = object : ParameterizedTypeReference<List<AdmissionMovement>>() {
  }

  fun admittedPrisoners(prisonId: String, fromDate : LocalDate, toDate : LocalDate): List<AdmissionMovement>{

    val fromDateTime = fromDate.atStartOfDay()
    val toDateTime = toDate.plusDays(1).atStartOfDay()

    return webClient.get()
      .uri("/api/movements/$prisonId/in?fromDateTime=$fromDateTime&toDateTime=$toDateTime&allMovements=true")
      .header("Page-Limit", "1000")
      .retrieve()
      .bodyToMono(movementType)
      .block()!!
  }


  fun <T> emptyWhenNotFound(exception: WebClientResponseException): Mono<T> = emptyWhen(exception, NOT_FOUND)
  fun <T> emptyWhen(exception: WebClientResponseException, statusCode: HttpStatus): Mono<T> =
    if (exception.rawStatusCode == statusCode.value()) Mono.empty() else Mono.error(exception)
}

data class AdmissionMovement (
  val offenderNo: String,
  val bookingId: Int,
  val dateOfBirth: LocalDate,
  val firstName: String,
  val lastName: String,
  val fromAgencyId: String?,
  val fromAgencyDescription: String?,
  val toAgencyId: String,
  val toAgencyDescription: String,
  val movementTime: LocalTime,
  val movementDateTime : LocalDateTime,
  val location: String?
  )