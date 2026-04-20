package com.wardellbagby.thebeehive.initiallaunch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wardellbagby.thebeehive.initiallaunch.InitialLaunchScreen.HostReadyState
import com.wardellbagby.thebeehive.navigation.ComposeScreen
import com.wardellbagby.thebeehive.util.isValidUrl
import org.jetbrains.compose.resources.painterResource
import thebeehiveapp.mobile.generated.resources.Res
import thebeehiveapp.mobile.generated.resources.ic_forward

class InitialLaunchScreen(
  val hostReadyState: HostReadyState,
  val onHostEntered: (String) -> Unit,
  val onHostErrorAcknowledged: () -> Unit,
) : ComposeScreen {
  sealed interface HostReadyState {
    data object Unchecked : HostReadyState

    data object Checking : HostReadyState

    data object Unavailable : HostReadyState
  }

  @OptIn(ExperimentalMaterial3ExpressiveApi::class)
  @Composable
  override fun Content() {
    Scaffold {
      Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(128.dp, Alignment.CenterVertically),
      ) {
        Text(
          "Welcome to The Beehive!",
          style = MaterialTheme.typography.displayMedium,
          textAlign = TextAlign.Center,
        )
        Column {
          Row {
            val hostFieldState by remember { mutableStateOf(TextFieldState()) }

            LaunchedEffect(hostFieldState.text) {
              if (hostReadyState is HostReadyState.Unavailable) {
                onHostErrorAcknowledged()
              }
            }

            OutlinedTextField(
              modifier = Modifier.weight(1f),
              enabled = hostReadyState !is HostReadyState.Checking,
              state = hostFieldState,
              placeholder = { Text("http://example.com") },
              supportingText = {
                Text(
                  if (hostReadyState is HostReadyState.Unavailable) {
                    "Unable to connect to host."
                  } else {
                    "Enter your server's domain"
                  }
                )
              },
              lineLimits = TextFieldLineLimits.SingleLine,
              onKeyboardAction = { onHostEntered(hostFieldState.text.toString()) },
              isError = hostReadyState is HostReadyState.Unavailable,
              inputTransformation = {
                val filtered = asCharSequence().replace(Regex("\\s"), "")
                replace(0, length, filtered)
              },
              suffix = {
                FilledIconButton(
                  enabled =
                    hostReadyState is HostReadyState.Unchecked && hostFieldState.text.isValidUrl(),
                  onClick = { onHostEntered(hostFieldState.text.toString()) },
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    if (hostReadyState is HostReadyState.Checking) {
                      CircularProgressIndicator()
                    }
                    Icon(painterResource(Res.drawable.ic_forward), contentDescription = "Enter")
                  }
                }
              },
            )
          }
        }
      }
    }
  }
}

@Preview
@Composable
fun InitialLaunchScreenPreview() {
  InitialLaunchScreen(
      hostReadyState = HostReadyState.Unchecked,
      onHostEntered = {},
      onHostErrorAcknowledged = {},
    )
    .Content()
}
