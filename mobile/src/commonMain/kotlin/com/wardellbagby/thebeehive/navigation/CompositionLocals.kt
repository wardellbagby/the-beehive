package com.wardellbagby.thebeehive.navigation

import androidx.compose.runtime.Composable
import kotlin.coroutines.CoroutineContext

@Composable
expect fun <T> withDefaultPlatformCompositionLocals(
  coroutineContext: CoroutineContext,
  body: @Composable () -> T,
): T

@Composable expect fun createDefaultMoleculeContext(): CoroutineContext
