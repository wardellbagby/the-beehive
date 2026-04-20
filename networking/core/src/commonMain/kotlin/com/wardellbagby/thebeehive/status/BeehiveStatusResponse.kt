package com.wardellbagby.thebeehive.status

import kotlinx.serialization.Serializable

typealias JobId = String

@Serializable data class BeehiveStatusResponse(val ok: Boolean, val jobs: Map<JobId, JobStatus>) {}

@Serializable data class JobStatus(val enabled: Boolean, val errorMessage: String? = null) {}
