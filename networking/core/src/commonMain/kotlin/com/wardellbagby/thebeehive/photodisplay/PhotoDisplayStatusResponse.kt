package com.wardellbagby.thebeehive.photodisplay

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class PhotoDisplayStatusResponse(val image: ByteArray?, val metadata: PhotoDisplayMetadata) {
  @Serializable
  data class PhotoDisplayMetadata(
    val label: String,
    val createdAt: LocalDateTime? = null,
    val isAlbumArtwork: Boolean,
    val hasPrevious: Boolean = false,
  )
}
