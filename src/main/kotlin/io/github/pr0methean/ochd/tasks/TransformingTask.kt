package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.tasks.caching.TaskCache
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import java.util.*

abstract class TransformingTask<T, U>(
    name: String,
    val base: Task<T>,
    cache: TaskCache<U>
)
        : AbstractTask<U>(name, cache) {

    override suspend fun createCoroutineAsync(): Deferred<Result<U>> {
        val myBase = base
        return getCoroutineScope().async(start = CoroutineStart.LAZY) {
            val result = runCatching { transform(myBase.await().getOrThrow()) }
            emit(result)
            result
        }
    }

    abstract suspend fun transform(input: T): U

    override fun equals(other: Any?): Boolean {
        return (other === this) || (other is TransformingTask<*, *>
                && javaClass == other.javaClass
                && base == other.base)
    }

    override fun hashCode(): Int = Objects.hash(javaClass, base)

    @Suppress("UNCHECKED_CAST")
    override suspend fun mergeWithDuplicate(other: Task<*>): Task<U> {
        val deduped = super.mergeWithDuplicate(other) as TransformingTask<T, U>
        if (deduped !== other && other is TransformingTask<*, *> && deduped.base == other.base) {
            deduped.base.mergeWithDuplicate(other.base as Task<T>)
        }
        return deduped
    }

    override suspend fun removeDirectDependentTask(task: Task<*>) {
        super.removeDirectDependentTask(task)
        // Relies on the fact that a PngCompressionTask has only one consumer
        base.removeDirectDependentTask(this)
    }

    override val directDependencies: List<Task<T>> = listOf(base)
}
