package com.wardellbagby.thebeehive.photodisplay

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wardellbagby.thebeehive.logs.FilteredLogList
import com.wardellbagby.thebeehive.navigation.ComposeScreen
import com.wardellbagby.thebeehive.status.LogMessage
import org.jetbrains.compose.resources.painterResource
import thebeehiveapp.mobile.generated.resources.Res
import thebeehiveapp.mobile.generated.resources.ic_close

class UpdatePhotosScreen(
  val logs: List<LogMessage>,
  val isFinished: Boolean,
  val onDismiss: () -> Unit,
) : ComposeScreen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    Scaffold(
      topBar = {
        CenterAlignedTopAppBar(
          title = { Text(if (isFinished) "Update Complete" else "Updating Photos…") },
          navigationIcon = {
            IconButton(onClick = onDismiss) {
              Icon(painterResource(Res.drawable.ic_close), contentDescription = "Close")
            }
          },
        )
      }
    ) { paddingValues ->
      Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
        FilteredLogList(
          logs = logs,
          modifier =
            Modifier.fillMaxSize()
              .padding(bottom = if (isFinished) ButtonDefaults.MinHeight + 32.dp else 0.dp),
        )
        if (isFinished) {
          Button(
            onClick = onDismiss,
            modifier =
              Modifier.fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 16.dp),
          ) {
            Text("Close")
          }
        }
      }
    }
  }
}