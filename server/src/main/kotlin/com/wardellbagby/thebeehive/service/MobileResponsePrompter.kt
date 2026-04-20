@file:OptIn(ExperimentalUuidApi::class)

package com.wardellbagby.thebeehive.service

import com.wardellbagby.thebeehive.prompt.PromptResponse
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

@SingleIn(AppScope::class)
@Inject
class MobileResponsePrompter {

  data class Prompt(
    val id: Uuid = Uuid.random(),
    val message: String,
    val placeholder: String? = null,
  )

  private val prompts: MutableStateFlow<Prompt?> = MutableStateFlow(null)
  private var currentContinuation: CancellableContinuation<PromptResponse?>? = null

  fun currentPrompt(): StateFlow<Prompt?> = prompts.asStateFlow()

  suspend fun sendPrompt(prompt: Prompt): PromptResponse? =
    withTimeoutOrNull(5.minutes) {
      suspendCancellableCoroutine {
        currentContinuation = it
        it.invokeOnCancellation { clearPrompt() }
      }
    }

  fun onPromptResponse(promptId: Uuid, response: PromptResponse) {
    if (promptId == prompts.value?.id) {
      currentContinuation?.resume(response)
      currentContinuation = null
      clearPrompt()
    }
  }

  fun clearPrompt() {
    prompts.value = null
  }
}
