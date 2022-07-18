package io.github.pr0methean.ochd

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.yield
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.util.Unbox

private val logger = LogManager.getLogger("Retryer")
class Retryer(val stats: ImageProcessingStats) {
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
                logger.error("Yielding before retrying {} ({} failed attempts)", name, Unbox.box(failedAttempts), t)
                yield()
                logger.info("Retrying: {}", name)
            }
        }

        return result!!
    }
}