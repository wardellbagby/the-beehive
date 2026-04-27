package com.wardellbagby.thebeehive.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wardellbagby.thebeehive.navigation.ComposeScreen
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.Image
import compose.icons.fontawesomeicons.solid.Music
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import thebeehiveapp.mobile.generated.resources.Res
import thebeehiveapp.mobile.generated.resources.app_name
import thebeehiveapp.mobile.generated.resources.ic_logs

class HomeScreen(
  val onMusicFilterClicked: () -> Unit,
  val onLogsClicked: () -> Unit,
  val onPhotoDisplayClicked: () -> Unit,
) : ComposeScreen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    Scaffold(
      topBar = { CenterAlignedTopAppBar(title = { Text(stringResource(Res.string.app_name)) }) },
      floatingActionButton = {
        FloatingActionButton(onClick = onLogsClicked) {
          Icon(painterResource(Res.drawable.ic_logs), contentDescription = "Logs")
        }
      },
    ) { paddingValues ->
      LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = paddingValues + PaddingValues(horizontal = 16.dp),
      ) {
        item {
          LargeButton(
            label = "Music Filter",
            icon = FontAwesomeIcons.Solid.Music,
            onClick = onMusicFilterClicked,
          )
        }
        item {
          LargeButton(
            label = "Photo Display",
            icon = FontAwesomeIcons.Solid.Image,
            onClick = onPhotoDisplayClicked,
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LargeButton(label: String, icon: ImageVector, onClick: () -> Unit) {
  Card(
    colors =
      CardDefaults.cardColors()
        .copy(
          containerColor = ButtonDefaults.buttonColors().containerColor,
          contentColor = ButtonDefaults.buttonColors().contentColor,
        ),
    modifier = Modifier.clickable(onClick = onClick).height(128.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxSize().padding(8.dp),
      verticalArrangement = Arrangement.SpaceAround,
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Icon(modifier = Modifier.size(48.dp), imageVector = icon, contentDescription = "Music Filter")
      Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
    }
  }
}

@Preview
@Composable
fun HomeScreenPreview() {
  HomeScreen(onMusicFilterClicked = {}, onLogsClicked = {}, onPhotoDisplayClicked = {}).Content()
}
