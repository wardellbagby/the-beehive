package com.wardellbagby.thebeehive.photodisplay

import com.wardellbagby.thebeehive.Filesystem
import com.wardellbagby.thebeehive.Job
import com.wardellbagby.thebeehive.getLogger
import com.wardellbagby.thebeehive.status.JobId
import com.wardellbagby.thebeehive.utils.finallyIgnoringAll
import com.wardellbagby.thebeehive.utils.forResult
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.net.InetAddress
import java.net.NetworkInterface
import java.nio.file.Path
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceListener
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readBytes
import kotlin.streams.asSequence
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

private const val TUNESHINE_SERVICE = "_tuneshine._tcp.local."
private val IMAGE_DISPLAY_DURATION = 20.seconds

@SingleIn(AppScope::class)
@ContributesIntoSet(AppScope::class)
@Inject
class PhotoDisplayJob(
  filesystem: Filesystem,
  private val apiFactory: TuneshineApi.Factory,
  private val clock: Clock,
) : Job {
  override val id: JobId = ID

  private val logger = getLogger()
  private val imagesDirectory: Path = DownloadPhotosOneShot.imagesDirectory(filesystem)

  override suspend fun run() {
    val api = waitForTuneshineDiscovery()

    while (true) {
      forResult {
          val state = api.getState()
          if (state.remoteMetadata?.idle == true) {
            val shownAt = state.localMetadata?.itemId?.let { Instant.parse(it) }
            val expired = shownAt == null || (clock.now() - shownAt) > IMAGE_DISPLAY_DURATION

            if (expired) {
              val imageBytes = randomImage()
              api.postImageFile(
                imageBytes,
                ImageMetadata(itemId = clock.now().toString(), idle = true),
              )
            }
          } else {
            api.deleteImage()
          }
        }
        .onFailure { logger.warn("Error when updating photo display", it) }
      delay(2_000)
    }
  }

  private suspend fun waitForTuneshineDiscovery(): TuneshineApi {
    logger.debug("Discovering Tuneshine via mDNS...")
    while (true) {
      val host = discoverTuneshine()
      if (host != null) {
        logger.info("Found Tuneshine at $host")
        return apiFactory.create(host)
      }
      delay(1.minutes)
    }
  }

  private suspend fun discoverTuneshine(): String? =
    withContext(Dispatchers.IO) { NetworkInterface.networkInterfaces() }
      .asSequence()
      .filter { it.isUp && it.supportsMulticast() && !it.isLoopback }
      .flatMap { it.inetAddresses.asSequence() }
      .distinct()
      .toList()
      .reversed() // It's often the last one anecdotally so let's just start there.
      .firstNotNullOfOrNull { discoverTuneshine(bindAddress = it) }

  private suspend fun discoverTuneshine(bindAddress: InetAddress): String? =
    withTimeoutOrNull(10_000L) {
        withContext(Dispatchers.IO) {
          suspendCancellableCoroutine { cont ->
            val jmdns = JmDNS.create(bindAddress)
            logger.debug("Using interface address {}", jmdns.inetAddress)
            jmdns.addServiceListener(
              TUNESHINE_SERVICE,
              object : ServiceListener {
                override fun serviceAdded(event: ServiceEvent) {
                  jmdns.requestServiceInfo(event.type, event.name)
                }

                override fun serviceRemoved(event: ServiceEvent) {}

                override fun serviceResolved(event: ServiceEvent) {
                  val ip = event.info.inetAddresses.firstOrNull()?.hostAddress
                  if (ip != null && cont.isActive) {
                    finallyIgnoringAll { jmdns.close() }
                    cont.resumeWith(Result.success(ip))
                  }
                }
              },
            )
            cont.invokeOnCancellation { finallyIgnoringAll { jmdns.close() } }
          }
        }
      }
      .also {
        if (it == null) {
          logger.debug("Tuneshine not found using bind address {}", bindAddress)
        }
      }

  private fun randomImage(): ByteArray {
    val files = imagesDirectory.listDirectoryEntries()
    val file = files.randomOrNull() ?: error("No images found in $imagesDirectory")
    return file.readBytes()
  }

  companion object {
    const val ID = "photo-display"
  }
}
