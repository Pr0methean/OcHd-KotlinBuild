package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.tasks.caching.TaskCache
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import org.apache.logging.log4j.LogManager

private val logger = LogManager.getLogger("SimpleTask")
abstract class SimpleTask<T>(name: String, cache: TaskCache<T>) : AbstractTask<T>(
    name, cache
) {
    abstract suspend fun perform(): T

    override suspend fun createCoroutineAsync(): Deferred<Result<T>> {
        return getCoroutineScope().async (start = CoroutineStart.LAZY) {
            val result = try {
                Result.success(perform())
            } catch (t: Throwable) {
                logger.error("Exception in {}", this, t)
                Result.failure(t)
            }
            emit(result)
            result
        }
    }

}