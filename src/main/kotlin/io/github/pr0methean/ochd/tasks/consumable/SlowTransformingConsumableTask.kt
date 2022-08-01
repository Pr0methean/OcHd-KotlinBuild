package io.github.pr0methean.ochd.tasks.consumable

import io.github.pr0methean.ochd.tasks.consumable.caching.TaskCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success

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

    override suspend fun createCoroutineAsync(coroutineScope: CoroutineScope): Deferred<Result<U>> {
        return coroutineScope.async(start = CoroutineStart.LAZY) {
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

    @Suppress("UNCHECKED_CAST")
    override suspend fun mergeWithDuplicate(other: ConsumableTask<U>): ConsumableTask<U> {
        return if (other is SlowTransformingConsumableTask<*, *> && System.identityHashCode(this) < System.identityHashCode(other)) {
            base.mergeWithDuplicate(other.base as ConsumableTask<T>)
            this
        } else {
            super.mergeWithDuplicate(other)
        }
    }

    override suspend fun clearFailure() {

        base.clearFailure()
        super.clearFailure()
    }
}