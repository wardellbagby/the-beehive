package com.wardellbagby.thebeehive.homebridge

@JsName("Reflect")
external object JsReflect {
  fun construct(target: dynamic, args: Array<dynamic>): dynamic
}

fun <T> invokeConstructor(target: dynamic, vararg args: dynamic): T {
  return JsReflect.construct(target, args)
}
