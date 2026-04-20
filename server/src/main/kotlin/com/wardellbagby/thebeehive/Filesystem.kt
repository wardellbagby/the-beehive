package com.wardellbagby.thebeehive

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import java.nio.file.Files
import java.nio.file.Path

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

context(job: Job)
fun Filesystem.workingDirectory(): Path {
  return getRootDirectory()
    .resolve(job::class.simpleName ?: error("Can't use anonymous jobs!"))
    .also { Files.createDirectories(it) }
}

inline fun <reified T : OneShot> Filesystem.workingDirectory(): Path =
  getRootDirectory().resolve(T::class.simpleName ?: error("Can't use anonymous one-shots!")).also {
    Files.createDirectories(it)
  }
