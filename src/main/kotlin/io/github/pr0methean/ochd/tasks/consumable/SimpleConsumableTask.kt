package io.github.pr0methean.ochd.tasks.consumable

import io.github.pr0methean.ochd.tasks.consumable.caching.TaskCache
import kotlinx.coroutines.*

abstract class SimpleConsumableTask<T>(name: String, cache: TaskCache<T>) : AbstractConsumableTask<T>(
    name, cache
) {
    abstract suspend fun perform(): T

    override suspend fun createCoroutineAsync() =
        CoroutineScope(currentCoroutineContext().plus(CoroutineName(name)).plus(SupervisorJob()))
            .async {
                val result = try {
                    Result.success(perform())
                } catch (t: Throwable) {
                    Result.failure(t)
                }
                emit(result)
                result
            }
}