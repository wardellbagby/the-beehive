package com.wardellbagby.thebeehive.musicfilter.data.spotify

import kotlinx.serialization.Serializable

@Serializable
internal data class SpotifyTokenResponse(
  val access_token: String,
  val refresh_token: String? = null,
  val expires_in: Int,
)
