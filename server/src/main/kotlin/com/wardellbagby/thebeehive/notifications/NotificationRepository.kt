package com.wardellbagby.thebeehive.notifications

import com.wardellbagby.thebeehive.Filesystem
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.nio.file.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable private data class TokenStore(val tokens: List<String> = emptyList())

@SingleIn(AppScope::class)
@Inject
class NotificationRepository(filesystem: Filesystem, private val json: Json) {
  private val file: Path = filesystem.getRootDirectory().resolve("fcm-tokens.json")

  @Synchronized
  fun addToken(token: String) {
    val store = readStore()
    if (token !in store.tokens) {
      writeStore(TokenStore(store.tokens + token))
    }
  }

  @Synchronized fun getTokens(): List<String> = readStore().tokens

  private fun readStore(): TokenStore {
    if (!file.exists()) return TokenStore()
    return json.decodeFromString(file.readText())
  }

  private fun writeStore(store: TokenStore) {
    file.createParentDirectories()
    file.writeText(json.encodeToString(store))
  }
}
