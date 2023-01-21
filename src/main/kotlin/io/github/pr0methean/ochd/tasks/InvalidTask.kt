package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.tasks.caching.noopDeferredTaskCache
import kotlinx.coroutines.Dispatchers

/**
 * A task that throws an exception if it's ever launched.
 */
object InvalidTask: AbstractImageTask("InvalidTask", noopDeferredTaskCache(), Dispatchers.Unconfined, 0, 0) {
    override val directDependencies: Iterable<AbstractTask<*>> = listOf()

    override fun computeHashCode(): Int = 0xDEADBEEF.toInt()

    override suspend fun perform(): Nothing {
        throw IllegalArgumentException("InvalidTask")
    }
}
