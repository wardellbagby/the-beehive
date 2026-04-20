package com.wardellbagby.thebeehive.status

import kotlinx.serialization.Serializable

@Serializable data class ToggleJobRequest(val jobId: JobId, val enabled: Boolean)
