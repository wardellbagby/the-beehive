package com.wardellbagby.thebeehive.logs

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wardellbagby.thebeehive.LocalTimeZone
import com.wardellbagby.thebeehive.status.LogMessage
import kotlin.time.Instant
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.Padding
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import thebeehiveapp.mobile.generated.resources.Res
import thebeehiveapp.mobile.generated.resources.ic_down

@Composable
fun FilteredLogList(
  logs: List<LogMessage>,
  includeLoggerName: Boolean = false,
  modifier: Modifier = Modifier,
) {
  val listState = rememberLazyListState()
  val timeZone = LocalTimeZone.current
  val horizontalScrollState = rememberScrollState()
  val scope = rememberCoroutineScope()
  var filter by remember { mutableStateOf("") }
  val filteredLogs =
    remember(logs, filter) {
      if (filter.isEmpty()) logs
      else
        logs.filter { log ->
          log.level.contains(filter, ignoreCase = true) ||
            log.loggerName.contains(filter, ignoreCase = true) ||
            log.message.contains(filter, ignoreCase = true)
        }
    }
  val showScrollToBottom by remember { derivedStateOf { listState.canScrollForward } }
  var stickToBottom by remember { mutableStateOf(false) }

  LaunchedEffect(filteredLogs.size) {
    if (stickToBottom && filteredLogs.isNotEmpty()) {
      listState.scrollToItem(filteredLogs.lastIndex)
    }
  }

  LaunchedEffect(Unit) {
    snapshotFlow { listState.canScrollForward }.collect { canScrollForward ->
      if (canScrollForward && stickToBottom) stickToBottom = false
    }
  }
  val textMeasurer = rememberTextMeasurer()
  val longestLine =
    remember(filteredLogs) {
      filteredLogs
        .maxByOrNull {
          it.annotated(includeLoggerName = includeLoggerName, timeZone = timeZone).length
        }
        ?.annotated(includeLoggerName = includeLoggerName, timeZone = timeZone)
        ?: AnnotatedString("")
    }
  val density = LocalDensity.current
  val longestLineWidthDp =
    remember(longestLine) {
      with(density) {
        textMeasurer
          .measure(longestLine, TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp))
          .size
          .width
          .toDp()
      }
    }
  val errorColor = MaterialTheme.colorScheme.error

  Box(modifier = modifier) {
    Column(modifier = Modifier.fillMaxSize()) {
      OutlinedTextField(
        value = filter,
        onValueChange = { filter = it },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        placeholder = { Text("Filter logs…") },
        singleLine = true,
      )
      Box(modifier = Modifier.fillMaxSize().horizontalScroll(horizontalScrollState)) {
        LazyColumn(
          modifier = Modifier.fillMaxHeight().widthIn(min = longestLineWidthDp + 16.dp),
          state = listState,
        ) {
          items(filteredLogs) { log ->
            Text(
              text = log.annotated(errorColor, includeLoggerName, timeZone),
              softWrap = false,
              fontFamily = FontFamily.Monospace,
              fontSize = 12.sp,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            )
          }
        }
      }
    }
    if (showScrollToBottom) {
      FloatingActionButton(
        onClick = {
          scope.launch {
            stickToBottom = true
            listState.scrollToItem(filteredLogs.lastIndex.coerceAtLeast(0))
          }
        },
        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
      ) {
        Icon(painterResource(Res.drawable.ic_down), contentDescription = "Scroll to bottom")
      }
    }
  }
}

internal fun LogMessage.annotated(
  errorColor: Color? = null,
  includeLoggerName: Boolean,
  timeZone: TimeZone,
): AnnotatedString {
  val time = formatTimestamp(timestamp, timeZone)
  val paddedLevel = level.padEnd(5)
  val shortLogger = loggerName.substringAfterLast('.')
  val levelColor =
    when (level.uppercase()) {
      "ERROR" -> errorColor
      "WARN" -> Color(0xFFFFB74D)
      "DEBUG" -> Color(0xFF64B5F6)
      "TRACE" -> Color(0xFF9E9E9E)
      else -> null
    }
  return buildAnnotatedString {
    append("$time ")
    if (levelColor != null) {
      withStyle(SpanStyle(color = levelColor)) { append(paddedLevel) }
    } else {
      append(paddedLevel)
    }
    append("${" $shortLogger ".takeIf { includeLoggerName } ?: ""} $message")
  }
}

private val logTimeFormat = LocalDateTime.Format {
  monthNumber(Padding.ZERO)
  chars("-")
  day(Padding.ZERO)
  chars(" ")
  amPmHour()
  chars(":")
  minute()
  chars(":")
  second()
  amPmMarker(am = "AM", pm = "PM")
}

internal fun formatTimestamp(millis: Long, timeZone: TimeZone): String {
  return Instant.fromEpochMilliseconds(millis).toLocalDateTime(timeZone).format(logTimeFormat)
}
