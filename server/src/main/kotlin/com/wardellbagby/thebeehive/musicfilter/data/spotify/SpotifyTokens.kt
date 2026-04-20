package com.wardellbagby.thebeehive.musicfilter.data.spotify

import kotlinx.serialization.Serializable

@Serializable data class SpotifyTokens(val refresh: String, val access: String)
