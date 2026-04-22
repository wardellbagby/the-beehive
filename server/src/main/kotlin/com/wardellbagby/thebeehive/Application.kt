package com.wardellbagby.thebeehive

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.ThrowableProxyUtil
import ch.qos.logback.core.AppenderBase
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.PropertySource
import com.wardellbagby.thebeehive.service.server.BeehiveServiceServerPlugin
import com.wardellbagby.thebeehive.status.LogMessage
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.IgnoreTrailingSlash
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import org.slf4j.LoggerFactory

@OptIn(ExperimentalHoplite::class)
suspend fun main(): Unit = supervisorScope {
  val config =
    ConfigLoaderBuilder.default()
      .addPropertySource(PropertySource.file(File("config.yaml")))
      .withExplicitSealedTypes()
      .build()
      .loadConfigOrThrow<ServerConfig>()
  val graph = createAppGraph(config, coroutineScope = this, logs = createLogs())

  launch {
    embeddedServer(Netty, port = config.serverPort) {
        install(IgnoreTrailingSlash)
        install(ContentNegotiation) { json(json = graph.json()) }
        install(BeehiveServiceServerPlugin(graph.service))
      }
      .startSuspend(wait = true)
  }

  graph.jobManager.startAll()
}

private fun createLogs(): Flow<LogMessage> {
  return MutableSharedFlow<LogMessage>(replay = 500).apply {
    (LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger).apply {
      addAppender(
        object : AppenderBase<ILoggingEvent>() {
            override fun append(logEvent: ILoggingEvent) {
              val throwableMessage =
                logEvent.throwableProxy?.let {
                  ThrowableProxyUtil.indent(StringBuilder(ThrowableProxyUtil.asString(it)), 3)
                }
              tryEmit(
                LogMessage(
                  timestamp = logEvent.timeStamp,
                  level = logEvent.level.levelStr,
                  loggerName = logEvent.loggerName,
                  message =
                    logEvent.formattedMessage +
                      (throwableMessage?.let { "\n$throwableMessage" } ?: ""),
                )
              )
            }
          }
          .apply { start() }
      )
    }
  }
}
