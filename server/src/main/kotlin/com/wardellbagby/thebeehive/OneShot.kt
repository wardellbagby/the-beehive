package com.wardellbagby.thebeehive

import org.slf4j.Logger

/**
 * Intended for short-running work that will complete eventually.
 *
 * Must be invoked with [OneShotExecutor].
 */
interface OneShot {

  suspend fun run(logger: Logger)
}
