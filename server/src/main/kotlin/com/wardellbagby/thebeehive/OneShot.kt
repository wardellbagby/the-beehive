package com.wardellbagby.thebeehive

/**
 * Intended for short-running work that will complete eventually.
 *
 * Must be invoked with [OneShotExecutor].
 */
interface OneShot {
  val logs: List<String>

  suspend fun run()
}
