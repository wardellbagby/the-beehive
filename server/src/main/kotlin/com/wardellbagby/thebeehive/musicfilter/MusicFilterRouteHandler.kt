@file:OptIn(ExperimentalUuidApi::class)

package com.wardellbagby.thebeehive.musicfilter

import com.wardellbagby.thebeehive.JobManager
import com.wardellbagby.thebeehive.musicfilter.data.DeletePlayRequest
import com.wardellbagby.thebeehive.musicfilter.data.MusicFilterStatusResponse
import com.wardellbagby.thebeehive.musicfilter.data.UpdateMusicFilterSettingsRequest
import dev.zacsweers.metro.Inject
import kotlin.uuid.ExperimentalUuidApi

@Inject
class MusicFilterRouteHandler(
  private val manager: MusicFilterManager,
  private val jobManager: JobManager,
) {
  fun getMusicFilterStatus(): MusicFilterStatusResponse {
    return MusicFilterStatusResponse(
      isEnabled = jobManager.isStarted(MusicFilterJob.ID),
      bannedTracks = manager.bannedTracks.sortedByDescending { it.playedAt },
      nextEviction = manager.getNextEvictionTime(),
      maxPlaysAllowed = manager.maxPlaysAllowed,
      bannedArtistNames = manager.bannedArtistNames,
      slidingWindow = manager.slidingWindow,
    )
  }

  fun deletePlay(request: DeletePlayRequest) {
    manager.removeTrack(request.id)
  }

  fun updateMusicFilterSettings(
    request: UpdateMusicFilterSettingsRequest
  ): MusicFilterStatusResponse {
    with(request) {
      maxPlaysAllowed?.let { manager.maxPlaysAllowed = it }
      bannedArtistNames?.let { newNames ->
        manager.bannedArtistNames = newNames.map { it.trim() }.filter { it.isNotBlank() }
      }
      slidingWindow?.let { manager.slidingWindow = it }
    }

    return getMusicFilterStatus()
  }
}
