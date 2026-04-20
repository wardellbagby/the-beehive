@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.androidLibrary)
  alias(libs.plugins.kotlinSerialization)
}

kotlin {
  android {
    namespace = "com.wardellbagby.thebeehive.networking.core"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    minSdk = libs.versions.android.minSdk.get().toInt()

    compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
  }

  iosArm64()
  iosSimulatorArm64()

  jvm()

  js { nodejs() }

  sourceSets {
    commonMain {
      dependencies {
        api(libs.ktor.client.core)
        implementation(libs.kotlinx.serialization.json)
      }
    }
  }
}
