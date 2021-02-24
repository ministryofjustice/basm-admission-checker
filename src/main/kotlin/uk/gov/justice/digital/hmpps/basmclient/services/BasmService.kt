package uk.gov.justice.digital.hmpps.basmclient.services

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class BasmService(@Qualifier("basmApiWebClient") private val webClient: WebClient) {

  fun getPrison(prisonId: String): Location? {
    return webClient.get()
      .uri("/api/reference/locations?filter[nomis_agency_id]=$prisonId")
      .header("Accept", "application/vnd.api+json; version=2")
      .retrieve()
      .bodyToMono(PrisonResponseWrapper::class.java)
      .block()!!.data.get(0)
  }

  fun getLocation(locationUUid: String): Location? {
    return webClient.get()
      .uri("/api/reference/locations/$locationUUid")
      .header("Accept", "application/vnd.api+json; version=2")
      .retrieve()
      .bodyToMono(LocationResponseWrapper::class.java)
      .block()!!.data
  }

  fun getMovements(prisonUuid: String, dateFrom: LocalDate, dateTo: LocalDate): MovementResponseWrapper {
    val date_from = dateFrom.format(DateTimeFormatter.ISO_DATE)
    val date_to = dateTo.format(DateTimeFormatter.ISO_DATE)
    return webClient.get()
      .uri("/api/moves?filter[to_location_id]=$prisonUuid&filter[date_from]=$date_from&filter[date_to]=$date_to&filter[status]=requested,accepted,booked,in_transit,completed&sort[by]=date&sort[direction]=asc&page=1&per_page=200&include=profile.person,profile.person_escort_record.flags")
      .header("Accept", "application/vnd.api+json; version=2")
      .retrieve()
      .bodyToMono(MovementResponseWrapper::class.java)
      .block()!!
  }

  fun getPeople(uuidPeopleId: String): People {
    return webClient.get()
      .uri("/api/people/$uuidPeopleId/?include=profiles")
      .header("Accept", "application/vnd.api+json; version=2")
      .retrieve()
      .bodyToMono(PeopleResponseWrapper::class.java)
      .block()!!.data
  }

  //
  fun <T> emptyWhenNotFound(exception: WebClientResponseException): Mono<T> = emptyWhen(exception, NOT_FOUND)
  fun <T> emptyWhen(exception: WebClientResponseException, statusCode: HttpStatus): Mono<T> =
    if (exception.rawStatusCode == statusCode.value()) Mono.empty() else Mono.error(exception)
}

@JsonInclude(NON_NULL)
data class PrisonResponseWrapper(
  val data: List<Location>,
  val meta: ResponseMetadata
)

data class LocationResponseWrapper(
  val data: Location,
  val meta: ResponseMetadata?
)

@JsonInclude(NON_NULL)
data class MovementResponseWrapper(
  val data: List<Movement>,
  val meta: ResponseMetadata?,
  val included: List<Includes>
)

@JsonInclude(NON_NULL)
data class PeopleResponseWrapper(
  val data: People,
  val meta: ResponseMetadata?
)

@JsonInclude(NON_NULL)
data class ResponseMetadata(
  val pagination: ResponsePagination
)

@JsonInclude(NON_NULL)
data class ResponsePagination(
  val per_page: Int,
  val total_pages: Int,
  val total_objects: Int
)

@JsonInclude(NON_NULL)
data class Location(
  val id: String,
  val type: String,
  val attributes: LocationAttributes
)

@JsonInclude(NON_NULL)
data class LocationAttributes(
  val key: String,
  val title: String,
  val location_type: String,
  val nomis_agency_id: String?
)

@JsonInclude(NON_NULL)
data class Movement(
  val id: String,
  val type: String,
  val attributes: MovementAttributes,
  val relationships: Relationships
)

@JsonInclude(NON_NULL)
data class Includes(
  val id: String,
  val type: String,
  val attributes: PeopleAttributes?,
  val relationships: IncludesRelationships?
)

@JsonInclude(NON_NULL)
data class IncludesRelationships (
  val person: Relationship?
)

@JsonInclude(NON_NULL)
data class MovementAttributes(
  val additional_information: String?,
  val date: LocalDate?,
  val date_from: String?,
  val date_to: String?,
  val move_type: String,
  val reference: String?,
  val status: String,
  val time_due: String?,
  val created_at: String?,
  val updated_at: String?

)

/**
 *    "additional_information": null,
"cancellation_reason": null,
"cancellation_reason_comment": null,
"created_at": "2021-01-27T14:00:09+00:00",
"date": "2021-01-27",
"date_from": null,
"date_to": null,
"move_agreed": null,
"move_agreed_by": null,
"move_type": "prison_transfer",
"reference": "MTA3581E",
"rejection_reason": null,
"status": "requested",
"time_due": null,
"updated_at": "2021-01-27T14:02:00+00:00"

 */

@JsonInclude(NON_NULL)
data class Relationships(
  val from_location: Relationship,
  val to_location: Relationship,
  val profile: Relationship,
  val supplier: Relationship?,
)

@JsonInclude(NON_NULL)
data class Relationship(
  val data: RelationshipData?
)

@JsonInclude(NON_NULL)
data class RelationshipData(
  val id: String?,
  val type: String?
)

@JsonInclude(NON_NULL)
data class People(
  val id: String?,
  val type: String?,
  val attributes: PeopleAttributes?,
  val relationships: PeopleRelationships?
)

@JsonInclude(NON_NULL)
data class PeopleAttributes(
  val first_names: String?,
  val last_name: String?,
  val date_of_birth: LocalDate?,
  val prison_number: String?,
  val criminal_records_office: String?,
  val police_national_computer: String?
)

@JsonInclude(NON_NULL)
data class PeopleRelationships(
  val ethnicity: Relationship,
  val gender: Relationship
)