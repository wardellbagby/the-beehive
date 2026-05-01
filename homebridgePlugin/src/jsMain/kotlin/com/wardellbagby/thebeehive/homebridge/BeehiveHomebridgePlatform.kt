@file:OptIn(ExperimentalJsExport::class, DelicateCoroutinesApi::class)

package com.wardellbagby.thebeehive.homebridge

import com.wardellbagby.thebeehive.client.createDefaultHttpClient
import com.wardellbagby.thebeehive.client.createDefaultJson
import com.wardellbagby.thebeehive.homebridge.externals.API
import com.wardellbagby.thebeehive.homebridge.externals.Logging
import com.wardellbagby.thebeehive.homebridge.externals.PlatformAccessory
import com.wardellbagby.thebeehive.service.BeehiveServiceClient
import com.wardellbagby.thebeehive.service.onFailure
import com.wardellbagby.thebeehive.service.onSuccess
import io.ktor.http.Url
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@JsExport
class BeehiveHomebridgePlatform(val log: Logging, config: dynamic, val api: API) {
  val Service: dynamic = api.hap.Service
  val Characteristic: dynamic = api.hap.Characteristic
  val hostname: String = config.hostname as String
  internal val beehiveService =
    BeehiveServiceClient(
      client = createDefaultHttpClient(createDefaultJson()),
      baseUrl = Url(hostname),
    )
  internal val accessories: MutableMap<String, PlatformAccessory> = mutableMapOf()

  init {
    log.debug("Finished initializing Beehive platform:", config.name)
    api.on("didFinishLaunching") {
      GlobalScope.launch {
        discoverDevices()
        launch {
          while (true) {
            delay(15_000L)
            runCatching { pollJobStatuses() }
              .onFailure {
                log.error("Unhandled error in pollJobStatuses:", it.message ?: "unknown")
              }
          }
        }
      }
    }
  }

  @JsName("configureAccessory")
  fun configureAccessory(accessory: PlatformAccessory) {
    log.info("Loading accessory from cache:", accessory.displayName)
    accessories[accessory.UUID] = accessory
  }

  internal suspend fun discoverDevices() {
    beehiveService
      .beehiveStatus()
      .onSuccess { body ->
        if (!body.ok) {
          log.error("Beehive status returned ok=false")
          return@onSuccess
        }
        val discoveredUUIDs = mutableListOf<String>()
        for ((jobId, jobStatus) in body.jobs) {
          val uuid = api.hap.uuid.generate(jobId)
          discoveredUUIDs.add(uuid)
          val existing = accessories[uuid]
          if (existing != null) {
            log.info("Restoring existing accessory from cache:", existing.displayName)
            existing.context.jobId = jobId
            existing.context.enabled = jobStatus.enabled
            api.updatePlatformAccessories(arrayOf(existing))
            BeehiveSwitchAccessory(this, existing)
          } else {
            log.info("Adding new job accessory:", jobId)
            val accessory = invokeConstructor<PlatformAccessory>(api.platformAccessory, jobId, uuid)
            accessory.context.jobId = jobId
            accessory.context.enabled = jobStatus.enabled
            BeehiveSwitchAccessory(this, accessory)
            api.registerPlatformAccessories(PLUGIN_NAME, PLATFORM_NAME, arrayOf(accessory))
          }
        }
        for ((uuid, accessory) in accessories.toMap()) {
          if (uuid !in discoveredUUIDs) {
            log.info("Removing stale accessory:", accessory.displayName)
            api.unregisterPlatformAccessories(PLUGIN_NAME, PLATFORM_NAME, arrayOf(accessory))
          }
        }
      }
      .onFailure { log.error("Failed to reach Beehive server:", failure.message ?: "unknown") }
  }

  internal suspend fun pollJobStatuses() {
    beehiveService
      .beehiveStatus()
      .onSuccess { body ->
        if (!body.ok) {
          log.error("Poll failed — ok=false")
          return@onSuccess
        }
        for ((jobId, jobStatus) in body.jobs) {
          val uuid = api.hap.uuid.generate(jobId)
          val cached = accessories[uuid] ?: continue
          val currentEnabled = cached.context.enabled as? Boolean ?: continue
          if (currentEnabled != jobStatus.enabled) {
            log.debug("Poll update [$jobId]: enabled $currentEnabled -> ${jobStatus.enabled}")
            cached.context.enabled = jobStatus.enabled
            cached
              .getService(Service.Switch)
              ?.updateCharacteristic(Characteristic.On, jobStatus.enabled)
          }
        }
      }
      .onFailure { log.error("Poll failed:", failure.message ?: "unknown") }
  }
}
