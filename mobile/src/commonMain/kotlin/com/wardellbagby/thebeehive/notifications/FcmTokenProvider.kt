package com.wardellbagby.thebeehive.notifications

expect class FcmTokenProvider() {
  suspend fun token(): String?
}
