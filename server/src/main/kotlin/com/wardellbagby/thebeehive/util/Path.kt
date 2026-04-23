package com.wardellbagby.thebeehive.util

import java.nio.file.Path
import kotlin.io.path.exists

fun <R> Path.ifExists(block: (Path) -> R): R? {
  if (exists()) {
    return block(this)
  }
  return null
}
