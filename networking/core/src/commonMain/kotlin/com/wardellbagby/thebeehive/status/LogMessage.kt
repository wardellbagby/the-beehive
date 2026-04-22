package com.wardellbagby.thebeehive.status

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

@OptIn(ExperimentalSerializationApi::class)
@JsonIgnoreUnknownKeys
@Serializable
data class LogMessage(
  val timestamp: Long,
  val level: String,
  val loggerName: String,
  val message: String,
)
