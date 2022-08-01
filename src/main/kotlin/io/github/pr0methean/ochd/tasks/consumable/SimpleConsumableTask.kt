package io.github.pr0methean.ochd.tasks.consumable

import io.github.pr0methean.ochd.tasks.consumable.caching.TaskCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async

abstract class SimpleConsumableTask<T>(name: String, cache: TaskCache<T>) : AbstractConsumableTask<T>(
    name, cache
) {
    abstract suspend fun perform(): T

    override suspend fun createCoroutineAsync(coroutineScope: CoroutineScope): Deferred<Result<T>> {
        return coroutineScope.async(start = CoroutineStart.LAZY) {
                val result = try {
                    Result.success(perform())
                } catch (t: Throwable) {
                    Result.failure(t)
                }
                emit(result)
                result
            }
    }

}