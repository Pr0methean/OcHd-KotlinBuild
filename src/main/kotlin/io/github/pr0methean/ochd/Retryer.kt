package io.github.pr0methean.ochd

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.util.Unbox
import java.lang.Thread.sleep
import kotlin.math.min

private val logger = LogManager.getLogger("Retryer")
class Retryer(val stats: ImageProcessingStats) {
    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun <T> retrying(name: String, task: suspend () -> T): T {
        var completed = false
        var result: T? = null
        var failedAttempts = 0
        while (!completed) {
            try {
                result = task()
                completed = true
            } catch (t: CancellationException) {
                throw t
            } catch (t: Throwable) {
                failedAttempts++
                stats.retries.increment()
                val delay = 1.shl(min(failedAttempts, 20)).toLong()
                logger.error("Blocking for {} ms before retrying {} ({} failed attempts)", delay, name, Unbox.box(failedAttempts), t)
                System.gc()
                withContext(MEMORY_INTENSE_COROUTINE_CONTEXT) {
                    // Delay the introduction of another real task so we can catch up
                    sleep(delay)
                    yield()
                }
                yield()
                logger.info("Retrying: {}", name)
            }
        }

        return result!!
    }
}