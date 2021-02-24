package uk.gov.justice.digital.hmpps.basmclient.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.format.annotation.DateTimeFormat.ISO.DATE
import org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.basmclient.ErrorResponse
import uk.gov.justice.digital.hmpps.basmclient.services.IncomingPrisonerService
import uk.gov.justice.digital.hmpps.basmclient.services.Movements
import java.time.LocalDate
import javax.validation.constraints.Size

@RestController
@Validated
@RequestMapping("/prison-movements", produces = [MediaType.APPLICATION_JSON_VALUE])
class ClientResource(private val incomingPrisonerService: IncomingPrisonerService) {
  @GetMapping("/id/{prisonId}")
  @Operation(
    summary = "Get specified prison inward movements from BaSM",
    description = "Information on a specific prison inward movements",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Incoming Movements Information Returned",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = Movements::class))]
      ),
      ApiResponse(
        responseCode = "404",
        description = "Prison ID not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      )
    ]
  )
  fun getMovementsFromPrisonId(
    @Schema(description = "Prison ID", example = "PVI", required = true)
    @PathVariable @Size(max = 3, min = 3, message = "Prison ID must b e 3 characters") prisonId: String,
    @RequestParam("dateFrom", required = false) @DateTimeFormat(iso = DATE) dateFrom : LocalDate?,
    @RequestParam("dateTo", required = false) @DateTimeFormat(iso = DATE) dateTo : LocalDate?
  ): Movements {

    var from = LocalDate.now()
    var to = LocalDate.now()

    if (dateFrom != null) {
      from = dateFrom
    }

    if (dateTo != null) {
      to = dateTo
    }
    return incomingPrisonerService.getInwardMovementsToPrisonBetweenDates(prisonId, from, to)
  }
}
