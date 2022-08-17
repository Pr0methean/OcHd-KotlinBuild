package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.tasks.caching.TaskCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import org.apache.logging.log4j.LogManager
import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success

private val logger = LogManager.getLogger("SlowTransformingTask")
open class AsyncTransformingTask<T, U>(
    name: String,
    open val base: Task<T>,
    cache: TaskCache<U>,
    val transform: suspend (T) -> U
)
        : AbstractTask<U>(name, cache) {

    override suspend fun createCoroutineAsync(coroutineScope: CoroutineScope): Deferred<Result<U>> {
        val myBase = base
        val myTransform = transform
        return coroutineScope.async(start = CoroutineStart.LAZY) {
            val result = try {
                logger.debug("Awaiting {} to transform it in {}", myBase, this)
                val input = myBase.await()
                logger.debug("Got {} from {}; transforming it in {}", input, myBase, this)
                success(myTransform(input.getOrThrow()))
            } catch (t: Throwable) {
                logger.error("Exception in {}", this, t)
                failure(t)
            }
            result
        }
    }

    @Suppress("DeferredResultUnused")
    override suspend fun startPrerequisites() {
        base.startAsync()
    }

    override fun getNow(): Result<U>? {
        base.getNow()
        return super.getNow()
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun mergeWithDuplicate(other: Task<U>): Task<U> {
        val deduped = super.mergeWithDuplicate(other)
        if (other !== deduped && other is TransformingTask<*, U>) {
            (deduped as TransformingTask<T, U>).base.mergeWithDuplicate(other.base as Task<T>)
        }
        return deduped
    }

    override suspend fun clearFailure() {
        base.clearFailure()
        super.clearFailure()
    }
}