package com.wardellbagby.thebeehive.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import thebeehiveapp.mobile.generated.resources.Res
import thebeehiveapp.mobile.generated.resources.ic_back

/**
 * Wraps [content] with a left-edge swipe-to-back gesture. The content can be dragged up to 15% of
 * its width to the right; releasing at the maximum reveals a back arrow and triggers [onBack].
 * Releasing before the maximum snaps back.
 */
@Composable
fun SwipeToBack(
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  val scope = rememberCoroutineScope()
  val dragOffsetX = remember { Animatable(0f) }

  BoxWithConstraints(modifier = modifier) {
    val maxDragPx = constraints.maxWidth * 0.15f

    Box(
      modifier = Modifier.fillMaxWidth(0.15f).fillMaxHeight().align(Alignment.CenterStart),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        painter = painterResource(Res.drawable.ic_back),
        contentDescription = "Back",
        modifier =
          Modifier.alpha((dragOffsetX.value / maxDragPx).coerceIn(0f, 1f)).padding(start = 16.dp),
      )
    }

    Box(
      modifier =
        Modifier.fillMaxSize()
          .offset { IntOffset(dragOffsetX.value.roundToInt(), 0) }
          .pointerInput(Unit) {
            val edgePx = 40.dp.toPx()
            var trackingEdgeSwipe = false
            detectHorizontalDragGestures(
              onDragStart = { offset -> trackingEdgeSwipe = offset.x <= edgePx },
              onDragEnd = {
                if (trackingEdgeSwipe) {
                  scope.launch {
                    if (dragOffsetX.value >= maxDragPx * 0.9f) {
                      dragOffsetX.snapTo(0f)
                      onBack()
                    } else {
                      dragOffsetX.animateTo(0f, spring())
                    }
                  }
                }
                trackingEdgeSwipe = false
              },
              onDragCancel = {
                if (trackingEdgeSwipe) {
                  scope.launch { dragOffsetX.animateTo(0f, spring()) }
                }
                trackingEdgeSwipe = false
              },
              onHorizontalDrag = { change, dragAmount ->
                if (trackingEdgeSwipe) {
                  change.consume()
                  scope.launch {
                    dragOffsetX.snapTo((dragOffsetX.value + dragAmount).coerceIn(0f, maxDragPx))
                  }
                }
              },
            )
          }
    ) {
      content()
    }
  }
}
