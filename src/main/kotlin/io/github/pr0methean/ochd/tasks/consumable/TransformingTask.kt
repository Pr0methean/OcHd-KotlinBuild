package io.github.pr0methean.ochd.tasks.consumable

import io.github.pr0methean.ochd.tasks.consumable.caching.TaskCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import org.apache.logging.log4j.LogManager
import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success

private val logger = LogManager.getLogger("TransformingTask")
open class TransformingTask<T, U>(
    name: String,
    open val base: Task<T>,
    open val cache: TaskCache<U>,
    val transform: (T) -> U
)
        : AbstractTask<U>(name, cache) {

    override fun getNow(): Result<U>? {
        base.getNow()
        return super.getNow()
    }

    @Suppress("DeferredResultUnused")
    override suspend fun startPrerequisites() {
        base.startAsync()
    }

    override suspend fun createCoroutineAsync(coroutineScope: CoroutineScope): Deferred<Result<U>> {
        val myBase = base
        val myTransform = transform
        return coroutineScope.async {
            val result = try {
                logger.debug("Awaiting {} to transform it in {}", myBase, this)
                val input = myBase.await()
                logger.debug("Got {} from {}; transforming it in {}", input, myBase, this)
                if (input.isSuccess) {
                    success(myTransform(input.getOrThrow()))
                } else {
                    failure(input.exceptionOrNull()!!)
                }
            } catch (t: Throwable) {
                logger.error("Exception in {}", this, t)
                failure(t)
            }
            result
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun mergeWithDuplicate(other: Task<U>): Task<U> {
        if (other is TransformingTask<*, *>) {
            base.mergeWithDuplicate(other.base as Task<T>)
        }
        return super.mergeWithDuplicate(other)
    }

    override suspend fun clearFailure() {
        base.clearFailure()
        super.clearFailure()
    }
}