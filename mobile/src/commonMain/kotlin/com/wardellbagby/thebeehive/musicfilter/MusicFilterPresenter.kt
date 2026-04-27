package com.wardellbagby.thebeehive.musicfilter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.wardellbagby.thebeehive.LocalSnackbarHost
import com.wardellbagby.thebeehive.musicfilter.MusicFilterScreen.UiData
import com.wardellbagby.thebeehive.musicfilter.data.DeletePlayRequest
import com.wardellbagby.thebeehive.musicfilter.data.MusicFilterStatusResponse
import com.wardellbagby.thebeehive.musicfilter.settings.MusicFilterSettingsPresenter
import com.wardellbagby.thebeehive.navigation.ScreenPresenter
import com.wardellbagby.thebeehive.navigation.UiStack
import com.wardellbagby.thebeehive.service.BeehiveServiceClient
import com.wardellbagby.thebeehive.service.onFailure
import com.wardellbagby.thebeehive.service.onSuccess
import com.wardellbagby.thebeehive.status.ToggleJobRequest
import dev.zacsweers.metro.Inject
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.ExperimentalUuidApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Inject
class MusicFilterPresenter(
  private val service: BeehiveServiceClient,
  private val settingsPresenter: MusicFilterSettingsPresenter,
) : ScreenPresenter<Unit, MusicFilterPresenter.Output, UiStack>() {

  sealed interface Output {
    data object Exited : Output
  }

  sealed interface State {
    data object Loading : State

    data object Error : State

    data class Loaded(val response: MusicFilterStatusResponse, val isRefreshing: Boolean) : State

    data class Settings(val previousState: Loaded) : State
  }

  @OptIn(ExperimentalUuidApi::class)
  @Composable
  override fun present(props: Unit, onOutput: (Output) -> Unit): UiStack {
    var refreshKey by remember { mutableStateOf(0) }
    var state by remember { mutableStateOf<State>(State.Loading) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = LocalSnackbarHost.current

    LaunchedEffect(refreshKey) {
      state =
        if (refreshKey == 0) {
          State.Loading
        } else {
          (state as? State.Loaded)?.copy(isRefreshing = true) ?: State.Loading
        }

      service
        .musicFilterStatus()
        .onSuccess { state = State.Loaded(it, isRefreshing = false) }
        .onFailure {
          if (state is State.Loaded) {
            snackbarHostState.showSnackbar("Failed to refresh")
          } else {
            state = State.Error
          }
        }
    }

    if (state is State.Loaded) {
      LaunchedEffect(state) {
        delay(1.minutes)
        if (!state.isLoading()) {
          refreshKey++
        }
      }
    }

    val uiData =
      when (val renderState = state) {
        is State.Loading -> UiData.Loading
        is State.Error -> UiData.Error
        is State.Loaded ->
          UiData.Loaded(
            isEnabled = renderState.response.isEnabled,
            bannedTracks = renderState.response.bannedTracks,
            nextEviction = renderState.response.nextEviction,
            maxPlaysAllowed = renderState.response.maxPlaysAllowed,
            isRefreshing = renderState.isRefreshing,
          )
        is State.Settings ->
          UiData.Loaded(
            isEnabled = renderState.previousState.response.isEnabled,
            bannedTracks = renderState.previousState.response.bannedTracks,
            nextEviction = renderState.previousState.response.nextEviction,
            maxPlaysAllowed = renderState.previousState.response.maxPlaysAllowed,
            isRefreshing = false,
          )
      }

    val mainScreen =
      MusicFilterScreen(
        uiData = uiData,
        onBack = { onOutput(Output.Exited) },
        onRefresh = { refreshKey++ },
        onDeletePlay = { id ->
          scope.launch {
            service
              .deletePlay(DeletePlayRequest(id))
              .onSuccess { refreshKey++ }
              .onFailure { snackbarHostState.showSnackbar("Failed to delete play") }
          }
        },
        onEnabledChanged = { enabled ->
          scope.launch {
            val oldState = state

            // Eagerly update local state; the refresh will get it to match in a bit if
            // it was off.
            state =
              (state as? State.Loaded)?.let {
                it.copy(response = it.response.copy(isEnabled = enabled))
              } ?: state
            service
              // todo pull Job ids out into a shared module so they can be reused here?
              .toggleJob(ToggleJobRequest(jobId = "music-filter", enabled = enabled))
              .onSuccess { refreshKey++ }
              .onFailure {
                state = oldState
                snackbarHostState.showSnackbar("Failed to toggle Music Filter")
              }
          }
        },
        onSettingsClicked = { (state as? State.Loaded)?.let { state = State.Settings(it) } },
      )

    return when (val renderState = state) {
      is State.Settings -> {
        val overlay =
          settingsPresenter.render(
            props = MusicFilterSettingsPresenter.Props(renderState.previousState.response),
            onOutput = { output ->
              when (output) {
                is MusicFilterSettingsPresenter.Output.Cancelled ->
                  state = renderState.previousState
                is MusicFilterSettingsPresenter.Output.Saved ->
                  state = State.Loaded(output.response, isRefreshing = false)
                is MusicFilterSettingsPresenter.Output.SaveFailed -> {
                  state = State.Loading
                  refreshKey++
                }
              }
            },
          )
        UiStack(mainScreen, listOf(overlay))
      }
      else -> UiStack(mainScreen)
    }
  }

  private fun State.isLoading(): Boolean =
    this is State.Loading || (this as? State.Loaded)?.isRefreshing == true
}
