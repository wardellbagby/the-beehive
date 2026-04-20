package com.wardellbagby.thebeehive

import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.wardellbagby.thebeehive.home.HomePresenter
import com.wardellbagby.thebeehive.initiallaunch.InitialLaunchPresenter
import com.wardellbagby.thebeehive.navigation.BasicScreenPresenter
import com.wardellbagby.thebeehive.navigation.UiStack
import com.wardellbagby.thebeehive.navigation.render
import com.wardellbagby.thebeehive.service.BeehiveServiceClient
import com.wardellbagby.thebeehive.service.onFailure
import com.wardellbagby.thebeehive.settings.SettingsRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Provider
import dev.zacsweers.metro.SingleIn
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.delay

@SingleIn(AppScope::class)
class AppPresenter
@Inject
constructor(
    private val homePresenter: HomePresenter,
    private val initialLaunchPresenter: InitialLaunchPresenter,
    private val settingsRepository: SettingsRepository,
    private val serviceProvider: Provider<BeehiveServiceClient>,
) : BasicScreenPresenter<UiStack>() {
  sealed interface State {
    data object InitialLaunch : State

    data object Home : State
  }

  @Composable
  override fun present(): UiStack {
    var state by remember {
      mutableStateOf(
          if (settingsRepository.hasAllRequiredSettings()) {
            State.Home
          } else {
            State.InitialLaunch
          }
      )
    }
    val snackbarHost = LocalSnackbarHost.current

    LaunchedEffect(state) {
      if (state !is State.InitialLaunch) {
        val service = serviceProvider.invoke()
        do {
          service.beehiveStatus().onFailure {
            val result =
                snackbarHost.showSnackbar(
                    message = "Failed to connect to server",
                    actionLabel = "Logout?",
                    withDismissAction = true,
                )
            when (result) {
              SnackbarResult.ActionPerformed -> {
                settingsRepository.clearAllRequiredSettings()
                state = State.InitialLaunch
              }
              SnackbarResult.Dismissed -> {
                break
              }
            }
          }
          delay(1.minutes)
        } while (true)
      }
    }

    return when (state) {
      State.Home -> homePresenter.render()
      State.InitialLaunch ->
          initialLaunchPresenter.render { output ->
            when (output) {
              is InitialLaunchPresenter.Output.Finished -> {
                settingsRepository.setRequiredSettings(hostname = output.hostname)
                state = State.Home
              }
            }
          }
    }
  }
}
