package com.wardellbagby.thebeehive.service

import com.wardellbagby.thebeehive.utils.forResult
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond

inline fun <T : Any> NetworkResponse(
  onFailure: (Throwable) -> HttpStatusCode = { HttpStatusCode.InternalServerError },
  block: () -> T,
): NetworkResponse<T> {
  return forResult { block() }
    .fold(
      onSuccess = { NetworkResponse.Successful(it) },
      onFailure = { NetworkResponse.Failure(failure = it, responseCode = onFailure(it)) },
    )
}

suspend inline fun <reified T : Any> ApplicationCall.sendNetworkResponse(
  response: NetworkResponse<T>
) {
  response
    .onSuccess { respond(it) }
    .onFailure { this@sendNetworkResponse.response.status(responseCode) }
}
