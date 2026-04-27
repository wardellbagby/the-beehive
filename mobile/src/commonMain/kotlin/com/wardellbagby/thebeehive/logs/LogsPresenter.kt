package com.wardellbagby.thebeehive.logs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.wardellbagby.thebeehive.LocalSnackbarHost
import com.wardellbagby.thebeehive.navigation.ScreenPresenter
import com.wardellbagby.thebeehive.navigation.UiStack
import com.wardellbagby.thebeehive.navigation.asBackStackScreen
import com.wardellbagby.thebeehive.service.BeehiveService
import com.wardellbagby.thebeehive.service.onFailure
import com.wardellbagby.thebeehive.service.onSuccess
import com.wardellbagby.thebeehive.status.LogMessage
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.onCompletion

@Inject
class LogsPresenter(private val service: BeehiveService) :
  ScreenPresenter<Unit, LogsPresenter.Output, UiStack>() {

  sealed interface Output {
    data object Exited : Output
  }

  @Composable
  override fun present(props: Unit, onOutput: (Output) -> Unit): UiStack {
    val logs = remember { mutableStateListOf<LogMessage>() }
    var isFinished by remember { mutableStateOf(false) }
    val snackbarHost = LocalSnackbarHost.current

    LaunchedEffect(isFinished) {
      if (isFinished) {
        snackbarHost.showSnackbar("Disconnected!")
      }
    }

    LaunchedEffect(Unit) {
      service
        .logs()
        .onSuccess { flow -> flow.onCompletion { isFinished = true }.collect { logs.add(it) } }
        .onFailure { isFinished = true }
    }

    return UiStack(
      LogsScreen(logs = logs, onBack = { onOutput(Output.Exited) }).asBackStackScreen()
    )
  }
}
