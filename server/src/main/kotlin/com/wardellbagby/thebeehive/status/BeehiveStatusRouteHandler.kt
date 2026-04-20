package com.wardellbagby.thebeehive.status

import com.wardellbagby.thebeehive.JobManager
import dev.zacsweers.metro.Inject

@Inject
class BeehiveStatusRouteHandler(private val jobManager: JobManager) {
  fun beehiveStatus(): BeehiveStatusResponse {
    return BeehiveStatusResponse(
      ok = true,
      jobs =
        jobManager.jobStatuses().mapValues { (id, enabled) ->
          JobStatus(enabled = enabled, errorMessage = jobManager.getErrorMessage(id))
        },
    )
  }
}
