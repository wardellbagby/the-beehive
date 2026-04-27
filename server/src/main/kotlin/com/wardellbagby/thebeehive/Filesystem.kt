package com.wardellbagby.thebeehive

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import java.nio.file.Files
import java.nio.file.Path
import kotlin.reflect.KClass

interface Filesystem {
  fun getRootDirectory(): Path
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class FilesystemImpl : Filesystem {
  init {
    getLogger().debug("Using root directory: {}", getRootDirectory())
  }

  override fun getRootDirectory(): Path {
    return Path.of(System.getProperty("user.home")).resolve(".beehive")
  }
}

context(containing: Any)
fun Filesystem.workingDirectory(): Path {
  return workingDirectory(forClass = containing::class)
}

fun Filesystem.workingDirectory(forClass: KClass<*>): Path {
  val key: String =
    if (forClass.isCompanion) {
      forClass.java.declaringClass.simpleName
    } else {
      forClass.simpleName
    } ?: error("Can't get a working directory inside of a non-class!")

  return getRootDirectory().resolve(key).also { Files.createDirectories(it) }
}
