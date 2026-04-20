package com.wardellbagby.thebeehive.prompt

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

@Serializable
data class PromptReply
@OptIn(ExperimentalUuidApi::class)
constructor(val promptId: Uuid, val response: PromptResponse)
