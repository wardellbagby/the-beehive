package com.wardellbagby.thebeehive.musicfilter.data.spotify

import kotlinx.serialization.Serializable

@Serializable
internal data class SpotifyCurrentlyPlaying(val is_playing: Boolean, val item: SpotifyTrack? = null)
