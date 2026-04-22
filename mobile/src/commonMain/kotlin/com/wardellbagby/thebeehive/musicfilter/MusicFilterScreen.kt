package com.wardellbagby.thebeehive.musicfilter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxDefaults
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.ColorImage
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import com.wardellbagby.thebeehive.LocalClock
import com.wardellbagby.thebeehive.musicfilter.data.BannedTrackPlay
import com.wardellbagby.thebeehive.musicfilter.data.spotify.SpotifyAlbum
import com.wardellbagby.thebeehive.musicfilter.data.spotify.SpotifyArtist
import com.wardellbagby.thebeehive.musicfilter.data.spotify.SpotifyTrack
import com.wardellbagby.thebeehive.navigation.ComposeScreen
import com.wardellbagby.thebeehive.navigation.SwipeToBack
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.Trash
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import org.jetbrains.compose.resources.painterResource
import thebeehiveapp.mobile.generated.resources.Res
import thebeehiveapp.mobile.generated.resources.ic_back
import thebeehiveapp.mobile.generated.resources.ic_settings

class MusicFilterScreen
@OptIn(ExperimentalUuidApi::class)
constructor(
  val uiData: UiData,
  val onRefresh: () -> Unit,
  val onDeletePlay: (Uuid) -> Unit,
  val onBack: () -> Unit,
  val onEnabledChanged: (Boolean) -> Unit,
  val onSettingsClicked: () -> Unit,
) : ComposeScreen {

  sealed interface UiData {
    data object Loading : UiData

    data object Error : UiData

    data class Loaded(
      val isEnabled: Boolean,
      val bannedTracks: List<BannedTrackPlay>,
      val nextEviction: Instant?,
      val maxPlaysAllowed: Int,
      val isRefreshing: Boolean,
    ) : UiData
  }

  @OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)
  @Composable
  override fun Content() {
    SwipeToBack(onBack = onBack) {
      Scaffold(
        topBar = {
          MusicFilterTopBar(
            showActions = uiData is UiData.Loaded,
            isEnabled = (uiData as? UiData.Loaded)?.isEnabled ?: false,
            onEnabledChanged = onEnabledChanged,
            onBack = onBack,
            onSettingsClicked = onSettingsClicked,
          )
        }
      ) { padding ->
        when (val data = uiData) {
          is UiData.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              CircularProgressIndicator()
            }
          }
          is UiData.Error -> {
            Column(
              modifier = Modifier.fillMaxSize(),
              verticalArrangement = Arrangement.Center,
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Text(
                "Failed to load.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
              )
              Spacer(Modifier.height(16.dp))
              Button(onClick = onRefresh) { Text("Retry") }
            }
          }
          is UiData.Loaded -> {
            PullToRefreshBox(isRefreshing = data.isRefreshing, onRefresh = onRefresh) {
              LazyColumn(contentPadding = padding, modifier = Modifier.fillMaxSize()) {
                stickyHeader {
                  MusicFilterHeader(
                    nextEviction = data.nextEviction,
                    trackCount = data.bannedTracks.size,
                    maxPlaysAllowed = data.maxPlaysAllowed,
                  )
                }
                items(data.bannedTracks, key = { it.id.toString() }) { play ->
                  BannedTrackRow(play, onDelete = { onDeletePlay(play.id) })
                  HorizontalDivider()
                }
              }
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MusicFilterTopBar(
  showActions: Boolean,
  isEnabled: Boolean,
  onEnabledChanged: (Boolean) -> Unit,
  onSettingsClicked: () -> Unit,
  onBack: () -> Unit,
) {
  MediumFlexibleTopAppBar(
    title = { Text("Music Filter") },
    navigationIcon = {
      IconButton(onClick = onBack) {
        Icon(painterResource(Res.drawable.ic_back), contentDescription = "Back")
      }
    },
    actions = {
      if (showActions) {
        Switch(
          modifier = Modifier.padding(end = 16.dp),
          checked = isEnabled,
          onCheckedChange = onEnabledChanged,
        )
        IconButton(onSettingsClicked) {
          Icon(painterResource(Res.drawable.ic_settings), contentDescription = "Settings")
        }
      }
    },
  )
}

private fun Duration.format(): String {
  val suffix = if (this.isNegative()) "" else "ago"
  val abs = this.absoluteValue
  val seconds = abs.inWholeSeconds
  val minutes = abs.inWholeMinutes
  val hours = abs.inWholeHours
  val days = abs.inWholeDays
  val amount =
    when {
      seconds < 60 -> if (seconds == 1L) "1 second" else "$seconds seconds"
      minutes < 60 -> if (minutes == 1L) "1 minute" else "$minutes minutes"
      hours < 24 -> if (hours == 1L) "1 hour" else "$hours hours"
      else -> if (days == 1L) "1 day" else "$days days"
    }
  return "$amount $suffix".trim()
}

@Composable
private fun MusicFilterHeader(nextEviction: Instant?, trackCount: Int, maxPlaysAllowed: Int) {
  val evictionText =
    nextEviction?.let {
      val evictionTime = (LocalClock.current.now() - it)
      val formattedEvictionTime =
        if (!evictionTime.isNegative()) {
          "On next track play"
        } else {
          evictionTime.format()
        }

      "Next eviction: $formattedEvictionTime"
    } ?: "No eviction scheduled"
  Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.padding(16.dp)) {
    Text(
      evictionText,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.weight(1f).requiredWidthIn(4.dp))
    Text(
      "$trackCount / $maxPlaysAllowed",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
  HorizontalDivider()
}

@OptIn(ExperimentalUuidApi::class, ExperimentalCoilApi::class)
@Composable
@Preview
private fun BannedTrackRowPreview() {
  CompositionLocalProvider(
    LocalClock provides Clock.System,
    LocalAsyncImagePreviewHandler provides
      AsyncImagePreviewHandler { ColorImage(Color.Red.toArgb()) },
  ) {
    BannedTrackRow(
      onDelete = {},
      play =
        BannedTrackPlay(
          id = Uuid.random(),
          playedAt = Clock.System.now(),
          track =
            SpotifyTrack(
              id = "",
              name = "A Great Song",
              artists = listOf(SpotifyArtist("A Bad Artist")),
              album = SpotifyAlbum(emptyList()),
            ),
        ),
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
private fun BannedTrackRow(play: BannedTrackPlay, onDelete: () -> Unit) {
  val dismissState =
    rememberSwipeToDismissBoxState(
      SwipeToDismissBoxValue.Settled,
      SwipeToDismissBoxDefaults.positionalThreshold,
    )
  Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
    SwipeToDismissBox(
      state = dismissState,
      enableDismissFromStartToEnd = false,
      onDismiss = { onDelete() },
      backgroundContent = {
        Box(
          modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.error),
          contentAlignment = Alignment.CenterEnd,
        ) {
          Icon(
            imageVector = FontAwesomeIcons.Solid.Trash,
            contentDescription = "Delete",
            tint = MaterialTheme.colorScheme.onError,
            modifier = Modifier.padding(end = 24.dp).size(24.dp),
          )
        }
      },
    ) {
      Row(
        modifier = Modifier.fillMaxSize().background(CardDefaults.cardColors().containerColor),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        AsyncImage(
          model = play.track.album.images.firstOrNull()?.url,
          contentDescription = play.track.name,
          modifier = Modifier.size(96.dp),
          contentScale = ContentScale.Crop,
        )
        Spacer(Modifier.width(16.dp))
        Column(verticalArrangement = Arrangement.Center) {
          Text(play.track.name, style = MaterialTheme.typography.titleMedium)
          Text(
            play.track.artists.joinToString { it.name },
            style = MaterialTheme.typography.bodyMedium,
          )
          Spacer(Modifier.height(16.dp))
          Text(
            (LocalClock.current.now() - play.playedAt).format(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}
