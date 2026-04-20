package com.wardellbagby.thebeehive

import com.wardellbagby.thebeehive.status.JobId

/**
 * Intended for long-running work that will run potentially forever.
 *
 * Jobs are started immediately upon server start.
 */
interface Job {
  val id: JobId

  suspend fun run()
}
