package com.wardellbagby.thebeehive.client

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.plugins.addDefaultResponseValidation
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

fun createDefaultJson(): Json = Json { ignoreUnknownKeys = true }

fun createDefaultHttpClient(json: Json): HttpClient {
  return HttpClient {
    addDefaultResponseValidation()
    install(ContentNegotiation) { json(json) }
    install(WebSockets)
    install(Logging) {
      logger =
        object : Logger {
          override fun log(message: String) {
            Napier.v(message, tag = "HttpClient")
          }
        }
      level = LogLevel.HEADERS
    }
  }
}
