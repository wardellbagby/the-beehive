@file:OptIn(ExperimentalJsExport::class)

package com.wardellbagby.thebeehive.homebridge.externals

import kotlin.js.Promise

@JsExport
interface PlatformAccessory {
  val displayName: String
  val UUID: String
  var context: dynamic

  fun getService(service: dynamic): Service?

  fun addService(service: dynamic): Service
}

@JsExport
interface Service {
  fun setCharacteristic(characteristic: dynamic, value: dynamic): Service

  fun getCharacteristic(characteristic: dynamic): Characteristic

  fun updateCharacteristic(characteristic: dynamic, value: dynamic): Service
}

@JsExport
interface Characteristic {
  fun onGet(handler: () -> Promise<dynamic>): Characteristic

  fun onSet(handler: (value: dynamic) -> Promise<dynamic>): Characteristic
}

@JsExport
interface Logging {
  fun info(message: String, vararg parameters: dynamic)

  fun warn(message: String, vararg parameters: dynamic)

  fun error(message: String, vararg parameters: dynamic)

  fun debug(message: String, vararg parameters: dynamic)
}

@JsExport
interface UuidNamespace {
  fun generate(data: String): String
}

@JsExport
interface HapNamespace {
  val Service: dynamic
  val Characteristic: dynamic
  val uuid: UuidNamespace
}

@JsExport
interface API {
  val hap: HapNamespace

  fun on(event: String, listener: () -> Unit)

  val platformAccessory: dynamic

  fun registerPlatform(platformName: String, constructor: dynamic)

  fun registerPlatformAccessories(
    pluginIdentifier: String,
    platformName: String,
    accessories: Array<PlatformAccessory>,
  )

  fun updatePlatformAccessories(accessories: Array<PlatformAccessory>)

  fun unregisterPlatformAccessories(
    pluginIdentifier: String,
    platformName: String,
    accessories: Array<PlatformAccessory>,
  )
}
