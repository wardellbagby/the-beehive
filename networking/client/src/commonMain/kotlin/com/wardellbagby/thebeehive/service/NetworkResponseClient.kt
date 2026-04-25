package com.wardellbagby.thebeehive.service

import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode

inline fun <T : Any, R : Any> tryCatchingNetworkError(
  block: () -> R,
  successMapper: (R) -> NetworkResponse.Successful<T>,
): NetworkResponse<T> {
  return try {
    successMapper(block())
  } catch (e: ClientRequestException) {
    NetworkResponse.Failure(failure = e, responseCode = e.response.status)
  } catch (e: ServerResponseException) {
    NetworkResponse.Failure(failure = e, responseCode = e.response.status)
  } catch (e: Exception) {
    NetworkResponse.Failure(failure = e, responseCode = HttpStatusCode.InternalServerError)
  }
}

suspend inline fun <reified T : Any> performHttpCall(
  block: () -> HttpResponse
): NetworkResponse<T> {
  return tryCatchingNetworkError(block) { NetworkResponse.Successful(it.body()) }
}

inline fun performHttpCall(block: () -> HttpResponse): EmptyResponse {
  return tryCatchingNetworkError(block) { NetworkResponse.Successful(Unit) }
}

inline fun <T : Any> initializeWebsocketConnection(block: () -> T): NetworkResponse<T> {
  return tryCatchingNetworkError(block) { NetworkResponse.Successful(it) }
}
