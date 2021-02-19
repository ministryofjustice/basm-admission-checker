package uk.gov.justice.digital.hmpps.basmclient.services

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono

@Service
class BasmService(@Qualifier("basmApiWebClient") private val webClient: WebClient) {

  fun getOffender(offenderNo: String): Booking {
    return webClient.get()
      .uri("/api/offenders/$offenderNo")
      .retrieve()
      .bodyToMono(Booking::class.java)
      .block()!!
  }

  fun <T> emptyWhenNotFound(exception: WebClientResponseException): Mono<T> = emptyWhen(exception, NOT_FOUND)
  fun <T> emptyWhen(exception: WebClientResponseException, statusCode: HttpStatus): Mono<T> =
    if (exception.rawStatusCode == statusCode.value()) Mono.empty() else Mono.error(exception)
}
