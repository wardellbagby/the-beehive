package com.wardellbagby.thebeehive.service

import com.wardellbagby.thebeehive.JobManager
import com.wardellbagby.thebeehive.musicfilter.MusicFilterRouteHandler
import com.wardellbagby.thebeehive.musicfilter.data.DeletePlayRequest
import com.wardellbagby.thebeehive.musicfilter.data.MusicFilterStatusResponse
import com.wardellbagby.thebeehive.musicfilter.data.UpdateMusicFilterSettingsRequest
import com.wardellbagby.thebeehive.notifications.NotificationRouteHandler
import com.wardellbagby.thebeehive.notifications.RegisterTokenRequest
import com.wardellbagby.thebeehive.prompt.PromptReply
import com.wardellbagby.thebeehive.status.BeehiveStatusResponse
import com.wardellbagby.thebeehive.status.BeehiveStatusRouteHandler
import com.wardellbagby.thebeehive.status.ToggleJobRequest
import com.wardellbagby.thebeehive.status.ToggleJobResponse
import dev.zacsweers.metro.Inject
import kotlin.uuid.ExperimentalUuidApi

@Inject
class BeehiveServiceServer(
  private val musicFilterRouteHandler: MusicFilterRouteHandler,
  private val beehiveStatusRouteHandler: BeehiveStatusRouteHandler,
  private val notificationRouteHandler: NotificationRouteHandler,
  private val prompter: MobileResponsePrompter,
  private val jobManager: JobManager,
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
}
