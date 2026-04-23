package com.wardellbagby.thebeehive.photodisplay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.wardellbagby.thebeehive.navigation.ScreenPresenter
import com.wardellbagby.thebeehive.navigation.UiStack
import com.wardellbagby.thebeehive.navigation.asBackStackScreen
import com.wardellbagby.thebeehive.service.BeehiveService
import com.wardellbagby.thebeehive.service.onFailure
import com.wardellbagby.thebeehive.service.onSuccess
import dev.zacsweers.metro.Inject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay

class PhotoDisplayPresenter
@Inject
constructor(
  private val service: BeehiveService,
  private val updatePhotosPresenter: UpdatePhotosPresenter,
) : ScreenPresenter<Unit, PhotoDisplayPresenter.Output, UiStack>() {

  sealed interface Output {
    data object Exited : Output
  }

  @Composable
  override fun present(props: Unit, onOutput: (Output) -> Unit): UiStack {
    var response by remember { mutableStateOf<PhotoDisplayStatusResponse?>(null) }
    var error by remember { mutableStateOf(false) }
    var showUpdateOverlay by remember { mutableStateOf(false) }
    var navigateRequest by remember { mutableStateOf<Boolean?>(null) }
    var refreshKey by remember { mutableStateOf(false) }

    LaunchedEffect(refreshKey) {
      while (true) {
        service.photoDisplayStatus().onSuccess { response = it }.onFailure { error = true }
        delay(5.seconds)
      }
    }

    LaunchedEffect(navigateRequest) {
      val forward = navigateRequest ?: return@LaunchedEffect
      service.navigatePhotoDisplay(PhotoDisplayNavigateRequest(forward))
      refreshKey = !refreshKey

      navigateRequest = null
    }

    val mainScreen =
      PhotoDisplayScreen(
          response = response,
          error = error,
          onBack = { onOutput(Output.Exited) },
          onUpdatePhotos = { showUpdateOverlay = true },
          onPrevious =
            { navigateRequest = false }.takeIf { response?.metadata?.hasPrevious == true },
          onNext = { navigateRequest = true },
        )
        .asBackStackScreen()

    if (!showUpdateOverlay) {
      return UiStack(mainScreen)
    }

    val overlay =
      updatePhotosPresenter.render(
        props = Unit,
        onOutput = { output ->
          when (output) {
            UpdatePhotosPresenter.Output.Dismissed -> showUpdateOverlay = false
          }
        },
      )

    return UiStack(mainScreen, listOf(overlay))
  }
}
