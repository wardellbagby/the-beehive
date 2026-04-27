package com.wardellbagby.thebeehive

import com.wardellbagby.thebeehive.utils.forResult
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlinx.serialization.json.Json

interface HasSerializableState {
  val filesystem: Filesystem
}

context(container: Any)
inline fun <reified T> HasSerializableState.savableState(initial: T) =
  object : ReadWriteProperty<HasSerializableState, T> {
    private var lastSeenState: T = initial
    // Seems odd but lets T be nullable without needing to create a sentinel value to represent
    // lastSeenState having not been set yet.
    private var hasBeenSet = false

    override fun getValue(thisRef: HasSerializableState, property: KProperty<*>): T {
      if (hasBeenSet) {
        return lastSeenState
      }

      if (property.path().exists()) {
        return forResult { Json.decodeFromString<T>(property.path().readText()) }
          .getOrElse {
            container
              .getLogger()
              .warn(
                "Unable to decode saved state for property {}; using initial value.",
                property.name,
              )
            initial
          }
      }

      return initial
    }

    override fun setValue(thisRef: HasSerializableState, property: KProperty<*>, value: T) {
      lastSeenState = value
      hasBeenSet = true
      property.path().writeText(Json.encodeToString(value))
    }

    private fun KProperty<*>.path(): Path {
      return filesystem.workingDirectory(forClass = container::class).resolve(name)
    }
  }
