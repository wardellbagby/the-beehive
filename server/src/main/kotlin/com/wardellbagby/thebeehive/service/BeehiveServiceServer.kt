package com.wardellbagby.thebeehive.service

import com.wardellbagby.thebeehive.JobManager
import com.wardellbagby.thebeehive.musicfilter.MusicFilterRouteHandler
import com.wardellbagby.thebeehive.musicfilter.data.DeletePlayRequest
import com.wardellbagby.thebeehive.musicfilter.data.MusicFilterStatusResponse
import com.wardellbagby.thebeehive.musicfilter.data.UpdateMusicFilterSettingsRequest
import com.wardellbagby.thebeehive.notifications.NotificationRouteHandler
import com.wardellbagby.thebeehive.notifications.RegisterTokenRequest
import com.wardellbagby.thebeehive.photodisplay.PhotoDisplayNavigateRequest
import com.wardellbagby.thebeehive.photodisplay.PhotoDisplayRouteHandler
import com.wardellbagby.thebeehive.photodisplay.PhotoDisplayStatusResponse
import com.wardellbagby.thebeehive.prompt.PromptReply
import com.wardellbagby.thebeehive.status.BeehiveStatusResponse
import com.wardellbagby.thebeehive.status.BeehiveStatusRouteHandler
import com.wardellbagby.thebeehive.status.LogMessage
import com.wardellbagby.thebeehive.status.ToggleJobRequest
import com.wardellbagby.thebeehive.status.ToggleJobResponse
import dev.zacsweers.metro.Inject
import kotlin.uuid.ExperimentalUuidApi
import kotlinx.coroutines.flow.Flow

@Inject
class BeehiveServiceServer(
  private val musicFilterRouteHandler: MusicFilterRouteHandler,
  private val beehiveStatusRouteHandler: BeehiveStatusRouteHandler,
  private val notificationRouteHandler: NotificationRouteHandler,
  private val photoDisplayRouteHandler: PhotoDisplayRouteHandler,
  private val prompter: MobileResponsePrompter,
  private val jobManager: JobManager,
  private val logs: Flow<LogMessage>,
) : BeehiveService {
  override suspend fun musicFilterStatus(): NetworkResponse<MusicFilterStatusResponse> {
    return NetworkResponse { musicFilterRouteHandler.getMusicFilterStatus() }
  }

  override suspend fun toggleJob(request: ToggleJobRequest): NetworkResponse<ToggleJobResponse> {
    return NetworkResponse {
      ToggleJobResponse(
        success =
          if (request.enabled) {
            jobManager.start(request.jobId)
          } else {
            jobManager.stop(request.jobId)
          }
      )
    }
  }

  override suspend fun updateMusicFilterSettings(
    request: UpdateMusicFilterSettingsRequest
  ): NetworkResponse<MusicFilterStatusResponse> {
    return NetworkResponse { musicFilterRouteHandler.updateMusicFilterSettings(request) }
  }

  override suspend fun deletePlay(request: DeletePlayRequest): EmptyResponse {
    return NetworkResponse { musicFilterRouteHandler.deletePlay(request) }
  }

  override suspend fun photoDisplayStatus(): NetworkResponse<PhotoDisplayStatusResponse> {
    return NetworkResponse { photoDisplayRouteHandler.getStatus() }
  }

  override fun updatePhotoDisplay(): Flow<LogMessage> {
    return photoDisplayRouteHandler.updatePhotoDisplay()
  }

  override suspend fun navigatePhotoDisplay(request: PhotoDisplayNavigateRequest): EmptyResponse {
    return NetworkResponse { photoDisplayRouteHandler.navigate(request.forward) }
  }

  override suspend fun registerToken(request: RegisterTokenRequest): EmptyResponse {
    return NetworkResponse { notificationRouteHandler.registerToken(request) }
  }

  override suspend fun beehiveStatus(): NetworkResponse<BeehiveStatusResponse> {
    return NetworkResponse { beehiveStatusRouteHandler.beehiveStatus() }
  }

  @OptIn(ExperimentalUuidApi::class)
  override suspend fun promptResponse(reply: PromptReply): EmptyResponse {
    return NetworkResponse {
      prompter.onPromptResponse(promptId = reply.promptId, response = reply.response)
    }
  }

  override fun logs(): Flow<LogMessage> {
    return logs
  }
}
