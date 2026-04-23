package com.wardellbagby.thebeehive.logs

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.wardellbagby.thebeehive.navigation.ComposeScreen
import com.wardellbagby.thebeehive.status.LogMessage
import org.jetbrains.compose.resources.painterResource
import thebeehiveapp.mobile.generated.resources.Res
import thebeehiveapp.mobile.generated.resources.ic_back

class LogsScreen(val logs: List<LogMessage>, val onBack: () -> Unit) : ComposeScreen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    Scaffold(
      topBar = {
        CenterAlignedTopAppBar(
          title = { Text("Logs") },
          navigationIcon = {
            IconButton(onClick = onBack) {
              Icon(painterResource(Res.drawable.ic_back), contentDescription = "Back")
            }
          },
        )
      }
    ) { paddingValues ->
      FilteredLogList(
        logs = logs,
        includeLoggerName = true,
        modifier = Modifier.fillMaxSize().padding(paddingValues),
      )
    }
  }
}
