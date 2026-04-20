package com.wardellbagby.thebeehive.notifications

import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

actual class FcmTokenProvider actual constructor() {
  actual suspend fun token(): String? =
      try {
        FirebaseMessaging.getInstance().token.await()
      } catch (_: Exception) {
        null
      }
}
