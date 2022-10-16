package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.tasks.caching.TaskCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import org.apache.logging.log4j.LogManager
import java.util.*
import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success

private val logger = LogManager.getLogger("TransformingTask")
open class TransformingTask<T, U>(
    name: String,
    val base: Task<T>,
    cache: TaskCache<U>,
    val transform: suspend (T) -> U
)
        : AbstractTask<U>(name, cache) {

    override suspend fun createCoroutineAsync(coroutineScope: CoroutineScope): Deferred<Result<U>> {
        val myBase = base
        val myTransform = transform
        return coroutineScope.async(start = CoroutineStart.LAZY) {
            val result = try {
                logger.debug("Awaiting {} to transform it in {}", myBase, this)
                val input = myBase.await()
                logger.debug("Got {} from {}; transforming it in {}", input, myBase, this)
                success(myTransform(input.getOrThrow()))
            } catch (t: Throwable) {
                logger.error("Exception in {}", this, t)
                failure(t)
            }
            result
        }
    }

    override fun equals(other: Any?): Boolean {
        return (other === this) || (other is TransformingTask<*, *>
                && transform.javaClass == other.transform.javaClass
                && base == other.base)
    }

    override fun hashCode(): Int {
        return Objects.hash(javaClass, base)
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun mergeWithDuplicate(other: Task<U>): Task<U> {
        val deduped = super.mergeWithDuplicate(other)
        if (other !== deduped && other is TransformingTask<*, U>) {
            (deduped as TransformingTask<T, U>).base.mergeWithDuplicate(other.base as Task<T>)
        }
        return deduped
    }

    override val directDependencies: List<Task<T>> = listOf(base)

    override suspend fun clearFailure() {
        base.clearFailure()
        super.clearFailure()
    }
}