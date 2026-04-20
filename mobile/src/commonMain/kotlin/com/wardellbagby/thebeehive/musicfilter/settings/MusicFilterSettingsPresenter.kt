package com.wardellbagby.thebeehive.musicfilter.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.wardellbagby.thebeehive.musicfilter.data.MusicFilterStatusResponse
import com.wardellbagby.thebeehive.musicfilter.data.UpdateMusicFilterSettingsRequest
import com.wardellbagby.thebeehive.navigation.ComposeOverlay
import com.wardellbagby.thebeehive.navigation.FullModalOverlay
import com.wardellbagby.thebeehive.navigation.ScreenPresenter
import com.wardellbagby.thebeehive.service.BeehiveService
import com.wardellbagby.thebeehive.service.onFailure
import com.wardellbagby.thebeehive.service.onSuccess
import dev.zacsweers.metro.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.launch

@Inject
class MusicFilterSettingsPresenter(private val service: BeehiveService) :
  ScreenPresenter<
    MusicFilterSettingsPresenter.Props,
    MusicFilterSettingsPresenter.Output,
    ComposeOverlay,
  >() {

  data class Props(val currentStatus: MusicFilterStatusResponse)

  sealed interface Output {
    data object Cancelled : Output

    data class Saved(val response: MusicFilterStatusResponse) : Output

    data object SaveFailed : Output
  }

  @Composable
  override fun present(props: Props, onOutput: (Output) -> Unit): ComposeOverlay {
    var bannedArtistNames by remember { mutableStateOf(props.currentStatus.bannedArtistNames) }
    var maxPlaysAllowed by remember { mutableStateOf(props.currentStatus.maxPlaysAllowed) }
    val (initialWindowAmount, initialWindowUnit) =
      remember { props.currentStatus.slidingWindow.decompose() }
    var slidingWindowAmount by remember { mutableStateOf(initialWindowAmount) }
    var slidingWindowUnit by remember { mutableStateOf(initialWindowUnit) }
    var isSaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    return FullModalOverlay(
      content =
        MusicFilterSettingsScreen(
          uiData =
            MusicFilterSettingsScreen.UiData(
              maxPlaysAllowed = maxPlaysAllowed,
              slidingWindowAmount = slidingWindowAmount,
              slidingWindowUnit = slidingWindowUnit,
              bannedArtistNames = bannedArtistNames,
              isSaving = isSaving,
            ),
          onCancel = { onOutput(Output.Cancelled) },
          onConfirm = {
            scope.launch {
              isSaving = true
              service
                .updateMusicFilterSettings(
                  UpdateMusicFilterSettingsRequest(
                    maxPlaysAllowed = maxPlaysAllowed,
                    bannedArtistNames = bannedArtistNames,
                    slidingWindow = slidingWindowAmount.toSlidingWindowDuration(slidingWindowUnit),
                  )
                )
                .onSuccess { onOutput(Output.Saved(it)) }
                .onFailure { onOutput(Output.SaveFailed) }
            }
          },
          onMaxPlaysChanged = { value -> maxPlaysAllowed = value },
          onSlidingWindowAmountChanged = { value -> slidingWindowAmount = value },
          onSlidingWindowUnitChanged = { unit -> slidingWindowUnit = unit },
          onArtistNameChanged = { index: Int, value: String ->
            bannedArtistNames =
              bannedArtistNames.toMutableList().also { list -> list[index] = value }
          },
          onArtistNameRemoved = { index: Int ->
            bannedArtistNames =
              bannedArtistNames.toMutableList().also { list -> list.removeAt(index) }
          },
          onAddArtist = { bannedArtistNames = bannedArtistNames + "" },
        ),
      onDismiss = { onOutput(Output.Cancelled) },
    )
  }

  private fun Duration.decompose(): Pair<Int, SlidingWindowUnit> {
    val totalMinutes = inWholeMinutes
    return when {
      totalMinutes > 0 && totalMinutes % (24 * 60) == 0L ->
        (totalMinutes / (24 * 60)).toInt() to SlidingWindowUnit.Days
      totalMinutes > 0 && totalMinutes % 60 == 0L ->
        (totalMinutes / 60).toInt() to SlidingWindowUnit.Hours
      else -> totalMinutes.toInt() to SlidingWindowUnit.Minutes
    }
  }

  private fun Int.toSlidingWindowDuration(unit: SlidingWindowUnit): Duration {
    return when (unit) {
      SlidingWindowUnit.Minutes -> toLong().minutes
      SlidingWindowUnit.Hours -> toLong().hours
      SlidingWindowUnit.Days -> toLong().days
    }
  }
}
