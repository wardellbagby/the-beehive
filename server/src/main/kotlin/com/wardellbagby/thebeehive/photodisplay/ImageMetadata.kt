package com.wardellbagby.thebeehive.photodisplay

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/** Extra data extracted from Apple Photos via osxphotos. */
@Serializable
data class SidecarData(
  @SerialName("XMP:PersonInImage") val people: List<String>?,
  @SerialName("EXIF:DateTimeOriginal")
  @Serializable(with = ExifLocalDateTimeSerializer::class)
  val dateTimeCreated: LocalDateTime,
)

object ExifLocalDateTimeSerializer : KSerializer<LocalDateTime> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor(
      "com.wardellbagby.thebeehive.ExifLocalDateTimeSerializer",
      PrimitiveKind.STRING,
    )

  override fun serialize(encoder: Encoder, value: LocalDateTime) {
    with(value) { encoder.encodeString("$year:$month:$day $hour:$minute:$second") }
  }

  override fun deserialize(decoder: Decoder): LocalDateTime {
    val (date, time) = decoder.decodeString().split(" ")
    val (year, month, day) = date.split(":").map { it.toInt() }
    val (hour, minute, second) = time.split(":").map { it.toInt() }

    return LocalDateTime(
      year = year,
      month = month,
      day = day,
      hour = hour,
      minute = minute,
      second = second,
      nanosecond = 0,
    )
  }
}
