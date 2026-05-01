@file:OptIn(DelicateCoroutinesApi::class)

package com.wardellbagby.thebeehive.homebridge

import com.wardellbagby.thebeehive.homebridge.externals.PlatformAccessory
import com.wardellbagby.thebeehive.service.onFailure
import com.wardellbagby.thebeehive.service.onSuccess
import com.wardellbagby.thebeehive.status.ToggleJobRequest
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise

class BeehiveSwitchAccessory(
  private val platform: BeehiveHomebridgePlatform,
  private val accessory: PlatformAccessory,
) {
  init {
    val jobId = accessory.context.jobId as String
    accessory
      .getService(platform.Service.AccessoryInformation)!!
      .setCharacteristic(platform.Characteristic.Manufacturer, "Wardell Bagby")
      .setCharacteristic(platform.Characteristic.Model, "Beehive Job Switch")
      .setCharacteristic(platform.Characteristic.SerialNumber, jobId)

    val service =
      accessory.getService(platform.Service.Switch) ?: accessory.addService(platform.Service.Switch)
    service
      .setCharacteristic(platform.Characteristic.Name, jobId)
      .getCharacteristic(platform.Characteristic.On)
      .onGet { GlobalScope.promise { getOn() } }
      .onSet { value -> GlobalScope.promise { setOn(value) } }
  }

  private suspend fun getOn(): Boolean = accessory.context.enabled as Boolean

  private suspend fun setOn(value: dynamic) {
    val jobId = accessory.context.jobId as String
    val isEnabled = value as Boolean
    val previousEnabled = accessory.context.enabled as Boolean
    val service = accessory.getService(platform.Service.Switch)!!

    platform.beehiveService
      .toggleJob(ToggleJobRequest(jobId = jobId, enabled = isEnabled))
      .onSuccess {
        accessory.context.enabled = isEnabled
        platform.api.updatePlatformAccessories(arrayOf(accessory))
      }
      .onFailure {
        platform.log.error("Failed to toggle job [$jobId]:", failure.message ?: "unknown")
        service.updateCharacteristic(platform.Characteristic.On, previousEnabled)
      }
  }
}
