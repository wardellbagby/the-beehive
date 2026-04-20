package com.wardellbagby.thebeehive.musicfilter.data

import kotlin.time.Duration
import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class MusicFilterStatusResponse(
  val isEnabled: Boolean,
  val bannedTracks: List<BannedTrackPlay>,
  val nextEviction: Instant?,
  val maxPlaysAllowed: Int,
  val bannedArtistNames: List<String>,
  val slidingWindow: Duration,
)

val MusicFilterStatusResponse.playsLeft: Int
  get() = maxPlaysAllowed - bannedTracks.size
