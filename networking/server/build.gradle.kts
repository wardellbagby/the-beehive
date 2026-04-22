@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.ksp)
}

kotlin {
  jvm()

  sourceSets {
    commonMain {
      dependencies {
        api(projects.networking.core)
        api(libs.ktor.server.core)
      }
    }
    jvmMain {
      dependencies {
        api(libs.ktor.server.websockets)
        implementation(libs.kotlinx.serialization.json)
      }
      kotlin.srcDir("${projectDir}/build/generated/ksp/jvm/jvmMain/kotlin")
    }
  }
}

// KSP runs against JVM so it can resolve BeehiveService from networking:core's compiled JVM jar.
dependencies { add("kspJvm", projects.servicesGenerator) }

ksp { arg("generate", "server") }

tasks.withType<KotlinCompilationTask<*>>().configureEach {
  if (name != "kspKotlinJvm") {
    dependsOn("kspKotlinJvm")
  }
}
