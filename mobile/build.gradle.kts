import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.androidLibrary)
  alias(libs.plugins.composeMultiplatform)
  alias(libs.plugins.composeCompiler)
  alias(libs.plugins.metro)
}

kotlin {
  android {
    namespace = "com.wardellbagby.thebeehive"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    minSdk = libs.versions.android.minSdk.get().toInt()

    compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }

    androidResources { enable = true }
  }

  listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
    iosTarget.binaries.framework {
      baseName = "ComposeApp"
      isStatic = true
    }
  }

  sourceSets {
    androidMain {
      dependencies {
        implementation(project.dependencies.platform(libs.firebase.bom))
        implementation(libs.coil3.gif)

        implementation(libs.androidx.activity.compose)
        implementation(libs.kotlinx.coroutines.play.services)
        implementation(libs.firebase.messaging.ktx)
        implementation(libs.ktor.client.cio)
      }
    }
    iosMain { dependencies { implementation(libs.ktor.client.darwin) } }
    commonMain.dependencies {
      implementation(libs.compose.runtime)
      implementation(libs.compose.foundation)
      implementation(libs.compose.material3)
      implementation(libs.compose.ui)
      implementation(libs.compose.components.resources)
      implementation(libs.compose.uiToolingPreview)
      implementation(libs.androidx.lifecycle.viewmodelCompose)
      implementation(libs.androidx.lifecycle.runtimeCompose)
      implementation(libs.cashapp.molecule.runtime)
      implementation(libs.metro.runtime)
      implementation(libs.compose.icons.font.awesome)
      implementation(libs.ktor.client.core)
      implementation(libs.ktor.client.contentNegotiation)
      implementation(libs.ktor.serializationKotlinxJson)
      implementation(libs.multiplatform.settings)
      implementation(libs.coil3.compose)
      implementation(libs.coil3.network.ktor)
      implementation(libs.kotlinx.datetime)
      implementation(projects.networking.client)
      implementation(libs.napier)
    }
    commonTest.dependencies { implementation(libs.kotlin.test) }
  }
}

dependencies { androidRuntimeClasspath(libs.compose.uiTooling) }
