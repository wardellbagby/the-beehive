package com.wardellbagby.thebeehive.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class BeehiveFcmService : FirebaseMessagingService() {
  override fun onNewToken(token: String) {}

  override fun onMessageReceived(message: RemoteMessage) {}
}
