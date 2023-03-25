package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.tasks.caching.noopDeferredTaskCache
import kotlinx.coroutines.Dispatchers
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge

private val emptyGraph = DefaultDirectedGraph<AbstractTask<*>,DefaultEdge>(DefaultEdge::class.java)

/**
 * A task that throws an exception if it's ever launched.
 */
object InvalidTask: AbstractImageTask("InvalidTask", noopDeferredTaskCache(), Dispatchers.Unconfined, 0, 0,
    emptyGraph) {
    override fun hasColor(): Boolean = false

    override val directDependencies: Iterable<AbstractTask<*>> = listOf()

    override fun computeHashCode(): Int = 0xDEADBEEF.toInt()

    override suspend fun perform(): Nothing = error("InvalidTask")
}
