package com.wardellbagby.thebeehive.service

import dev.zacsweers.metro.Inject
import io.ktor.client.HttpClient

@Inject
class BeehiveServiceClientFactory(private val client: HttpClient) {
  fun create(hostname: String) = BeehiveServiceClient(client = client, baseUrl = hostname)
}
