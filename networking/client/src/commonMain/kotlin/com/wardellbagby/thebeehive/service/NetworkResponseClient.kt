package com.wardellbagby.thebeehive.service

import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode

inline fun <T : Any> tryCatchingNetworkError(
  block: () -> HttpResponse,
  successMapper: (HttpResponse) -> NetworkResponse.Successful<T>,
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

suspend inline fun <reified T : Any> performNetworkCall(
  block: () -> HttpResponse
): NetworkResponse<T> {
  return tryCatchingNetworkError(block) { NetworkResponse.Successful(it.body()) }
}

inline fun performNetworkCall(block: () -> HttpResponse): EmptyResponse {
  return tryCatchingNetworkError(block) { NetworkResponse.Successful(Unit) }
}
