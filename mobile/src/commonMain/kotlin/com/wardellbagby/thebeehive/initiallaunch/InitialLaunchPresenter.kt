package com.wardellbagby.thebeehive.initiallaunch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.wardellbagby.thebeehive.initiallaunch.InitialLaunchScreen.HostReadyState
import com.wardellbagby.thebeehive.navigation.ScreenPresenter
import com.wardellbagby.thebeehive.navigation.UiStack
import com.wardellbagby.thebeehive.service.BeehiveServiceClientFactory
import com.wardellbagby.thebeehive.service.onFailure
import com.wardellbagby.thebeehive.service.onSuccess
import dev.zacsweers.metro.Inject

@Inject
class InitialLaunchPresenter(private val serviceFactory: BeehiveServiceClientFactory) :
  ScreenPresenter<Unit, InitialLaunchPresenter.Output, UiStack>() {
  sealed interface Output {
    data class Finished(val hostname: String) : Output
  }

  @Composable
  override fun present(props: Unit, onOutput: (Output) -> Unit): UiStack {
    var hostReadyState by remember { mutableStateOf<HostReadyState>(HostReadyState.Unchecked) }
    var hostToCheck by remember { mutableStateOf<String?>(null) }
    val service = remember(hostToCheck) { serviceFactory.create(hostToCheck ?: "") }

    LaunchedEffect(hostToCheck) {
      hostToCheck?.let { host ->
        hostReadyState = HostReadyState.Checking
        service
          .beehiveStatus()
          .onFailure { hostReadyState = HostReadyState.Unavailable }
          .onSuccess { onOutput(Output.Finished(host)) }
      }
    }

    return UiStack(
      InitialLaunchScreen(
        hostReadyState = hostReadyState,
        onHostErrorAcknowledged = {
          hostReadyState = HostReadyState.Unchecked
          hostToCheck = null
        },
        onHostEntered = { hostToCheck = it },
      )
    )
  }
}
