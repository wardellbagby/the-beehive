package com.wardellbagby.thebeehive.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import org.jetbrains.compose.resources.Font
import thebeehiveapp.mobile.generated.resources.Res
import thebeehiveapp.mobile.generated.resources.space_grotesk

@Composable
fun AppTypography(): Typography {
  val spaceGroteskFamily = FontFamily(Font(Res.font.space_grotesk))
  val base = MaterialTheme.typography
  return Typography(
    displayLarge = base.displayLarge.copy(fontFamily = spaceGroteskFamily),
    displayMedium = base.displayMedium.copy(fontFamily = spaceGroteskFamily),
    displaySmall = base.displaySmall.copy(fontFamily = spaceGroteskFamily),
    headlineLarge = base.headlineLarge.copy(fontFamily = spaceGroteskFamily),
    headlineMedium = base.headlineMedium.copy(fontFamily = spaceGroteskFamily),
    headlineSmall = base.headlineSmall.copy(fontFamily = spaceGroteskFamily),
    titleLarge = base.titleLarge.copy(fontFamily = spaceGroteskFamily),
    titleMedium = base.titleMedium.copy(fontFamily = spaceGroteskFamily),
    titleSmall = base.titleSmall.copy(fontFamily = spaceGroteskFamily),
    bodyLarge = base.bodyLarge.copy(fontFamily = spaceGroteskFamily),
    bodyMedium = base.bodyMedium.copy(fontFamily = spaceGroteskFamily),
    bodySmall = base.bodySmall.copy(fontFamily = spaceGroteskFamily),
    labelLarge = base.labelLarge.copy(fontFamily = spaceGroteskFamily),
    labelMedium = base.labelMedium.copy(fontFamily = spaceGroteskFamily),
    labelSmall = base.labelSmall.copy(fontFamily = spaceGroteskFamily),
  )
}
