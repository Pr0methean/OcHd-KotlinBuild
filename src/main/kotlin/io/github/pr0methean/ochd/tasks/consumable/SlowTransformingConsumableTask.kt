package io.github.pr0methean.ochd.tasks.consumable

import io.github.pr0methean.ochd.tasks.consumable.caching.TaskCache
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import org.apache.logging.log4j.LogManager
import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success

private val logger = LogManager.getLogger("SlowTransformingConsumableTask")
open class SlowTransformingConsumableTask<T, U>(
    name: String,
    open val base: ConsumableTask<T>,
    open val cache: TaskCache<U>,
    val transform: suspend (T) -> U
)
        : AbstractConsumableTask<U>(name, cache) {

    private suspend fun wrappingTransform(it: Result<T>): Result<U> {
        return if (it.isSuccess) {
            success(transform(it.getOrThrow()))
        } else {
            failure(it.exceptionOrNull()!!)
        }
    }

    override suspend fun checkSanity() {
        base.checkSanity()
        super.checkSanity()
    }

    override suspend fun createCoroutineAsync(): Deferred<Result<U>> {
        val attempt = attemptNumber.incrementAndGet()
        return createCoroutineScope(attempt).async(start = CoroutineStart.LAZY) {
            val result = try {
                wrappingTransform(base.await())
            } catch (t: Throwable) {
                failure(t)
            }
            emit(result)
            result
        }
    }

    @Suppress("DeferredResultUnused")
    override suspend fun startAsync(): Deferred<Result<U>> {
        base.startAsync()
        return super.startAsync()
    }

    override fun getNow(): Result<U>? {
        base.getNow()
        return super.getNow()
    }

    override suspend fun clearFailure() {

        base.clearFailure()
        super.clearFailure()
    }
}