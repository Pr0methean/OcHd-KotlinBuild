package io.github.pr0methean.ochd

import com.google.common.collect.ConcurrentHashMultiset
import io.github.pr0methean.ochd.tasks.AbstractImageTask
import io.github.pr0methean.ochd.tasks.AbstractTask
import io.github.pr0methean.ochd.tasks.AnimationTask
import io.github.pr0methean.ochd.tasks.ImageStackingTask
import io.github.pr0methean.ochd.tasks.PngOutputTask
import io.github.pr0methean.ochd.tasks.RepaintTask
import io.github.pr0methean.ochd.tasks.SvgToBitmapTask
import io.github.pr0methean.ochd.tasks.caching.DeferredTaskCache
import io.github.pr0methean.ochd.tasks.caching.HardTaskCache
import io.github.pr0methean.ochd.tasks.caching.ReferenceTaskCache
import io.github.pr0methean.ochd.tasks.caching.noopDeferredTaskCache
import javafx.scene.image.Image
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import org.apache.logging.log4j.LogManager
import java.io.File
import java.lang.ref.SoftReference
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext

fun color(web: String): Color = Color.web(web)

fun color(web: String, alpha: Double): Color = Color.web(web, alpha)

private val logger = LogManager.getLogger("TaskPlanningContext")

private fun isHugeTileTask(name: String): Boolean = name.contains("commandBlock") || name.contains("4x")

/**
 * Holds info needed to build and deduplicate the task graph. Needs to become unreachable once the graph is built.
 */
class TaskPlanningContext(
    val name: String,
    val tileSize: Int,
    val svgDirectory: File,
    val outTextureRoot: File,
    val ctx: CoroutineContext
) {

    override fun toString(): String = name
    private val svgTasks: Map<String, SvgToBitmapTask>
    private val taskDeduplicationMap = ConcurrentHashMap<AbstractTask<*>, AbstractTask<*>>()
    private val dedupedSvgTasks = ConcurrentHashMultiset.create<String>()
    val stats: ImageProcessingStats = ImageProcessingStats()

    fun createTaskCache(name: String): DeferredTaskCache<Image> {
        return ReferenceTaskCache(name, ::SoftReference)
    }

    private fun createSvgToBitmapTaskCache(shortName: String): DeferredTaskCache<Image> {
        return if (isHugeTileTask(shortName)) {
            createTaskCache(shortName)
        } else HardTaskCache(shortName)
    }

    init {
        val builder = mutableMapOf<String, SvgToBitmapTask>()
        svgDirectory.list()!!.forEach { svgFile ->
            val shortName = svgFile.removeSuffix(".svg")
            builder[shortName] = SvgToBitmapTask(
                shortName,
                tileSize,
                svgDirectory.resolve("$shortName.svg"),
                createSvgToBitmapTaskCache(shortName),
                ctx,
                stats
            )
        }
        svgTasks = builder.toMap()
    }

    @Suppress("UNCHECKED_CAST")
    tailrec suspend fun <T, TTask : AbstractTask<T>> deduplicate(task: AbstractTask<T>): TTask = when {
        task is SvgToBitmapTask
        -> findSvgTask(task.name) as TTask
        task is RepaintTask
                && (task.paint == null || task.paint == Color.BLACK)
                && task.alpha == 1.0
        -> deduplicate(task.base)
        task is ImageStackingTask
                && task.layers.layers.size == 1
                && task.layers.background == Color.TRANSPARENT
        -> deduplicate(task.layers.layers[0] as TTask)
        else -> {
            val className = task::class.simpleName ?: "[unnamed class]"
            val deduped = taskDeduplicationMap.computeIfAbsent(task) {
                logger.info("New task: {}", task)
                stats.dedupeFailures.add(className)
                task as TTask
            }
            if (deduped !== task) {
                logger.info("Deduplicated: {}", task)
                stats.dedupeSuccesses.add(className)
                deduped.mergeWithDuplicate(task) as TTask
            } else deduped as TTask
        }
    }

    fun findSvgTask(name: String): SvgToBitmapTask {
        val task = svgTasks[name]
        requireNotNull(task) { "Missing SvgToBitmapTask for $name" }
        if (dedupedSvgTasks.add(name, 1) > 0) {
            stats.dedupeSuccesses.add("SvgToBitmapTask")
        } else {
            stats.dedupeFailures.add("SvgToBitmapTask")
        }
        return task
    }

    suspend inline fun layer(name: String, paint: Paint? = null, alpha: Double = 1.0): AbstractImageTask
            = layer(findSvgTask(name), paint, alpha)

    suspend inline fun layer(
        source: AbstractTask<Image>,
        paint: Paint? = null,
        alpha: Double = 1.0
    ): AbstractImageTask {
        return deduplicate(
            RepaintTask(
                deduplicate(source),
                paint,
                alpha,
                createTaskCache("$source@$paint@$alpha"),
                ctx,
                stats
            )) as AbstractImageTask
    }

    suspend inline fun stack(init: LayerListBuilder.() -> Unit): AbstractImageTask {
        val layerTasksBuilder = LayerListBuilder(this)
        layerTasksBuilder.init()
        val layerTasks = layerTasksBuilder.build()
        return stack(layerTasks)
    }

    suspend inline fun animate(background: AbstractImageTask, frames: List<AbstractImageTask>): AbstractImageTask {
        return deduplicate(
            AnimationTask(
                deduplicate(background) as AbstractImageTask,
                frames.map { deduplicate(it) as AbstractImageTask },
                tileSize,
                tileSize,
                frames.toString(),
                noopDeferredTaskCache(),
                ctx,
                stats
            )) as AbstractImageTask
    }

    suspend inline fun out(source: AbstractImageTask, names: Array<String>): PngOutputTask {
        val lowercaseName = names.map { it.lowercase(Locale.ENGLISH) }
        val dedupedSource = deduplicate(source) as AbstractImageTask
        val orig =
            PngOutputTask(
                lowercaseName[0],
                dedupedSource,
                lowercaseName.map { outTextureRoot.resolve("$it.png") },
                ctx,
                stats
            )
        val deduped = deduplicate(orig) as PngOutputTask
        if (deduped === orig) {
            dedupedSource.addDirectDependentTask(deduped)
        }
        return deduped
    }

    suspend inline fun out(source: AbstractImageTask, name: String): PngOutputTask = out(source, arrayOf(name))

    suspend inline fun out(source: LayerListBuilder.() -> Unit, name: String): PngOutputTask
            = out(stack {source()}, arrayOf(name))

    suspend inline fun stack(layers: LayerList): AbstractImageTask = deduplicate(
        ImageStackingTask(
            layers,
            createTaskCache(layers.toString()),
            ctx,
            stats
        )
    ) as AbstractImageTask
}
