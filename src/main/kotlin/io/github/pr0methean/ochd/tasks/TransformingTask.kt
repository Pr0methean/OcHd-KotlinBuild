package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.tasks.caching.DeferredTaskCache
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import java.util.*
import kotlin.coroutines.CoroutineContext

/**
 * Task that takes the output of another task as input.
 * @param base the task that produces the input
 */
abstract class TransformingTask<T, U>(
    name: String,
    val base: Task<T>,
    cache: DeferredTaskCache<U>,
    ctx: CoroutineContext
)
        : AbstractTask<U>(name, cache, ctx) {

    override fun createCoroutineAsync(): Deferred<U> {
        val myBase = base
        return coroutineScope.async(start = CoroutineStart.LAZY) {
            try {
                return@async transform(myBase.await())
            } catch (t: Throwable) {
                logFailure(t)
                throw t
            }
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
        // Relies on the fact that a PngEncodingTask has only one consumer
        base.removeDirectDependentTask(this)
    }

    override val directDependencies: List<Task<T>> = listOf(base)
}
