package com.wardellbagby.thebeehive.notifications

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import com.wardellbagby.thebeehive.Filesystem
import com.wardellbagby.thebeehive.HasSerializableState
import com.wardellbagby.thebeehive.ServerConfig
import com.wardellbagby.thebeehive.getLogger
import com.wardellbagby.thebeehive.savableState
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.io.FileInputStream
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@SingleIn(AppScope::class)
@Inject
class PushNotifications(config: ServerConfig, override val filesystem: Filesystem) :
  HasSerializableState {
  private val logger = getLogger()

  var latestFcmToken: String? by savableState(null)

  init {
    val credentials =
      GoogleCredentials.fromStream(
        FileInputStream(Path.of(config.firebaseServiceAccountKeyPath).absolutePathString())
      )
    if (FirebaseApp.getApps().isEmpty()) {
      FirebaseApp.initializeApp(FirebaseOptions.builder().setCredentials(credentials).build())
    }
  }

  suspend fun send(title: String = "Beehive", body: String) {
    if (latestFcmToken == null) {
      logger.warn("No FCM token has been set and a push tried to send!")
      return
    }

    val message =
      Message.builder()
        .setNotification(Notification.builder().setTitle(title).setBody(body).build())
        .setToken(latestFcmToken)
        .build()

    withContext(Dispatchers.IO) {
      try {
        FirebaseMessaging.getInstance().send(message)
        logger.debug("FCM sent")
      } catch (e: Exception) {
        logger.error("FCM send failed: ${e.message}", e)
      }
    }
  }
}
