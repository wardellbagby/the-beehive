package com.wardellbagby.thebeehive.photodisplay

import com.wardellbagby.thebeehive.Job
import com.wardellbagby.thebeehive.getLogger
import com.wardellbagby.thebeehive.status.JobId
import com.wardellbagby.thebeehive.utils.forResult
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.io.path.readBytes
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

private val IMAGE_DISPLAY_DURATION = 20.seconds

@SingleIn(AppScope::class)
@ContributesIntoSet(AppScope::class)
@Inject
class PhotoDisplayJob(private val clock: Clock, private val manager: PhotoDisplayManager) : Job {
  override val id: JobId = ID

  private val logger = getLogger()

  override suspend fun run() {
    var errorCount = 0
    var lastUpdatedAt: Instant? = null
    while (true) {
      val navigateForward: Boolean? = manager.pendingNavigate()

      forResult {
          val api = manager.waitForTuneshineDiscovery(force = errorCount > 5)
          val state = api.getState()

          if (state.remoteMetadata?.idle == true) {
            if (navigateForward != null) {
              if (navigateForward) {
                manager.advanceToNext()
              } else {
                manager.goToPrevious()
              }
              lastUpdatedAt = clock.now()
              postCurrentImage(api)
            } else {
              val expired =
                lastUpdatedAt == null || (clock.now() - lastUpdatedAt) > IMAGE_DISPLAY_DURATION
              if (expired) {
                manager.advanceToNext()
                lastUpdatedAt = clock.now()
                postCurrentImage(api)
              }
            }
          } else {
            api.deleteImage()
          }
        }
        .onFailure {
          errorCount += 1
          logger.warn("Error when updating photo display. Seen $errorCount errors so far.", it)
        }
        .onSuccess { errorCount = 0 }

      manager.awaitNavigateOrTimeout(2.seconds)
    }
  }

  private suspend fun postCurrentImage(api: TuneshineApi) {
    val file = manager.currentImagePath() ?: return
    api.postImageFile(file.readBytes(), ImageMetadata(idle = true))
  }

  companion object {
    const val ID = "photo-display"
  }
}
