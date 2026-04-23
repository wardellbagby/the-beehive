package com.wardellbagby.thebeehive

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.ThrowableProxyUtil
import ch.qos.logback.core.AppenderBase
import com.wardellbagby.thebeehive.status.LogMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.slf4j.LoggerFactory

inline fun <reified T : Any> T.getLogger(): org.slf4j.Logger =
  LoggerFactory.getLogger(T::class.simpleName)

fun createLogFlow(loggerName: String): Pair<Logger, Flow<LogMessage>> {
  val flow = MutableSharedFlow<LogMessage>(replay = 500)
  val logger = LoggerFactory.getLogger(loggerName) as Logger
  logger.addAppender(
    object : AppenderBase<ILoggingEvent>() {
        override fun append(logEvent: ILoggingEvent) {
          val throwableMessage =
            logEvent.throwableProxy?.let {
              StringBuilder(ThrowableProxyUtil.asString(it))
                .apply { ThrowableProxyUtil.indent(this, 3) }
                .toString()
            }
          flow.tryEmit(
            LogMessage(
              timestamp = logEvent.timeStamp,
              level = logEvent.level.levelStr,
              loggerName = logEvent.loggerName,
              message =
                logEvent.formattedMessage + (throwableMessage?.let { "\n$throwableMessage" } ?: ""),
            )
          )
        }
      }
      .apply { start() }
  )
  return logger to flow
}
