package com.wardellbagby.thebeehive.notifications

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import com.wardellbagby.thebeehive.ServerConfig
import com.wardellbagby.thebeehive.getLogger
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
class PushNotifications(config: ServerConfig, private val repo: NotificationRepository) {
  private val logger = getLogger()

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
    val tokens = repo.getTokens()
    if (tokens.isEmpty()) return

    val messages = tokens.map { token ->
      Message.builder()
        .setNotification(Notification.builder().setTitle(title).setBody(body).build())
        .setToken(token)
        .build()
    }

    withContext(Dispatchers.IO) {
      try {
        val response = FirebaseMessaging.getInstance().sendEach(messages)
        logger.debug(
          "FCM sent: ${response.successCount} success, ${response.failureCount} failures"
        )
        response.responses.forEachIndexed { i, r ->
          if (!r.isSuccessful) {
            logger.warn("FCM failure for token ${tokens[i]}: ${r.exception?.message}")
          }
        }
      } catch (e: Exception) {
        logger.error("FCM send failed: ${e.message}", e)
      }
    }
  }
}
