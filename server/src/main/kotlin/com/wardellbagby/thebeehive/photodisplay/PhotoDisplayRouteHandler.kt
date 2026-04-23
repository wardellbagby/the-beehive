package com.wardellbagby.thebeehive.photodisplay

import com.wardellbagby.thebeehive.OneShotExecutor
import com.wardellbagby.thebeehive.status.LogMessage
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow

@Inject
class PhotoDisplayRouteHandler(
  private val manager: PhotoDisplayManager,
  private val oneShotExecutor: OneShotExecutor,
  private val downloadPhotosOneShot: DownloadPhotosOneShot,
) {
  suspend fun getStatus(): PhotoDisplayStatusResponse {
    return manager.getStatus()
  }

  fun updatePhotoDisplay(): Flow<LogMessage> {
    return oneShotExecutor.start(downloadPhotosOneShot)
  }

  fun navigate(forward: Boolean) {
    manager.navigate(forward)
  }
}
