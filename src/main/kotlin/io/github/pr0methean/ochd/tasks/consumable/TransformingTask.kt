package io.github.pr0methean.ochd.tasks.consumable

import io.github.pr0methean.ochd.tasks.consumable.caching.TaskCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import org.apache.logging.log4j.LogManager
import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success

private val logger = LogManager.getLogger("TransformingTask")
open class TransformingTask<T, U>(
    name: String,
    open val base: ConsumableTask<T>,
    open val cache: TaskCache<U>,
    val transform: (T) -> U
)
        : AbstractConsumableTask<U>(name, cache) {

    override fun getNow(): Result<U>? {
        base.getNow()
        return super.getNow()
    }

    @Suppress("DeferredResultUnused")
    override suspend fun startPrerequisites() {
        base.startAsync()
    }

    override suspend fun createCoroutineAsync(coroutineScope: CoroutineScope): Deferred<Result<U>> {
        return coroutineScope.async(start = CoroutineStart.LAZY) {
            val result = try {
                logger.debug("Awaiting {} to transform it in {}", base, this)
                val input = base.await()
                logger.debug("Got {} from {}; transforming it in {}", input, base, this)
                if (input.isSuccess) {
                    success(transform(input.getOrThrow()))
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
    override suspend fun mergeWithDuplicate(other: ConsumableTask<U>): ConsumableTask<U> {
        if (other is TransformingTask<*, *>) {
            base.mergeWithDuplicate(other.base as ConsumableTask<T>)
        }
        return super.mergeWithDuplicate(other)
    }

    override suspend fun checkSanity() {
        base.checkSanity()
        super.checkSanity()
    }

    override suspend fun clearFailure() {
        base.clearFailure()
        super.clearFailure()
    }
}