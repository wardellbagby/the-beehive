package com.wardellbagby.thebeehive.musicfilter.data.spotify

import kotlinx.serialization.Serializable

@Serializable
data class SpotifyTrack(
  val id: String?,
  val name: String,
  val artists: List<SpotifyArtist>,
  val album: SpotifyAlbum,
)
