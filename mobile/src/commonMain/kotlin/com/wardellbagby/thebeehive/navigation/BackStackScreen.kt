package com.wardellbagby.thebeehive.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import kotlin.uuid.ExperimentalUuidApi

/**
 * A [ComposeScreen] that renders the last screen in [screens] as its visible content, animating
 * transitions when the stack changes in a meaningful way.
 *
 * Animation only fires when the new screens represent a push or pop relative to the previous ones,
 * determined by screen type. Re-renders that rebuild screen instances at the same class sequence
 * produce no animation. Complete replacements (no common prefix) also produce no animation.
 *
 * - **Pop**: new screens are a strict prefix of the old (same types, fewer of them) → outgoing top
 *   slides away to the right.
 * - **Push**: new screens start with all of the old and add more at the end → new top slides in
 *   from the right.
 */
class BackStackScreen(val screens: List<ComposeScreen>) : ComposeScreen {

  constructor(vararg screen: ComposeScreen) : this(screen.toList())

  @OptIn(ExperimentalUuidApi::class)
  @Composable
  override fun Content() {
    AnimatedContent(
      targetState = screens,
      // Key on the sequence of screen classes, not instances. This prevents an animation
      // from firing when the presenter rebuilds screen objects at the same class sequence,
      // while still triggering one when the sequence genuinely changes.
      contentKey = { it.map { screen -> screen::class } },
      transitionSpec = {
        val oldClasses = initialState.map { it::class }
        val newClasses = targetState.map { it::class }
        when {
          // Pop: new is a strict prefix of old → outgoing top slides out to the right.
          newClasses.size < oldClasses.size && oldClasses.take(newClasses.size) == newClasses -> {
            slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
          }

          // Push: old is a strict prefix of new → new top slides in from the right.
          newClasses.size > oldClasses.size && newClasses.take(oldClasses.size) == oldClasses -> {
            slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
          }

          // Complete replacement or no meaningful relationship: switch instantly.
          else -> {
            fadeIn() togetherWith fadeOut()
          }
        }
      },
      label = "BackStackScreen",
    ) { targetScreens ->
      targetScreens.last().Content()
    }
  }
}
