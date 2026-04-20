package com.wardellbagby.thebeehive.notifications

import com.wardellbagby.thebeehive.notifications.IosFcmTokenHolder.TokenStatus

/** Called from Swift (AppDelegate) after Firebase delivers the FCM registration token. */
object FcmTokenBridge {
  fun setToken(token: String) {
    IosFcmTokenHolder.token = TokenStatus.Set(token)
  }
}
