package com.wardellbagby.thebeehive

import com.wardellbagby.thebeehive.status.JobId
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.SingleIn
import kotlin.collections.forEach
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job as CoroutineJob
import kotlinx.coroutines.job
import kotlinx.coroutines.launch

@SingleIn(AppScope::class)
class JobManager(private val jobs: Set<Job>, private val scope: CoroutineScope) {
  private val jobErrors = mutableMapOf<JobId, String?>()
  private val activeCoroutineJobs = mutableMapOf<JobId, CoroutineJob>()
  private val logger = getLogger()

  fun startAll() {
    jobs.forEach { start(it.id) }
  }

  fun isStarted(id: JobId) = activeCoroutineJobs.contains(id)

  fun jobStatuses() = jobs.associate { it.id to isStarted(it.id) }

  fun getErrorMessage(id: JobId): String? = jobErrors[id]

  fun start(id: JobId): Boolean {
    val job = jobs.firstOrNull { it.id == id } ?: return false

    if (activeCoroutineJobs.contains(id)) {
      logger.warn("Not starting already started job $id.")
      return false
    }

    logger.debug("Starting $id job: ${job::class.simpleName}")
    jobErrors.remove(id)
    activeCoroutineJobs[id] =
      scope
        .launch { job.run() }
        .also { coroutineJob ->
          coroutineJob.invokeOnCompletion { throwable ->
            if (throwable != null && throwable !is CancellationException) {
              logger.warn("Error in job ${job.id}", throwable)
              activeCoroutineJobs.remove(job.id)
              jobErrors[job.id] = throwable.message
            }
          }
        }
    return true
  }

  fun stop(id: JobId): Boolean {
    if (activeCoroutineJobs.containsKey(id)) {
      jobErrors.remove(id)
      activeCoroutineJobs.remove(id)?.cancel()

      return true
    }
    return false
  }
}
