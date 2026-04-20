@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.androidLibrary)
  alias(libs.plugins.ksp)
}

kotlin {
  android {
    namespace = "com.wardellbagby.thebeehive.networking.client"
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
      kotlin {
        srcDir("${projectDir}/build/generated/ksp/metadata/commonMain/kotlin")
        exclude("**/service/server/**")
      }
      dependencies {
        api(libs.ktor.client.core)
        api(libs.ktor.client.websockets)
        api(projects.networking.core)

        implementation(libs.ktor.serializationKotlinxJson)
        implementation(libs.ktor.client.contentNegotiation)
        implementation(libs.ktor.clientLogging)
        implementation(libs.napier)
      }
    }
  }
}

dependencies { add("kspCommonMainMetadata", projects.servicesGenerator) }

ksp { arg("generate", "client") }

tasks.withType<KotlinCompilationTask<*>>().configureEach {
  if (name != "kspCommonMainKotlinMetadata") {
    dependsOn("kspCommonMainKotlinMetadata")
  }
}
