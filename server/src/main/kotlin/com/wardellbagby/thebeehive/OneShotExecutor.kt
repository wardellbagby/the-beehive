package com.wardellbagby.thebeehive

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Inject
class OneShotExecutor(private val scope: CoroutineScope) {
  fun start(oneShot: OneShot) {
    scope.launch { oneShot.run() }
  }
}
