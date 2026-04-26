package com.wardellbagby.thebeehive.util

import androidx.compose.foundation.text.input.InputTransformation

val INT_ONLY_INPUT_TRANSFORMATION = InputTransformation {
  val text = asCharSequence().toString()
  when {
    text.isEmpty() -> append("0")
    text.toIntOrNull() == null -> revertAllChanges()
    else -> {
      val normalized = text.toInt().toString()
      if (text != normalized) replace(0, length, normalized)
    }
  }
}
