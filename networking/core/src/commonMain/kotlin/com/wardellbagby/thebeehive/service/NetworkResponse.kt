package com.wardellbagby.thebeehive.service

import io.ktor.http.HttpStatusCode
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

sealed interface NetworkResponse<out T> {
  data class Successful<T : Any>(val response: T) : NetworkResponse<T>

  data class Failure(val failure: Throwable, val responseCode: HttpStatusCode) :
    NetworkResponse<Nothing>
}

@OptIn(ExperimentalContracts::class)
inline fun <T> NetworkResponse<T>.onSuccess(block: (T) -> Unit): NetworkResponse<T> {
  contract { callsInPlace(block, InvocationKind.AT_MOST_ONCE) }
  if (this is NetworkResponse.Successful) {
    block(response)
  }
  return this
}

@OptIn(ExperimentalContracts::class)
inline fun <T> NetworkResponse<T>.onFailure(
  block: NetworkResponse.Failure.() -> Unit
): NetworkResponse<T> {
  contract { callsInPlace(block, InvocationKind.AT_MOST_ONCE) }
  if (this is NetworkResponse.Failure) {
    this.block()
  }
  return this
}

typealias EmptyResponse = NetworkResponse<Unit>
