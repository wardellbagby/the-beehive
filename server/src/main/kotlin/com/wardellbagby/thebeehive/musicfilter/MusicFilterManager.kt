package com.wardellbagby.thebeehive.musicfilter

import com.wardellbagby.thebeehive.getLogger
import com.wardellbagby.thebeehive.musicfilter.MusicFilterManager.TrackAction.AllowPlay
import com.wardellbagby.thebeehive.musicfilter.MusicFilterManager.TrackAction.Skip
import com.wardellbagby.thebeehive.musicfilter.MusicFilterManager.TrackAction.TemporarilyAllowPlay
import com.wardellbagby.thebeehive.musicfilter.data.BannedTrackPlay
import com.wardellbagby.thebeehive.musicfilter.data.spotify.SpotifyTrack
import com.wardellbagby.thebeehive.musicfilter.data.spotifyId
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@SingleIn(AppScope::class)
@Inject
class MusicFilterManager(private val clock: Clock) {
  sealed interface TrackAction {
    data object AllowPlay : TrackAction

    data class Skip(val reason: String) : TrackAction

    data class TemporarilyAllowPlay(val reason: String) : TrackAction
  }

  private val logger = getLogger()
  private val bannedTracksThatPlayed = mutableListOf<BannedTrackPlay>()
  private var lastSeenPlayingTrack: SpotifyTrack? = null
  private var lastSeenPlayingTrackCount = 0
  var maxPlaysAllowed = 3
  var slidingWindow: Duration = 6.hours
  var bannedArtistNames: List<String> = listOf("Drake", "Kanye West")

  val bannedTracks: List<BannedTrackPlay>
    get() = bannedTracksThatPlayed.toList()

  fun getOldestBannedTrackTime(): Instant? = bannedTracksThatPlayed.firstOrNull()?.playedAt

  fun removeTrack(id: Uuid) {
    bannedTracksThatPlayed.removeIf { it.id == id }
  }

  fun evictTooOldTracks() {
    val now = clock.now()
    val before = bannedTracksThatPlayed.size
    bannedTracksThatPlayed.removeIf { now - it.playedAt >= slidingWindow }
    val evicted = before - bannedTracksThatPlayed.size
    if (evicted > 0) {
      logger.debug("Evicted $evicted tracks from banned list as they're too old.")
    }
  }

  fun isCurrentTrackSameAsLatestBannable(track: SpotifyTrack): Boolean {
    return bannedTracksThatPlayed.lastOrNull()?.spotifyId == track.id
  }

  fun hasCurrentTrackBeenPlayingLongEnoughToBeBanned(track: SpotifyTrack): Boolean {
    if (lastSeenPlayingTrack?.id != track.id) {
      lastSeenPlayingTrack = track
      lastSeenPlayingTrackCount = 1
      logger.debug("New track; resetting last seen track")
      return false
    }
    if (lastSeenPlayingTrackCount < 3) {
      logger.debug("Seen current track $lastSeenPlayingTrackCount times...")
      lastSeenPlayingTrackCount++
      return false
    }
    logger.debug("Seen current track too many times! It's bannable now!")
    return true
  }

  fun getNextEvictionTime(): Instant? = (getOldestBannedTrackTime() ?: return null) + slidingWindow

  private fun getTimeLeftUntilNextEviction(): Duration? =
    (getNextEvictionTime() ?: return null) - clock.now()

  @OptIn(ExperimentalUuidApi::class)
  fun onTrackSeen(track: SpotifyTrack): TrackAction {
    evictTooOldTracks()

    if (track.id == null) {
      // Not actually in Spotify's library so ignore it.
      return AllowPlay
    }

    val isBannableTrack = track.artists.any { it.name in bannedArtistNames }
    if (!isBannableTrack) return AllowPlay

    if (
      isCurrentTrackSameAsLatestBannable(track) && bannedTracksThatPlayed.size == maxPlaysAllowed
    ) {
      // This is the track that took us to the limit, and it's still playing; don't skip it.
      return AllowPlay
    }

    return if (bannedTracksThatPlayed.size < maxPlaysAllowed) {
      if (
        !isCurrentTrackSameAsLatestBannable(track) &&
          hasCurrentTrackBeenPlayingLongEnoughToBeBanned(track)
      ) {
        val timeLeft = getTimeLeftUntilNextEviction()
        val timeLeftMessage = timeLeft?.let { "Next reset in $it" } ?: ""
        val trackMessage = "${track.name} by ${track.artists.joinToString(", ") { it.name }}"
        val message =
          "Allowing \"$trackMessage\". (${bannedTracksThatPlayed.size + 1}/$maxPlaysAllowed) $timeLeftMessage"
            .trim()
        logger.debug(message)
        bannedTracksThatPlayed +=
          BannedTrackPlay(id = Uuid.random(), playedAt = clock.now(), track = track)

        TemporarilyAllowPlay(message)
      }
      AllowPlay
    } else {
      val timeLeft = getTimeLeftUntilNextEviction()
      val message =
        "Disallowing banned track since $maxPlaysAllowed limit has been reached. " +
          "Next expected allowed play after ${timeLeft ?: "unknown"}."
      logger.debug(message)
      Skip(message)
    }
  }
}
