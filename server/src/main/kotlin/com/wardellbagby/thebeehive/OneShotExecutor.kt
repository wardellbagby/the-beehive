package com.wardellbagby.thebeehive

import com.wardellbagby.thebeehive.status.LogMessage
import com.wardellbagby.thebeehive.utils.forResult
import dev.zacsweers.metro.Inject
import kotlin.reflect.KClass
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch

@Inject
class OneShotExecutor(private val scope: CoroutineScope) {
  private var status = mutableMapOf<KClass<*>, Flow<LogMessage>>()

  fun start(oneShot: OneShot): Flow<LogMessage> {
    val oneShotKey = oneShot::class

    if (status.contains(oneShotKey)) {
      return status[oneShotKey]!!
    }

    val (logger, logFlow) = createLogFlow("${oneShotKey.simpleName}")
    val runComplete = CompletableDeferred<Unit>()

    scope
      .launch {
        forResult { oneShot.run(logger) }.onFailure { logger.error("Error running one shot", it) }
        runComplete.complete(Unit)
      }
      .invokeOnCompletion { status.remove(oneShotKey) }

    return channelFlow {
        val collectionJob = launch { logFlow.collect { send(it) } }
        runComplete.await()
        collectionJob.cancel()
      }
      .also { status[oneShotKey] = it }
  }
}
