plugins { alias(libs.plugins.kotlinJvm) }

dependencies {
  implementation(libs.ksp.symbolProcessingApi)
  implementation(libs.kotlinpoet)
  implementation(libs.kotlinpoet.ksp)
}
