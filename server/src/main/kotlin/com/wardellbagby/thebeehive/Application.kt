package com.wardellbagby.thebeehive

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.PropertySource
import com.wardellbagby.thebeehive.service.server.BeehiveServiceServerPlugin
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.IgnoreTrailingSlash
import io.ktor.server.websocket.WebSockets
import java.io.File
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

@OptIn(ExperimentalHoplite::class)
suspend fun main(): Unit = supervisorScope {
  val config =
    ConfigLoaderBuilder.default()
      .addPropertySource(PropertySource.file(File("config.yaml")))
      .withExplicitSealedTypes()
      .build()
      .loadConfigOrThrow<ServerConfig>()
  val graph = createAppGraph(config, coroutineScope = this)

  launch {
    embeddedServer(Netty, port = config.serverPort) {
        install(IgnoreTrailingSlash)
        install(ContentNegotiation) { json(json = graph.json()) }
        install(BeehiveServiceServerPlugin(graph.service))
        install(WebSockets)
      }
      .startSuspend(wait = true)
  }

  graph.jobManager.startAll()
}
