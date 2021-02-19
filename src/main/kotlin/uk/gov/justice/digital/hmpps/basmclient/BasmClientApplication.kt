package uk.gov.justice.digital.hmpps.basmclient

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class BasmClientApplication

fun main(args: Array<String>) {
  runApplication<BasmClientApplication>(*args)
}
