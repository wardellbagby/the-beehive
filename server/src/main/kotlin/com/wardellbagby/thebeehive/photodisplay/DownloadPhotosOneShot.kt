package com.wardellbagby.thebeehive.photodisplay

import com.github.pgreze.process.Redirect
import com.github.pgreze.process.process
import com.wardellbagby.thebeehive.Filesystem
import com.wardellbagby.thebeehive.OneShot
import com.wardellbagby.thebeehive.util.ifExists
import com.wardellbagby.thebeehive.utils.forResult
import com.wardellbagby.thebeehive.workingDirectory
import dev.zacsweers.metro.Inject
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.Logger

@Inject
class DownloadPhotosOneShot(filesystem: Filesystem) : OneShot {
  companion object {
    fun imagesDirectory(filesystem: Filesystem): Path =
      filesystem.workingDirectory().resolve("images")
  }

  private val imagesDirectory = imagesDirectory(filesystem)

  private suspend fun renameSidecarIfExists(oldFile: Path, newFile: Path, logger: Logger) =
    withContext(Dispatchers.IO) {
      val oldSidecar = oldFile.resolveSibling("${oldFile.name}.json")
      oldSidecar.ifExists {
        val newSidecar = newFile.resolveSibling("${newFile.name}.json")
        logger.info("Renaming old sidecar ${oldSidecar.name} to ${newSidecar.name}")
        Files.move(oldSidecar, newSidecar)
      }
    }

  override suspend fun run(logger: Logger) {
    logger.info("Clearing images directory: $imagesDirectory")
    imagesDirectory.toFile().deleteRecursively()
    imagesDirectory.createDirectories()

    logger.info("Exporting Photos Favorite album")
    shell(
      logger,
      "osxphotos",
      "export",
      imagesDirectory.absolutePathString(),
      "--skip-original-if-edited",
      "--download-missing",
      "--sidecar",
      "json",
      "--favorite",
    )

    withContext(Dispatchers.IO) {
      logger.info("Checking for HEIC files with matching MOV counterparts...")
      val heicFiles =
        imagesDirectory
          .listDirectoryEntries()
          .filter { it.extension.lowercase() == "heic" }
          .sortedBy { it.fileName }
      var removed = 0
      for (heic in heicFiles) {
        val stem = heic.nameWithoutExtension
        val livePhoto =
          listOf("mov", "MOV")
            .map { imagesDirectory.resolve("$stem.$it") }
            .firstOrNull { it.exists() }

        if (livePhoto != null) {
          logger.info("Deleting HEIC (has matching MOV): $heic")
          heic.deleteIfExists()
          renameSidecarIfExists(heic, livePhoto, logger)
        }
        removed++
      }

      logger.info("Removed $removed HEIC file(s) with MOV counterparts.")

      val imageExts = setOf("jpg", "jpeg", "png", "gif", "bmp", "tiff", "tif")
      for (imageFile in imagesDirectory.listDirectoryEntries()) {
        if (imageFile.extension.lowercase() in imageExts) {
          logger.info("Converting ${imageFile.extension.lowercase()} image with ffmpeg: $imageFile")
          forResult {
              val output = imageFile.resolveSibling("${imageFile.nameWithoutExtension}.webp")
              shell(
                logger,
                "ffmpeg",
                "-nostdin",
                "-i",
                imageFile.toString(),
                "-vf",
                "scale=64:64:force_original_aspect_ratio=increase,crop=64:64",
                "-quality",
                "90",
                output.toString(),
                "-hide_banner",
                "-loglevel",
                "error",
              )
              renameSidecarIfExists(imageFile, output, logger)
              imageFile.deleteIfExists()
            }
            .onFailure {
              logger.warn("Unable to convert $imageFile to webp.", it)
              imageFile.deleteIfExists()
            }
        }
      }

      for (imageFile in imagesDirectory.listDirectoryEntries()) {
        if (imageFile.extension.lowercase() == "heic") {
          logger.info("Converting HEIC image with imagemagick: $imageFile")
          forResult {
              val output = imageFile.resolveSibling("${imageFile.nameWithoutExtension}.webp")
              shell(
                logger,
                "magick",
                imageFile.toString(),
                "-resize",
                "64x64^",
                "-gravity",
                "Center",
                "-extent",
                "64x64",
                output.toString(),
              )
              renameSidecarIfExists(imageFile, output, logger)
              imageFile.deleteIfExists()
            }
            .onFailure {
              logger.warn("Unable to convert $imageFile to webp.", it)
              imageFile.deleteIfExists()
            }
        }
      }

      val videoExts = setOf("mp4", "mov", "avi", "mkv", "webm")
      for (imageFile in imagesDirectory.listDirectoryEntries()) {
        if (imageFile.extension.lowercase() in videoExts) {
          logger.info("Converting video (boomerang): $imageFile")
          val duration = ffprobeGetDuration(imageFile)
          val cappedDuration = duration.coerceAtMost(10.0)
          val boomerangDuration = cappedDuration * 2
          val bitrate = ((8_388_608 * 0.9) / boomerangDuration).toInt()
          val output = imageFile.resolveSibling("${imageFile.nameWithoutExtension}.webp")

          forResult {
              shell(
                logger,
                "ffmpeg",
                "-nostdin",
                "-y",
                "-t",
                cappedDuration.toString(),
                "-i",
                imageFile.toString(),
                "-t",
                cappedDuration.toString(),
                "-i",
                imageFile.toString(),
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
                output.toString(),
                "-hide_banner",
                "-loglevel",
                "error",
              )
              renameSidecarIfExists(imageFile, output, logger)
              imageFile.deleteIfExists()
            }
            .onFailure {
              logger.warn("Unable to convert $imageFile to animated webp.", it)
              imageFile.deleteIfExists()
            }
        }
      }

      logger.info("Done.")
    }
  }

  private suspend fun shell(logger: Logger, vararg args: String) {
    logger.info("Running: ${args.joinToString(" ")}")
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
