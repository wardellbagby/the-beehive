package com.wardellbagby.thebeehive.musicfilter

import com.wardellbagby.thebeehive.Filesystem
import com.wardellbagby.thebeehive.Job
import com.wardellbagby.thebeehive.ServerConfig
import com.wardellbagby.thebeehive.getLogger
import com.wardellbagby.thebeehive.musicfilter.MusicFilterManager.TrackAction
import com.wardellbagby.thebeehive.musicfilter.data.spotify.SpotifyCurrentlyPlaying
import com.wardellbagby.thebeehive.musicfilter.data.spotify.SpotifyTokenResponse
import com.wardellbagby.thebeehive.musicfilter.data.spotify.SpotifyTokens
import com.wardellbagby.thebeehive.musicfilter.data.spotify.SpotifyTrack
import com.wardellbagby.thebeehive.notifications.PushNotifications
import com.wardellbagby.thebeehive.status.JobId
import com.wardellbagby.thebeehive.workingDirectory
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import java.nio.file.Path
import java.util.Base64
import java.util.UUID
import kotlin.io.path.createParentDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json

@SingleIn(AppScope::class)
@ContributesIntoSet(AppScope::class)
@Inject
class MusicFilterJob(
  filesystem: Filesystem,
  private val manager: MusicFilterManager,
  private val client: HttpClient,
  private val clock: Clock,
  private val config: ServerConfig,
  private val pushNotifications: PushNotifications,
) : Job {
  override val id: JobId = ID

  data class LoginData(val tokens: SpotifyTokens, val expiresIn: Duration)

  data class CurrentPlayingTrackData(val track: SpotifyTrack?, val isActivelyPlaying: Boolean)

  private val logger = getLogger()
  private val tokensFile: Path = filesystem.workingDirectory().resolve("tokens.json")

  private var accessToken: String? = null

  override suspend fun run() {
    var spotifyTokenExpiration = Instant.DISTANT_PAST
    var errorCount = 0

    while (true) {
      try {
        if (isSpotifyTokenExpired(spotifyTokenExpiration)) {
          logger.debug("Spotify token has expired. Fetching a new one.")
          val expiresIn = loginToSpotify()
          spotifyTokenExpiration = getNextExpirationTime(expiresIn)
        }

        checkPlayingTrack()
        delay(5_000)
        errorCount = 0
      } catch (e: Exception) {
        errorCount++
        logger.debug("Seen $errorCount errors so far out of $MAX_ERROR_COUNT allowed.", e)
        if (errorCount >= MAX_ERROR_COUNT) {
          sendPush(
            "An error occurred with the Drake filter that could not be recovered. Next message has log statements."
          )
          delay(1_000)
          sendPush(e.stackTraceToString())
          logger.error("Unable to recover; stopping Drake filter.")
          break
        }
        delay(60_000)
      }
    }
  }

  private fun spotifyAuthHeader(): String {
    val credentials =
      Base64.getEncoder()
        .encodeToString("${config.spotifyClientId}:${config.spotifyClientSecret}".toByteArray())
    return "Basic $credentials"
  }

  private fun saveTokens(tokens: SpotifyTokens) {
    tokensFile.createParentDirectories()
    tokensFile.writeText(json.encodeToString(tokens))
  }

  private fun readTokens(): SpotifyTokens? {
    if (!tokensFile.exists()) return null
    return json.decodeFromString<SpotifyTokens>(tokensFile.readText())
  }

  private suspend fun sendPush(message: String) {
    pushNotifications.send("Music Filter", message)
  }

  private suspend fun loginWithRefreshToken(tokens: SpotifyTokens): LoginData {
    val tokenResponse =
      client
        .submitForm(
          url = "https://accounts.spotify.com/api/token",
          formParameters =
            parameters {
              append("grant_type", "refresh_token")
              append("refresh_token", tokens.refresh)
            },
        ) {
          header(HttpHeaders.Authorization, spotifyAuthHeader())
        }
        .body<SpotifyTokenResponse>()

    val newTokens =
      SpotifyTokens(
        refresh = tokenResponse.refresh_token ?: tokens.refresh,
        access = tokenResponse.access_token,
      )
    accessToken = newTokens.access
    return LoginData(newTokens, tokenResponse.expires_in.seconds)
  }

  private fun getSpotifyAuthCode(): String {
    val state = UUID.randomUUID().toString()
    val scopes =
      listOf(
          "user-read-playback-state",
          "user-modify-playback-state",
          "user-read-currently-playing",
        )
        .joinToString(" ")
    val url =
      "https://accounts.spotify.com/authorize" +
        "?client_id=${config.spotifyClientId}" +
        "&response_type=code" +
        "&redirect_uri=${config.spotifyRedirectUri.encodeURLParameter()}" +
        "&scope=${scopes.encodeURLParameter()}" +
        "&state=$state" +
        "&show_dialog=true"
    logger.debug(url)
    print("Enter redirect URL: ")
    val input = readlnOrNull() ?: error("No input provided")
    val params = input.removePrefix("${config.spotifyRedirectUri}/?").split("&")
    return params.first { it.startsWith("code=") }.removePrefix("code=")
  }

  private suspend fun performInitialLogin(): LoginData {
    val code = getSpotifyAuthCode()
    val tokenResponse =
      client
        .submitForm(
          url = "https://accounts.spotify.com/api/token",
          formParameters =
            parameters {
              append("grant_type", "authorization_code")
              append("code", code)
              append("redirect_uri", config.spotifyRedirectUri)
            },
        ) {
          header(HttpHeaders.Authorization, spotifyAuthHeader())
        }
        .body<SpotifyTokenResponse>()

    val tokens =
      SpotifyTokens(
        refresh =
          tokenResponse.refresh_token ?: error("Missing refresh_token in initial login response"),
        access = tokenResponse.access_token,
      )
    accessToken = tokens.access
    saveTokens(tokens)
    return LoginData(tokens, tokenResponse.expires_in.seconds)
  }

  private suspend fun loginToSpotify(): Duration {
    val savedTokens = readTokens()
    return if (savedTokens != null) {
      try {
        logger.debug("Performing login to Spotify with refresh token")
        val (newTokens, expiresIn) = loginWithRefreshToken(savedTokens)
        saveTokens(newTokens)
        expiresIn
      } catch (e: Exception) {
        logger.debug("Error logging in with refresh token; doing initial login.", e)
        sendPush("Error logging in with refresh token; need to do initial login. Login to server.")
        performInitialLogin().expiresIn
      }
    } else {
      logger.debug("Performing initial login to Spotify")
      performInitialLogin().expiresIn
    }
  }

  private fun getNextExpirationTime(expiresIn: Duration): Instant {
    return clock.now() + (expiresIn - 5.minutes)
  }

  private fun isSpotifyTokenExpired(expiration: Instant): Boolean {
    return clock.now() >= expiration
  }

  private suspend fun getCurrentPlayingTrack(): CurrentPlayingTrackData? {
    val response =
      client.get("https://api.spotify.com/v1/me/player/currently-playing") {
        header(HttpHeaders.Authorization, "Bearer $accessToken")
      }
    if (response.status == HttpStatusCode.NoContent) return null
    val playing = response.body<SpotifyCurrentlyPlaying>()
    return CurrentPlayingTrackData(playing.item, playing.is_playing)
  }

  private suspend fun skipToNext() {
    client.post("https://api.spotify.com/v1/me/player/next") {
      header(HttpHeaders.Authorization, "Bearer $accessToken")
    }
  }

  private suspend fun checkPlayingTrack() {
    val (currentTrack, isPlaying) = getCurrentPlayingTrack() ?: return
    if (isPlaying && currentTrack != null) {
      when (val action = manager.onTrackSeen(currentTrack)) {
        TrackAction.AllowPlay -> Unit // do nothing
        is TrackAction.Skip -> {
          skipToNext()
          sendPush(action.reason)
        }
        is TrackAction.TemporarilyAllowPlay -> {
          sendPush(action.reason)
        }
      }
    }
  }

  companion object {
    const val ID = "music-filter"
    private const val MAX_ERROR_COUNT = 20
  }
}

private val json = Json { ignoreUnknownKeys = true }
