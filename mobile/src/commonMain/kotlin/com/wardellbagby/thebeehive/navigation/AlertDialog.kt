package com.wardellbagby.thebeehive.navigation

import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable

data class AlertDialog(
  val title: String,
  val message: String,
  val buttons: List<ButtonData>,
  val onDismiss: () -> Unit,
) : ComposeOverlay {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    BasicAlertDialog(onDismissRequest = onDismiss) {}
  }

  data class ButtonData(
    val style: ButtonStyle = ButtonStyle.Primary,
    val label: String,
    val onClick: () -> Unit,
  ) {
    enum class ButtonStyle {
      Primary,
      Secondary,
    }
  }
}
