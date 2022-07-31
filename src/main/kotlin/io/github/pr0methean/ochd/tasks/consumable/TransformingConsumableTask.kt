package io.github.pr0methean.ochd.tasks.consumable

import io.github.pr0methean.ochd.tasks.consumable.caching.TaskCache
import kotlinx.coroutines.*
import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success

open class TransformingConsumableTask<T, U>(
    open val base: ConsumableTask<T>,
    open val cache: TaskCache<U>,
    val transform: (T) -> U
)
        : AbstractConsumableTask<U>(base.name, cache) {
    private fun wrappingTransform(it: Result<T>): Result<U> {
        return if (it.isSuccess) {
            try {
                success(transform(it.getOrThrow()))
            } catch (t: Throwable) {
                failure(t)
            }
        } else {
            failure(it.exceptionOrNull()!!)
        }
    }

    override fun getNow(): Result<U>? {
        base.getNow()
        return super.getNow()
    }

    override suspend fun startAsync(): Deferred<Result<U>> {
        base.startAsync()
        return super.startAsync()
    }

    override suspend fun createCoroutineAsync(): Deferred<Result<U>>
            = CoroutineScope(currentCoroutineContext().plus(CoroutineName(name)).plus(SupervisorJob()))
        .async {
            val result = wrappingTransform(base.await())
            emit(result)
            result
        }

    override suspend fun clearFailure() {
        base.clearFailure()
        super.clearFailure()
    }
}