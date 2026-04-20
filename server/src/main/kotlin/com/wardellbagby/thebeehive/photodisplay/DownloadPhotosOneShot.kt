package com.wardellbagby.thebeehive.photodisplay

import com.github.pgreze.process.Redirect
import com.github.pgreze.process.process
import com.wardellbagby.thebeehive.Filesystem
import com.wardellbagby.thebeehive.OneShot
import com.wardellbagby.thebeehive.ServerConfig
import com.wardellbagby.thebeehive.getLogger
import com.wardellbagby.thebeehive.workingDirectory
import dev.zacsweers.metro.Inject
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.extension
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.nameWithoutExtension

@Inject
class DownloadPhotosOneShot(filesystem: Filesystem, private val config: ServerConfig) : OneShot {
  companion object {
    fun imagesDirectory(filesystem: Filesystem): Path =
      filesystem.workingDirectory<DownloadPhotosOneShot>().resolve("images")
  }

  private val _logs = mutableListOf<String>()
  override val logs: List<String>
    get() = _logs

  private val logger = getLogger()
  private val imagesDirectory = imagesDirectory(filesystem)

  private fun log(message: String) {
    _logs += message
    logger.info(message)
  }

  override suspend fun run() {
    log("Clearing images directory: $imagesDirectory")
    imagesDirectory.toFile().deleteRecursively()
    imagesDirectory.createDirectories()

    log("Downloading iCloud Favorites album for ${config.icloudUsername}...")
    shell(
      "npx",
      "--yes",
      "icloudpd",
      "--username",
      config.icloudUsername,
      "--directory",
      imagesDirectory.toString(),
      "--album",
      "Favorites",
      "--folder-structure",
      "none",
      "--live-photo-mov-filename-policy",
      "original",
    )

    log("Checking for HEIC files with matching MOV counterparts...")
    val heicFiles =
      imagesDirectory
        .listDirectoryEntries()
        .filter { it.extension.lowercase() == "heic" }
        .sortedBy { it.fileName }
    var removed = 0
    for (heic in heicFiles) {
      val stem = heic.nameWithoutExtension
      val hasMov =
        listOf("mov", "MOV").any { imagesDirectory.resolve("$stem.$it").toFile().exists() }
      if (hasMov) {
        log("Deleting HEIC (has matching MOV): $heic")
        heic.deleteIfExists()
        removed++
      }
    }
    log("Removed $removed HEIC file(s) with MOV counterparts.")

    val imageExts = setOf("jpg", "jpeg", "png", "gif", "bmp", "tiff", "tif")
    for (f in imagesDirectory.listDirectoryEntries()) {
      if (f.extension.lowercase() !in imageExts) continue
      log("Converting image: $f")
      shell(
        "ffmpeg",
        "-nostdin",
        "-i",
        f.toString(),
        "-vf",
        "scale=64:64:force_original_aspect_ratio=increase,crop=64:64",
        "-quality",
        "90",
        f.resolveSibling("${f.nameWithoutExtension}.webp").toString(),
        "-hide_banner",
        "-loglevel",
        "error",
      )
    }

    for (f in imagesDirectory.listDirectoryEntries()) {
      if (f.extension.lowercase() != "heic") continue
      log("Converting image: $f")
      shell(
        "convert",
        f.toString(),
        "-resize",
        "64x64^",
        "-gravity",
        "Center",
        "-extent",
        "64x64",
        f.resolveSibling("${f.nameWithoutExtension}.webp").toString(),
      )
    }

    val videoExts = setOf("mp4", "mov", "avi", "mkv", "webm")
    for (f in imagesDirectory.listDirectoryEntries()) {
      if (f.extension.lowercase() !in videoExts) continue
      log("Converting video (boomerang): $f")
      val duration = ffprobeGetDuration(f)
      val cappedDuration = duration.coerceAtMost(10.0)
      val boomerangDuration = cappedDuration * 2
      val bitrate = ((8_388_608 * 0.9) / boomerangDuration).toInt()
      val output = f.resolveSibling("${f.nameWithoutExtension}.webp").toString()
      shell(
        "ffmpeg",
        "-nostdin",
        "-y",
        "-t",
        cappedDuration.toString(),
        "-i",
        f.toString(),
        "-t",
        cappedDuration.toString(),
        "-i",
        f.toString(),
        "-filter_complex",
        "[0:v]scale=64:64:force_original_aspect_ratio=increase,crop=64:64,fps=20[fwd];" +
          "[1:v]scale=64:64:force_original_aspect_ratio=increase,crop=64:64,fps=20,reverse[rev];" +
          "[fwd][rev]concat=n=2:v=1:a=0[out]",
        "-map",
        "[out]",
        "-vcodec",
        "libwebp",
        "-lossless",
        "0",
        "-compression_level",
        "3",
        "-quality",
        "75",
        "-loop",
        "0",
        "-preset",
        "picture",
        "-an",
        "-vsync",
        "0",
        "-b:v",
        "$bitrate",
        "-maxrate",
        "$bitrate",
        "-bufsize",
        "8388608",
        output,
        "-hide_banner",
        "-loglevel",
        "error",
      )
    }

    log("Deleting non-WebP files...")
    for (f in imagesDirectory.listDirectoryEntries()) {
      if (f.extension.lowercase() != "webp") {
        f.deleteIfExists()
      }
    }
    log("Done.")
  }

  private suspend fun shell(vararg args: String) {
    log("Running: ${args.joinToString(" ")}")
    val result = process(*args)
    if (result.resultCode != 0) error("Command failed (exit ${result.resultCode}): ${args.first()}")
  }

  private suspend fun ffprobeGetDuration(file: Path): Double {
    val result =
      process(
        "ffprobe",
        "-v",
        "error",
        "-select_streams",
        "v:0",
        "-show_entries",
        "format=duration",
        "-of",
        "default=noprint_wrappers=1:nokey=1",
        file.toString(),
        stdout = Redirect.CAPTURE,
      )
    return result.output.first().trim().toDouble()
  }
}
