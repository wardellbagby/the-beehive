package com.wardellbagby.thebeehive.notifications

import com.wardellbagby.thebeehive.notifications.IosFcmTokenHolder.TokenStatus
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

internal object IosFcmTokenHolder {
  sealed interface TokenStatus {
    data object Unset : TokenStatus

    data class Set(val token: String?) : TokenStatus
  }

  var token: TokenStatus = TokenStatus.Unset
}

actual class FcmTokenProvider actual constructor() {
  actual suspend fun token(): String? =
    withTimeoutOrNull(5.minutes) {
      while (true) {
        when (val holder = IosFcmTokenHolder.token) {
          is TokenStatus.Set -> {
            return@withTimeoutOrNull holder.token
          }
          else -> delay(5.seconds)
        }
      }
      return@withTimeoutOrNull null
    }
}
