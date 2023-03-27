package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.tasks.caching.noopDeferredTaskCache
import javafx.scene.image.Image
import org.apache.logging.log4j.LogManager
import org.jgrapht.Graph
import org.jgrapht.graph.DefaultEdge
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext

private val logger = LogManager.getLogger("PngOutputTask")
val mkdirsedPaths: MutableSet<File> = ConcurrentHashMap.newKeySet()

/**
 * Task that saves an image to one or more PNG files.
 */
@Suppress("BlockingMethodInNonBlockingContext", "EqualsWithHashCodeExist", "EqualsOrHashCode")
class PngOutputTask(
    name: String,
    val base: AbstractTask<Image>,
    val files: List<File>,
    ctx: CoroutineContext, graph: Graph<AbstractTask<*>, DefaultEdge>,
): AbstractTask<Nothing>(name, noopDeferredTaskCache(), ctx, graph) {
    override val directDependencies: Iterable<AbstractTask<*>> = listOf(base)
    override fun mergeWithDuplicate(other: AbstractTask<*>): AbstractTask<Nothing> {
        if (other is PngOutputTask && other !== this && other.base !== base) {
            logger.debug("Merging PngOutputTask {} with duplicate {}", name, other.name)
            val mergedBase = base.mergeWithDuplicate(other.base)
            if (mergedBase !== base || files.toSet() != other.files.toSet()) {
                return PngOutputTask(
                    name,
                    mergedBase,
                    files.union(other.files).toList(),
                    ctx,
                    graph
                )
            }
        }
        return super.mergeWithDuplicate(other)
    }

    @Suppress("MagicNumber")
    override fun computeHashCode(): Int = base.hashCode() - 127

    override fun equals(other: Any?): Boolean {
        return (this === other) || other is PngOutputTask && base == other.base
    }

    override suspend fun perform(): Nothing = error("The output task is defined in Main.kt")

    init {
        check(files.isNotEmpty()) { "PngOutputTask $name has no destination files" }
    }
}
