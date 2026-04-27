package com.wardellbagby.thebeehive.notifications

import dev.zacsweers.metro.Inject

@Inject
class NotificationRouteHandler(private val pushNotifications: PushNotifications) {
  fun registerToken(request: RegisterTokenRequest) {
    pushNotifications.latestFcmToken = request.token
  }
}
