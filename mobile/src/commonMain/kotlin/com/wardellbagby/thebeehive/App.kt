package com.wardellbagby.thebeehive

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withCompositionLocals
import androidx.compose.ui.Modifier
import com.wardellbagby.thebeehive.navigation.UiStack
import com.wardellbagby.thebeehive.navigation.renderRoot
import com.wardellbagby.thebeehive.theme.AppTheme
import dev.zacsweers.metro.createGraph
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.datetime.TimeZone

@OptIn(ExperimentalTime::class)
@Composable
fun App() {
  LaunchedEffect(Unit) { Napier.base(DebugAntilog()) }
  AppTheme {
    val appGraph = remember { createGraph<AppGraph>() }

    CompositionLocalProvider(
      LocalClock provides appGraph.clock,
      LocalTimeZone provides appGraph.timeZone,
    ) {
      val snackbarHostState = remember { SnackbarHostState() }
      // CompositionLocals that should be shared between both the normal Compose UI and the Molecule
      // Presenters.
      val sharedLocals: Array<ProvidedValue<*>> = remember {
        arrayOf(LocalSnackbarHost provides snackbarHostState)
      }

      Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
      ) {
        withCompositionLocals(*sharedLocals) {
          appGraph.appPresenter.renderRoot(*sharedLocals, props = Unit, onOutput = {}).Show()
        }
      }
    }
  }
}

val LocalSnackbarHost =
  staticCompositionLocalOf<SnackbarHostState> { error("No SnackbarHostState provided!") }

@OptIn(ExperimentalTime::class)
val LocalClock = staticCompositionLocalOf<Clock> { error("No Clock provided!") }
val LocalTimeZone = staticCompositionLocalOf<TimeZone> { error("No TimeZone provided!") }

@Composable
private fun UiStack.Show() {
  body.Content()
  overlays.forEach { it.Content() }
}
