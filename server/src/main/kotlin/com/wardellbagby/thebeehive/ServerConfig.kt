package com.wardellbagby.thebeehive

data class ServerConfig(
  val serverPort: Int,
  val spotifyClientId: String,
  val spotifyClientSecret: String,
  val spotifyRedirectUri: String,
  val firebaseServiceAccountKeyPath: String
)
