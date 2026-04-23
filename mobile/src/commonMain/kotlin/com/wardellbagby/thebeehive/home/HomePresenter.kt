package com.wardellbagby.thebeehive.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.wardellbagby.thebeehive.logs.LogsPresenter
import com.wardellbagby.thebeehive.musicfilter.MusicFilterPresenter
import com.wardellbagby.thebeehive.photodisplay.PhotoDisplayPresenter
import com.wardellbagby.thebeehive.navigation.BasicScreenPresenter
import com.wardellbagby.thebeehive.navigation.UiStack
import com.wardellbagby.thebeehive.navigation.asBackStackScreen
import com.wardellbagby.thebeehive.navigation.atBottomOf
import com.wardellbagby.thebeehive.navigation.render
import com.wardellbagby.thebeehive.notifications.FcmTokenProvider
import com.wardellbagby.thebeehive.notifications.RegisterTokenRequest
import com.wardellbagby.thebeehive.service.BeehiveServiceClient
import dev.zacsweers.metro.Inject

class HomePresenter
@Inject
constructor(
  private val musicFilterPresenter: MusicFilterPresenter,
  private val logsPresenter: LogsPresenter,
  private val photoDisplayPresenter: PhotoDisplayPresenter,
  private val service: BeehiveServiceClient,
  private val tokenProvider: FcmTokenProvider,
) : BasicScreenPresenter<UiStack>() {

  private sealed interface State {
    data object Home : State

    data object MusicFilter : State

    data object Logs : State

    data object PhotoDisplay : State
  }

  @Composable
  override fun present(): UiStack {
    LaunchedEffect(Unit) {
      tokenProvider.token()?.let { service.registerToken(RegisterTokenRequest(token = it)) }
    }

    var state by remember { mutableStateOf<State>(State.Home) }

    val body =
      HomeScreen(
          onMusicFilterClicked = { state = State.MusicFilter },
          onLogsClicked = { state = State.Logs },
          onPhotoDisplayClicked = { state = State.PhotoDisplay },
        )
        .asBackStackScreen()

    return when (state) {
      State.Home -> UiStack(body)
      State.MusicFilter ->
        body atBottomOf
          musicFilterPresenter.render { output ->
            when (output) {
              MusicFilterPresenter.Output.Exited -> {
                state = State.Home
              }
            }
          }
      State.Logs ->
        body atBottomOf
          logsPresenter.render { output ->
            when (output) {
              LogsPresenter.Output.Exited -> state = State.Home
            }
          }
      State.PhotoDisplay ->
        body atBottomOf
          photoDisplayPresenter.render { output ->
            when (output) {
              PhotoDisplayPresenter.Output.Exited -> state = State.Home
            }
          }
    }
  }
}
