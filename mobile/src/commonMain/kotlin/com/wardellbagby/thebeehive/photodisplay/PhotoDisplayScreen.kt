package com.wardellbagby.thebeehive.photodisplay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.wardellbagby.thebeehive.navigation.ComposeScreen
import com.wardellbagby.thebeehive.navigation.SwipeToBack
import com.wardellbagby.thebeehive.util.shortDateTimeFormat
import kotlinx.datetime.format
import org.jetbrains.compose.resources.painterResource
import thebeehiveapp.mobile.generated.resources.Res
import thebeehiveapp.mobile.generated.resources.ic_back
import thebeehiveapp.mobile.generated.resources.ic_forward
import thebeehiveapp.mobile.generated.resources.ic_more

class PhotoDisplayScreen(
  val response: PhotoDisplayStatusResponse?,
  val error: Boolean,
  val onBack: () -> Unit,
  val onUpdatePhotos: () -> Unit,
  val onPrevious: (() -> Unit)?,
  val onNext: () -> Unit,
) : ComposeScreen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    SwipeToBack(onBack) {
      Scaffold(
        topBar = {
          var menuExpanded by remember { mutableStateOf(false) }
          CenterAlignedTopAppBar(
            title = { Text("Photo Display") },
            navigationIcon = {
              IconButton(onClick = onBack) {
                Icon(painterResource(Res.drawable.ic_back), contentDescription = "Back")
              }
            },
            actions = {
              IconButton(onClick = { menuExpanded = true }) {
                Icon(painterResource(Res.drawable.ic_more), contentDescription = "More options")
              }
              DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                  text = { Text("Update photos") },
                  onClick = {
                    menuExpanded = false
                    onUpdatePhotos()
                  },
                )
              }
            },
          )
        }
      ) { paddingValues ->
        Box(
          modifier = Modifier.fillMaxSize().padding(paddingValues),
          contentAlignment = Alignment.TopCenter,
        ) {
          when {
            error ->
              Text("Failed to load photo display.", style = MaterialTheme.typography.bodyLarge)
            response == null -> CircularProgressIndicator()
            else -> PhotoDisplayContent(response, onPrevious, onNext)
          }
        }
      }
    }
  }
}

@Composable
private fun PhotoDisplayContent(
  response: PhotoDisplayStatusResponse,
  onPrevious: (() -> Unit)?,
  onNext: () -> Unit,
) {
  Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
    PhotoDisplayCard(response)
    if (!response.metadata.isAlbumArtwork) {
      Spacer(Modifier.height(16.dp))
      PhotoDisplayNavigationButtons(onPrevious, onNext)
    }
  }
}

@Composable
private fun PhotoDisplayCard(response: PhotoDisplayStatusResponse) {
  Card(modifier = Modifier.fillMaxWidth()) {
    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
      PhotoDisplayImage(image = response.image, label = response.metadata.label)
      PhotoDisplayInfo(response.metadata)
    }
  }
}

@Composable
private fun PhotoDisplayImage(image: ByteArray?, label: String) {
  if (image != null) {
    key(image.hashCode()) {
      AsyncImage(
        modifier = Modifier.size(128.dp),
        model = image,
        contentDescription = label,
        contentScale = ContentScale.Fit,
      )
    }
  } else {
    Text(
      modifier = Modifier.size(128.dp),
      text = "No image",
      style = MaterialTheme.typography.bodyLarge,
    )
  }
}

@Composable
private fun PhotoDisplayInfo(metadata: PhotoDisplayStatusResponse.PhotoDisplayMetadata) {
  Column(modifier = Modifier.padding(start = 16.dp)) {
    Text(text = metadata.label, style = MaterialTheme.typography.titleMedium)
    metadata.createdAt?.let {
      Text(
        text = it.format(shortDateTimeFormat),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun PhotoDisplayNavigationButtons(onPrevious: (() -> Unit)?, onNext: () -> Unit) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
    FilledIconButton(
      onClick = onPrevious ?: {},
      enabled = onPrevious != null,
      modifier = Modifier.weight(1f).height(56.dp),
    ) {
      Icon(painterResource(Res.drawable.ic_back), contentDescription = "Previous")
    }
    FilledIconButton(onClick = onNext, modifier = Modifier.weight(1f).height(56.dp)) {
      Icon(painterResource(Res.drawable.ic_forward), contentDescription = "Next")
    }
  }
}
