package com.wardellbagby.thebeehive.settings

import com.russhwolf.settings.Settings as SettingsStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@SingleIn(AppScope::class)
class SettingsRepository @Inject constructor() {
  private companion object {
    private const val HOSTNAME_KEY = "hostname"

    private val REQUIRED_SETTINGS_KEYS = listOf(HOSTNAME_KEY)
  }

  private val store: SettingsStore = SettingsStore()

  fun hasAllRequiredSettings(): Boolean {
    return REQUIRED_SETTINGS_KEYS.all { store.hasKey(it) }
  }

  fun clearAllRequiredSettings() {
    REQUIRED_SETTINGS_KEYS.forEach { store.remove(it) }
  }

  fun settings(): Settings =
    Settings(hostname = store.getStringOrNull(HOSTNAME_KEY) ?: error("Hostname has not been set!"))

  fun setRequiredSettings(hostname: String) {
    store.putString("hostname", hostname)
  }
}
