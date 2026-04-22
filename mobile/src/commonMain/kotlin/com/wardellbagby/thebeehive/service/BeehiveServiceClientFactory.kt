package com.wardellbagby.thebeehive.service

import dev.zacsweers.metro.Inject
import io.ktor.client.HttpClient
import io.ktor.http.Url

@Inject
class BeehiveServiceClientFactory(private val client: HttpClient) {
  fun create(hostname: String) = BeehiveServiceClient(client = client, baseUrl = Url(hostname))
}
