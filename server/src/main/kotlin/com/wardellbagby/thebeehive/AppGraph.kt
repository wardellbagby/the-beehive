package com.wardellbagby.thebeehive

import com.wardellbagby.thebeehive.client.createDefaultHttpClient
import com.wardellbagby.thebeehive.client.createDefaultJson
import com.wardellbagby.thebeehive.service.BeehiveService
import com.wardellbagby.thebeehive.service.BeehiveServiceServer
import com.wardellbagby.thebeehive.status.LogMessage
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.createGraphFactory
import io.ktor.client.HttpClient
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json

@DependencyGraph(AppScope::class)
interface AppGraph {

  @DependencyGraph.Factory
  interface Factory {
    fun create(
      @Provides config: ServerConfig,
      @Provides coroutineScope: CoroutineScope,
      @Provides logs: Flow<LogMessage>,
    ): AppGraph
  }

  val jobManager: JobManager

  val service: BeehiveService

  @Binds fun service(impl: BeehiveServiceServer): BeehiveService

  @Provides fun clock(): Clock = Clock.System

  @Provides fun json(): Json = createDefaultJson()

  @Provides fun httpClient(json: Json): HttpClient = createDefaultHttpClient(json)
}

fun createAppGraph(
  config: ServerConfig,
  coroutineScope: CoroutineScope,
  logs: Flow<LogMessage>,
): AppGraph = createGraphFactory<AppGraph.Factory>().create(config, coroutineScope, logs)
