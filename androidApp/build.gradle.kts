plugins {
  alias(libs.plugins.androidApplication)
  alias(libs.plugins.googleServices)
}

android {
  namespace = "com.wardellbagby.thebeehive.app"
  compileSdk = libs.versions.android.compileSdk.get().toInt()

  defaultConfig {
    applicationId = "com.wardellbagby.thebeehive"
    minSdk = libs.versions.android.minSdk.get().toInt()
    targetSdk = libs.versions.android.targetSdk.get().toInt()
    versionCode = 3
    versionName = "0.0.2"
  }
  packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
  buildTypes { getByName("release") { isMinifyEnabled = false } }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
}

dependencies {
  implementation(projects.mobile)
  implementation(platform(libs.firebase.bom))
  implementation(libs.firebase.messaging.ktx)
}
