package com.wardellbagby.thebeehive.musicfilter.data

import kotlin.time.Duration
import kotlinx.serialization.Serializable

@Serializable
data class UpdateMusicFilterSettingsRequest(
  val maxPlaysAllowed: Int? = null,
  val bannedArtistNames: List<String>? = null,
  val slidingWindow: Duration? = null,
)
