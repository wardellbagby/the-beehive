package com.wardellbagby.thebeehive.util

import androidx.compose.foundation.text.input.InputTransformation

val INT_ONLY_INPUT_TRANSFORMATION = InputTransformation {
  // todo this doesn't allow adding negative numbers or easily going from 0 to 8
  if (length == 0) {
    append("0")
  }
  val isNumeric = asCharSequence().toString().toIntOrNull() != null

  if (!isNumeric) {
    revertAllChanges()
  }
}
