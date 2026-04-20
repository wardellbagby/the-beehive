@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins { alias(libs.plugins.kotlinMultiplatform) }

kotlin {
  js {
    nodejs()
    binaries.library()
    generateTypeScriptDefinitions()
    useEsModules()
  }
  sourceSets {
    jsMain {
      dependencies {
        implementation(projects.networking.client)
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.ktor.client.js)
        implementation(libs.kotlinx.serialization.json)
      }
    }
  }
}

tasks.register("dist") { dependsOn("jsNodeProductionLibraryDistribution") }

tasks.register<Exec>("npmLink") {
  workingDir(projectDir)
  commandLine("npm", "link")
}

tasks.register<Exec>("npmUnlink") {
  workingDir(projectDir)
  commandLine("npm", "unlink")
}
