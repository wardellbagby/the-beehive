package com.wardellbagby.thebeehive.utils

import kotlin.coroutines.cancellation.CancellationException

/**
 * A coroutines-aware version of [runCatching] that will rethrow [CancellationException]s. Prefer it
 * over [runCatching] in order to avoid accidentally stopping coroutine cancellations.
 */
inline fun <T> forResult(block: () -> T): Result<T> {
  return try {
    Result.success(block())
  } catch (e: CancellationException) {
    throw e
  } catch (e: Throwable) {
    Result.failure(e)
  }
}

/**
 * Run a block of code totally ignoring exceptions. Should be very rarely used; mostly to close
 * resources in the case where you can't recover from that action failing.
 */
inline fun finallyIgnoringAll(block: () -> Any?) {
  forResult { block() }
}
