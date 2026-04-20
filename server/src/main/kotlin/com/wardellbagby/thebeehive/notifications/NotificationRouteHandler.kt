package com.wardellbagby.thebeehive.notifications

import dev.zacsweers.metro.Inject

@Inject
class NotificationRouteHandler(private val repo: NotificationRepository) {
  fun registerToken(request: RegisterTokenRequest) {
    repo.addToken(request.token)
  }
}
