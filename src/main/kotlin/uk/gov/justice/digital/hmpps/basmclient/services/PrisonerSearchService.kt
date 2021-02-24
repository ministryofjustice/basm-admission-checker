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

@Service
class PrisonerSearchService(@Qualifier("prisonerSearchApiWebClient") private val webClient: WebClient) {


  fun matchPrisoner(searchRequest: SearchRequest): List<PrisonerSearchResult>{
    return webClient.post()
      .uri("/global-search")
      .bodyValue(searchRequest)
      .retrieve()
      .bodyToMono(PrisonerSearchResultContent::class.java)
      .block()!!.content
  }


  fun <T> emptyWhenNotFound(exception: WebClientResponseException): Mono<T> = emptyWhen(exception, NOT_FOUND)
  fun <T> emptyWhen(exception: WebClientResponseException, statusCode: HttpStatus): Mono<T> =
    if (exception.rawStatusCode == statusCode.value()) Mono.empty() else Mono.error(exception)
}

data class SearchRequest (
  val firstName: String?,
  val lastName: String?,
  val prisonerIdentifier : String?,
  val dateOfBirth: LocalDate?
)

data class PrisonerSearchResultContent (
  val content : List<PrisonerSearchResult>
  )

@JsonInclude(NON_NULL)
data class PrisonerSearchResult (
  val prisonerNumber: String,
  val pncNumber: String?,
  val pncNumberCanonicalShort: String?,
  val pncNumberCanonicalLong: String?,
  val croNumber: String?,
  val bookingId: String?,
  val bookNumber: String?,
  val firstName: String,
  val middleNames: String?,
  val lastName: String,
  val dateOfBirth: LocalDate,
  val gender: String,
  val ethnicity: String?,
  val prisonId: String?,
  val recall: Boolean,
  val receptionDate: LocalDate?
)