package com.wardellbagby.thebeehive.navigation

data class UiStack(val body: ComposeScreen, val overlays: List<ComposeOverlay> = emptyList())

fun UiStack.plus(body: ComposeScreen): UiStack =
  UiStack(body = BackStackScreen(this.body, body), overlays = overlays)

fun ComposeScreen.asBackStackScreen(): BackStackScreen =
  when (this) {
    is BackStackScreen -> this
    else -> BackStackScreen(this)
  }

infix fun ComposeScreen.atBottomOf(stack: UiStack): UiStack =
  UiStack(body = BackStackScreen(this.flatten() + stack.body), overlays = stack.overlays)

private fun ComposeScreen.flatten(): List<ComposeScreen> =
  when (this) {
    is BackStackScreen -> screens
    else -> listOf(this)
  }
