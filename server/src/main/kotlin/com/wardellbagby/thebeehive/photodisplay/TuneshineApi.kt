package com.wardellbagby.thebeehive.photodisplay

import dev.zacsweers.metro.Inject
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class ImageMetadata(
  val trackName: String? = null,
  val artistName: String? = null,
  val albumName: String? = null,
  val serviceName: String? = null,
  val subServiceName: String? = null,
  val itemId: String? = null,
  val serviceIconUrl: String? = null,
  val zoneName: String? = null,
  val accountName: String? = null,
  val accountId: String? = null,
  val idle: Boolean? = null,
  val imageUrl: String? = null,
  val contentType: String? = null,
  val lastImageError: String? = null,
)

@Serializable data class BrightnessConfig(val base: Int, val active: Int, val idle: Int)

@Serializable
data class DeviceConfig(
  val name: String? = null,
  val firmwareVersion: String? = null,
  val hardwareId: String? = null,
  val brightness: BrightnessConfig,
  val animation: String,
)

@Serializable
data class DeviceState(
  val config: DeviceConfig,
  val localMetadata: ImageMetadata? = null,
  val remoteMetadata: ImageMetadata? = null,
)

@Serializable data class BrightnessRequest(val active: Int? = null, val idle: Int? = null)

private val json = Json { ignoreUnknownKeys = true }

class TuneshineApi private constructor(private val client: HttpClient, host: String) {

  @Inject
  class Factory(private val client: HttpClient) {
    fun create(host: String): TuneshineApi = TuneshineApi(client, host)
  }

  private val baseUrl = "http://$host"

  suspend fun getState(): DeviceState = client.get("$baseUrl/state").body()

  suspend fun postImageFile(imageBytes: ByteArray, metadata: ImageMetadata) {
    client.post("$baseUrl/image") {
      setBody(
        MultiPartFormDataContent(
          formData {
            append(
              key = "image",
              value = imageBytes,
              headers =
                Headers.build {
                  append(
                    HttpHeaders.ContentDisposition,
                    "form-data; name=\"image\"; filename=\"image.webp\"",
                  )
                  append(HttpHeaders.ContentType, "image/webp")
                },
            )
            append("metadata", json.encodeToString(metadata))
          }
        )
      )
    }
  }

  suspend fun deleteImage() {
    client.delete("$baseUrl/image")
  }

  suspend fun setBrightness(request: BrightnessRequest) {
    client.post("$baseUrl/brightness") {
      contentType(io.ktor.http.ContentType.Application.Json)
      setBody(request)
    }
  }

  suspend fun getArtwork(): ByteArray = client.get("$baseUrl/artwork").body()
}
