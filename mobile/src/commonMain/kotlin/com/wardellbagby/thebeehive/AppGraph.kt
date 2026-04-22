package com.wardellbagby.thebeehive

import com.wardellbagby.thebeehive.client.createDefaultHttpClient
import com.wardellbagby.thebeehive.client.createDefaultJson
import com.wardellbagby.thebeehive.notifications.FcmTokenProvider
import com.wardellbagby.thebeehive.service.BeehiveService
import com.wardellbagby.thebeehive.service.BeehiveServiceClient
import com.wardellbagby.thebeehive.service.BeehiveServiceClientFactory
import com.wardellbagby.thebeehive.settings.SettingsRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import io.ktor.client.HttpClient
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.Json

@OptIn(ExperimentalTime::class)
@DependencyGraph(AppScope::class)
interface AppGraph {
  val appPresenter: AppPresenter
  val clock: Clock
  val timeZone: TimeZone

  @Provides fun json(): Json = createDefaultJson()

  @Provides fun httpClient(json: Json): HttpClient = createDefaultHttpClient(json)

  @Provides fun provideClock(): Clock = Clock.System

  @Provides fun provideTimeZone(): TimeZone = TimeZone.currentSystemDefault()

  @Provides
  fun service(
    factory: BeehiveServiceClientFactory,
    settingsRepository: SettingsRepository,
  ): BeehiveServiceClient {
    return factory.create(settingsRepository.settings().hostname)
  }

  @Provides fun beehiveService(client: BeehiveServiceClient): BeehiveService = client

  @Provides fun provideFcmTokenProvider(): FcmTokenProvider = FcmTokenProvider()
}
