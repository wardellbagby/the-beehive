@file:OptIn(ExperimentalJsExport::class)

package com.wardellbagby.thebeehive.homebridge

import com.wardellbagby.thebeehive.homebridge.externals.API

internal const val PLATFORM_NAME = "BeehiveHomebridgePlugin"
internal const val PLUGIN_NAME = "@wardellbagby/homebridge-beehive-plugin"

@JsExport
@JsExport.Default
@JsName("registerPlugin")
fun registerPlugin(api: API) {
  api.registerPlatform(PLATFORM_NAME, BeehiveHomebridgePlatform::class.js)
}
