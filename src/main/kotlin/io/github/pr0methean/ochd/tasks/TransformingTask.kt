package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.tasks.caching.TaskCache
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import org.apache.logging.log4j.LogManager
import java.util.*
import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success

private val logger = LogManager.getLogger("TransformingTask")
private val UNIT_SUCCESS = success(Unit)
abstract class TransformingTask<T, U>(
    name: String,
    val base: Task<T>,
    cache: TaskCache<U>
)
        : AbstractTask<U>(name, cache) {

    override suspend fun createCoroutineAsync(): Deferred<Result<U>> {
        val myBase = base
        return getCoroutineScope().async(start = CoroutineStart.LAZY) {
            val result = try {
                logger.debug("Awaiting {} to transform it in {}", myBase, this@TransformingTask)
                val input = myBase.await()
                logger.debug("Got {} from {}; transforming it in {}", input, myBase, this@TransformingTask)
                val result = transform(input.getOrThrow())
                if (result === Unit) {
                    UNIT_SUCCESS
                }
                success(result)
            } catch (t: Throwable) {
                logger.error("Exception in {}", this@TransformingTask, t)
                failure(t)
            }
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