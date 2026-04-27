package com.wardellbagby.thebeehive.photodisplay

import com.wardellbagby.thebeehive.Filesystem
import com.wardellbagby.thebeehive.getLogger
import com.wardellbagby.thebeehive.photodisplay.PhotoDisplayStatusResponse.PhotoDisplayMetadata
import com.wardellbagby.thebeehive.util.ifExists
import com.wardellbagby.thebeehive.utils.finallyIgnoringAll
import com.wardellbagby.thebeehive.utils.forResult
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.net.InetAddress
import java.net.NetworkInterface
import java.nio.file.Path
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceListener
import kotlin.io.path.extension
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.streams.asSequence
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json

@Inject
@SingleIn(AppScope::class)
class PhotoDisplayManager(
  filesystem: Filesystem,
  private val apiFactory: TuneshineApi.Factory,
  private val json: Json,
) {
  private val logger = getLogger()
  private val imagesDirectory: Path = DownloadPhotosOneShot.imagesDirectory(filesystem)

  private var tuneshineApi: TuneshineApi? = null

  private val shuffledRemaining = ArrayDeque<Path>()
  private val history = mutableListOf<Path>()
  private var historyIndex = -1
  private val navigateChannel = Channel<Boolean>(Channel.CONFLATED)

  fun navigate(forward: Boolean) {
    navigateChannel.trySend(forward)
  }

  fun pendingNavigate(): Boolean? = navigateChannel.tryReceive().getOrNull()

  suspend fun awaitNavigateOrTimeout(timeout: Duration) {
    withTimeoutOrNull(timeout) {
      val forward = navigateChannel.receive()
      navigateChannel.trySend(forward)
    }
  }

  fun advanceToNext() {
    if (historyIndex < history.lastIndex) {
      historyIndex++
      return
    }
    if (shuffledRemaining.isEmpty()) {
      val files =
        imagesDirectory.listDirectoryEntries().filter { it.extension.lowercase() == "webp" }
      if (files.isEmpty()) error("No images found in $imagesDirectory")
      shuffledRemaining.addAll(files.shuffled())
      logger.info("Starting new shuffle round with ${shuffledRemaining.size} images")
    }
    history.add(shuffledRemaining.removeFirst())
    historyIndex = history.lastIndex
  }

  fun goToPrevious() {
    if (historyIndex > 0) historyIndex--
  }

  fun currentImagePath(): Path? = if (historyIndex >= 0) history[historyIndex] else null

  private fun currentSidecarData(): SidecarData? =
    currentImagePath()?.let {
      it.resolveSibling("${it.name}.json").ifExists { sidecarFile ->
        forResult { json.decodeFromString<Array<SidecarData>>(sidecarFile.readText()).first() }
          .getOrNull()
      }
    }

  suspend fun waitForTuneshineDiscovery(force: Boolean = false): TuneshineApi {
    val api = tuneshineApi
    if (!force && api != null) {
      return api
    }

    logger.debug("Discovering Tuneshine via mDNS. Is forced? $force")
    while (true) {
      val host = discoverTuneshine()
      if (host != null) {
        logger.info("Found Tuneshine at $host")
        return apiFactory.create(host).also { tuneshineApi = it }
      }
      delay(20.seconds)
    }
  }

  suspend fun getStatus(): PhotoDisplayStatusResponse {
    val api =
      withTimeoutOrNull(20.seconds) { waitForTuneshineDiscovery() }
        ?: error("Unable to connect to Tuneshine")

    return PhotoDisplayStatusResponse(
      image = api.getArtwork(),
      metadata = api.getState().toMetadata(),
    )
  }

  private suspend fun discoverTuneshine(): String? =
    withContext(Dispatchers.IO) { NetworkInterface.networkInterfaces() }
      .asSequence()
      .filter { it.isUp && it.supportsMulticast() && !it.isLoopback }
      .flatMap { it.inetAddresses.asSequence() }
      .distinct()
      .toList()
      .reversed()
      .firstNotNullOfOrNull { discoverTuneshine(bindAddress = it) }

  private suspend fun discoverTuneshine(bindAddress: InetAddress): String? =
    withTimeoutOrNull(5.seconds) {
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

  private fun DeviceState.toMetadata(): PhotoDisplayMetadata {
    return localMetadata?.let {
      val currentSidecarData = currentSidecarData()
      PhotoDisplayMetadata(
        label = currentSidecarData?.people?.joinAsNounSeparatedStringOrNull() ?: "A pretty photo!",
        createdAt = currentSidecarData?.dateTimeCreated,
        isAlbumArtwork = false,
        hasPrevious = historyIndex > 0,
      )
    }
      ?: remoteMetadata?.let {
        PhotoDisplayMetadata(
          label = it.trackName ?: "Unknown song",
          isAlbumArtwork = !(it.idle ?: false),
        )
      }
      ?: PhotoDisplayMetadata(label = "Unknown", isAlbumArtwork = false)
  }

  private companion object {
    private const val TUNESHINE_SERVICE = "_tuneshine._tcp.local."

    private fun List<String>.joinAsNounSeparatedStringOrNull(): String? {
      if (isEmpty()) return null
      if (size == 1) return first()
      val suffix = "and ${last()}"
      val beginning = dropLast(1).joinToString()
      return "$beginning $suffix"
    }
  }
}
