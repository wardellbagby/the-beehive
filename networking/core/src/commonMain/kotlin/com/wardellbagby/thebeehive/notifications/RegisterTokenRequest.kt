package com.wardellbagby.thebeehive.notifications

import kotlinx.serialization.Serializable

@Serializable data class RegisterTokenRequest(val token: String)
