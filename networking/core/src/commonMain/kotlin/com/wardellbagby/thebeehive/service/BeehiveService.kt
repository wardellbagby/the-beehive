package com.wardellbagby.thebeehive.service

import com.wardellbagby.thebeehive.musicfilter.data.DeletePlayRequest
import com.wardellbagby.thebeehive.musicfilter.data.MusicFilterStatusResponse
import com.wardellbagby.thebeehive.musicfilter.data.UpdateMusicFilterSettingsRequest
import com.wardellbagby.thebeehive.notifications.RegisterTokenRequest
import com.wardellbagby.thebeehive.photodisplay.PhotoDisplayNavigateRequest
import com.wardellbagby.thebeehive.photodisplay.PhotoDisplayStatusResponse
import com.wardellbagby.thebeehive.prompt.PromptReply
import com.wardellbagby.thebeehive.service.annotations.Body
import com.wardellbagby.thebeehive.service.annotations.GET
import com.wardellbagby.thebeehive.service.annotations.NetworkService
import com.wardellbagby.thebeehive.service.annotations.POST
import com.wardellbagby.thebeehive.service.annotations.WS
import com.wardellbagby.thebeehive.status.BeehiveStatusResponse
import com.wardellbagby.thebeehive.status.LogMessage
import com.wardellbagby.thebeehive.status.ToggleJobRequest
import com.wardellbagby.thebeehive.status.ToggleJobResponse
import kotlinx.coroutines.flow.Flow

@NetworkService
interface BeehiveService {
  @GET("/api/beehive-status") suspend fun beehiveStatus(): NetworkResponse<BeehiveStatusResponse>

  @POST("/api/toggle-job")
  suspend fun toggleJob(@Body request: ToggleJobRequest): NetworkResponse<ToggleJobResponse>

  @GET("/api/music-filter")
  suspend fun musicFilterStatus(): NetworkResponse<MusicFilterStatusResponse>

  @POST("/api/music-filter/settings")
  suspend fun updateMusicFilterSettings(
    @Body request: UpdateMusicFilterSettingsRequest
  ): NetworkResponse<MusicFilterStatusResponse>

  @POST("/api/music-filter/delete-play")
  suspend fun deletePlay(@Body request: DeletePlayRequest): EmptyResponse

  @GET("/api/photo-display")
  suspend fun photoDisplayStatus(): NetworkResponse<PhotoDisplayStatusResponse>

  @WS("/api/photo-display/update")
  suspend fun updatePhotoDisplay(): NetworkResponse<Flow<LogMessage>>

  @POST("/api/photo-display/navigate")
  suspend fun navigatePhotoDisplay(@Body request: PhotoDisplayNavigateRequest): EmptyResponse

  @POST("/api/notifications/register-token")
  suspend fun registerToken(@Body request: RegisterTokenRequest): EmptyResponse

  @POST("/api/prompt-response") suspend fun promptResponse(@Body reply: PromptReply): EmptyResponse

  @WS("/api/logs") suspend fun logs(): NetworkResponse<Flow<LogMessage>>
}
