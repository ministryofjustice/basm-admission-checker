package uk.gov.justice.digital.hmpps.basmclient.services

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.time.LocalDate

@Service
class PrisonService(@Qualifier("prisonApiWebClient") private val webClient: WebClient) {

  fun getOffender(offenderNo: String): Booking {
    return webClient.get()
      .uri("/api/offenders/$offenderNo")
      .retrieve()
      .bodyToMono(Booking::class.java)
      .block()!!
  }

  fun getBooking(bookingId: Long): Booking {
    return webClient.get()
      .uri("/api/bookings/$bookingId?basicInfo=false&&extraInfo=true")
      .retrieve()
      .bodyToMono(Booking::class.java)
      .block()!!
  }

  fun <T> emptyWhenNotFound(exception: WebClientResponseException): Mono<T> = emptyWhen(exception, NOT_FOUND)
  fun <T> emptyWhen(exception: WebClientResponseException, statusCode: HttpStatus): Mono<T> =
    if (exception.rawStatusCode == statusCode.value()) Mono.empty() else Mono.error(exception)
}

data class Booking(
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
