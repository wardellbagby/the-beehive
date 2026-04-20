package com.wardellbagby.thebeehive.musicfilter.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wardellbagby.thebeehive.navigation.ComposeScreen
import com.wardellbagby.thebeehive.util.INT_ONLY_INPUT_TRANSFORMATION
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.Trash
import org.jetbrains.compose.resources.painterResource
import thebeehiveapp.mobile.generated.resources.Res
import thebeehiveapp.mobile.generated.resources.ic_close
import thebeehiveapp.mobile.generated.resources.ic_save

enum class SlidingWindowUnit(val label: String) {
  Minutes("Minutes"),
  Hours("Hours"),
  Days("Days"),
}

class MusicFilterSettingsScreen(
  private val uiData: UiData,
  private val onCancel: () -> Unit,
  private val onConfirm: () -> Unit,
  private val onMaxPlaysChanged: (Int) -> Unit,
  private val onSlidingWindowAmountChanged: (Int) -> Unit,
  private val onSlidingWindowUnitChanged: (SlidingWindowUnit) -> Unit,
  private val onArtistNameChanged: (index: Int, value: String) -> Unit,
  private val onArtistNameRemoved: (index: Int) -> Unit,
  private val onAddArtist: () -> Unit,
) : ComposeScreen {

  data class UiData(
    val maxPlaysAllowed: Int,
    val slidingWindowAmount: Int,
    val slidingWindowUnit: SlidingWindowUnit,
    val bannedArtistNames: List<String>,
    val isSaving: Boolean,
  )

  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    Scaffold(
      topBar = {
        CenterAlignedTopAppBar(
          title = { Text("Music Filter Settings") },
          navigationIcon = {
            IconButton(onClick = onCancel, enabled = !uiData.isSaving) {
              Icon(painterResource(Res.drawable.ic_close), contentDescription = "Cancel")
            }
          },
          actions = {
            if (uiData.isSaving) {
              CircularProgressIndicator(
                modifier = Modifier.padding(end = 16.dp).size(24.dp),
                strokeWidth = 2.dp,
              )
            } else {
              FilledIconButton(onClick = onConfirm) {
                Icon(painterResource(Res.drawable.ic_save), contentDescription = "Save")
              }
            }
          },
        )
      }
    ) { padding ->
      Column(
        modifier =
          Modifier.fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
      ) {
        MaxPlaysField(
          value = uiData.maxPlaysAllowed,
          enabled = !uiData.isSaving,
          onValueChanged = onMaxPlaysChanged,
        )
        Spacer(Modifier.height(16.dp))
        SlidingWindowField(
          amount = uiData.slidingWindowAmount,
          unit = uiData.slidingWindowUnit,
          enabled = !uiData.isSaving,
          onAmountChanged = onSlidingWindowAmountChanged,
          onUnitChanged = onSlidingWindowUnitChanged,
        )
        Spacer(Modifier.height(24.dp))
        BannedArtistsSection(
          names = uiData.bannedArtistNames,
          enabled = !uiData.isSaving,
          onNameChanged = onArtistNameChanged,
          onNameRemoved = onArtistNameRemoved,
          onAddArtist = onAddArtist,
        )
      }
    }
  }
}

@Composable
private fun MaxPlaysField(value: Int, enabled: Boolean, onValueChanged: (Int) -> Unit) {
  val textFieldState = rememberTextFieldState(value.toString())

  LaunchedEffect(textFieldState.text) { onValueChanged(textFieldState.text.toString().toInt()) }

  OutlinedTextField(
    state = textFieldState,
    label = { Text("Max Plays Allowed") },
    lineLimits = TextFieldLineLimits.SingleLine,
    inputTransformation = INT_ONLY_INPUT_TRANSFORMATION,
    enabled = enabled,
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    modifier = Modifier.fillMaxWidth(),
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SlidingWindowField(
  amount: Int,
  unit: SlidingWindowUnit,
  enabled: Boolean,
  onAmountChanged: (Int) -> Unit,
  onUnitChanged: (SlidingWindowUnit) -> Unit,
) {
  var unitDropdownExpanded by remember { mutableStateOf(false) }
  val textFieldState = rememberTextFieldState(amount.toString())

  LaunchedEffect(textFieldState.text) { onAmountChanged(textFieldState.text.toString().toInt()) }
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.Bottom,
  ) {
    OutlinedTextField(
      modifier = Modifier.weight(60f),
      state = textFieldState,
      label = { Text("Sliding Window") },
      lineLimits = TextFieldLineLimits.SingleLine,
      inputTransformation = INT_ONLY_INPUT_TRANSFORMATION,
      enabled = enabled,
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
    ExposedDropdownMenuBox(
      expanded = unitDropdownExpanded,
      onExpandedChange = { unitDropdownExpanded = it && enabled },
      modifier = Modifier.weight(40f),
    ) {
      OutlinedTextField(
        modifier =
          Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        value = unit.label,
        onValueChange = {},
        readOnly = true,
        enabled = enabled,
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitDropdownExpanded) },
      )
      ExposedDropdownMenu(
        expanded = unitDropdownExpanded,
        onDismissRequest = { unitDropdownExpanded = false },
      ) {
        SlidingWindowUnit.entries.forEach { entry ->
          DropdownMenuItem(
            text = { Text(entry.label) },
            onClick = {
              onUnitChanged(entry)
              unitDropdownExpanded = false
            },
          )
        }
      }
    }
  }
}

@Composable
private fun BannedArtistsSection(
  names: List<String>,
  enabled: Boolean,
  onNameChanged: (index: Int, value: String) -> Unit,
  onNameRemoved: (index: Int) -> Unit,
  onAddArtist: () -> Unit,
) {
  Text("Banned Artists", style = MaterialTheme.typography.titleMedium)
  Spacer(Modifier.height(4.dp))
  names.forEachIndexed { index, name ->
    val textFieldState = rememberTextFieldState(name)
    LaunchedEffect(textFieldState.text) { onNameChanged(index, textFieldState.text.toString()) }
    OutlinedTextField(
      state = textFieldState,
      label = { Text("Artist ${index + 1}") },
      lineLimits = TextFieldLineLimits.SingleLine,
      enabled = enabled,
      trailingIcon = {
        IconButton(onClick = { onNameRemoved(index) }, enabled = enabled) {
          Icon(
            FontAwesomeIcons.Solid.Trash,
            contentDescription = "Remove artist",
            modifier = Modifier.size(16.dp),
          )
        }
      },
      modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(8.dp))
  }
  if (names.size < 5 && enabled) {
    TextButton(onClick = onAddArtist) { Text("+ Add Artist") }
  }
}

@Preview
@Composable
fun MusicFilterSettingsScreenPreview() {
  MusicFilterSettingsScreen(
      uiData =
        MusicFilterSettingsScreen.UiData(
          maxPlaysAllowed = 6,
          slidingWindowAmount = 6,
          slidingWindowUnit = SlidingWindowUnit.Hours,
          bannedArtistNames = listOf("Drake", "Kanye"),
          isSaving = false,
        ),
      onCancel = {},
      onConfirm = {},
      onAddArtist = {},
      onMaxPlaysChanged = {},
      onArtistNameChanged = { _, _ -> },
      onArtistNameRemoved = {},
      onSlidingWindowUnitChanged = {},
      onSlidingWindowAmountChanged = {},
    )
    .Content()
}
