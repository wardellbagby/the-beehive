package com.wardellbagby.thebeehive.navigation

import androidx.compose.runtime.Composable
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@Composable
actual fun <T> withDefaultPlatformCompositionLocals(
  coroutineContext: CoroutineContext,
  body: @Composable (() -> T),
): T = body()

@Composable
actual fun createDefaultMoleculeContext(): CoroutineContext {
  return EmptyCoroutineContext
}
