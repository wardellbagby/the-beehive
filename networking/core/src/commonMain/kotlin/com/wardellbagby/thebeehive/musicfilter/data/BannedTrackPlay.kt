package com.wardellbagby.thebeehive.musicfilter.data

import com.wardellbagby.thebeehive.musicfilter.data.spotify.SpotifyTrack
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

@Serializable
data class BannedTrackPlay
@OptIn(ExperimentalUuidApi::class)
constructor(val id: Uuid, val playedAt: Instant, val track: SpotifyTrack)

val BannedTrackPlay.spotifyId: String?
  get() = track.id
