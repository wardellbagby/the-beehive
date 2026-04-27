package com.wardellbagby.thebeehive.photodisplay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.wardellbagby.thebeehive.navigation.ComposeOverlay
import com.wardellbagby.thebeehive.navigation.FullModalOverlay
import com.wardellbagby.thebeehive.navigation.ScreenPresenter
import com.wardellbagby.thebeehive.service.BeehiveService
import com.wardellbagby.thebeehive.service.onFailure
import com.wardellbagby.thebeehive.service.onSuccess
import com.wardellbagby.thebeehive.status.LogMessage
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.onCompletion

class UpdatePhotosPresenter @Inject constructor(private val service: BeehiveService) :
  ScreenPresenter<Unit, UpdatePhotosPresenter.Output, ComposeOverlay>() {

  sealed interface Output {
    data object Dismissed : Output
  }

  @Composable
  override fun present(props: Unit, onOutput: (Output) -> Unit): ComposeOverlay {
    val logs = remember { mutableStateListOf<LogMessage>() }
    var isFinished by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
      service
        .updatePhotoDisplay()
        .onSuccess { flow -> flow.onCompletion { isFinished = true }.collect { logs.add(it) } }
        .onFailure { isFinished = true }
    }

    return FullModalOverlay(
      content =
        UpdatePhotosScreen(
          logs = logs,
          isFinished = isFinished,
          onDismiss = { onOutput(Output.Dismissed) },
        ),
      onDismiss = { onOutput(Output.Dismissed) },
    )
  }
}
