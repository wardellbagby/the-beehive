package com.wardellbagby.thebeehive.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

data class FullModalOverlay(val content: ComposeScreen, val onDismiss: () -> Unit) :
  ComposeOverlay {
  @Composable
  override fun Content() {
    Dialog(
      onDismissRequest = onDismiss,
      properties =
        DialogProperties(
          dismissOnBackPress = false,
          dismissOnClickOutside = false,
          usePlatformDefaultWidth = false,
        ),
    ) {
      Box(modifier = Modifier.fillMaxSize()) { content.Content() }
    }
  }
}
