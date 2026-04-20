package com.wardellbagby.thebeehive.musicfilter.data

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

@OptIn(ExperimentalUuidApi::class) @Serializable data class DeletePlayRequest(val id: Uuid) {}
