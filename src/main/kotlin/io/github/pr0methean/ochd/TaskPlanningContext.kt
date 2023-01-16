package io.github.pr0methean.ochd

import com.google.common.collect.ConcurrentHashMultiset
import io.github.pr0methean.ochd.tasks.AbstractImageTask
import io.github.pr0methean.ochd.tasks.AbstractTask
import io.github.pr0methean.ochd.tasks.AnimationTask
import io.github.pr0methean.ochd.tasks.ImageStackingTask
import io.github.pr0methean.ochd.tasks.PngOutputTask
import io.github.pr0methean.ochd.tasks.RepaintTask
import io.github.pr0methean.ochd.tasks.SvgToBitmapTask
import io.github.pr0methean.ochd.tasks.caching.SoftTaskCache
import io.github.pr0methean.ochd.tasks.caching.noopDeferredTaskCache
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import org.apache.logging.log4j.LogManager
import java.io.File
import java.util.Locale
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.coroutines.CoroutineContext

fun color(web: String): Color = Color.web(web)

fun color(web: String, alpha: Double): Color = Color.web(web, alpha)

private val logger = LogManager.getLogger("TaskPlanningContext")

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
    private val taskDeduplicationMap = mutableMapOf<AbstractTask<*>, AbstractTask<*>>()
    private val taskDeduplicationLock = ReentrantLock()
    private val dedupedSvgTasks = ConcurrentHashMultiset.create<String>()
    val stats: ImageProcessingStats = ImageProcessingStats()

    init {
        val builder = mutableMapOf<String, SvgToBitmapTask>()
        svgDirectory.list()!!.forEach { svgFile ->
            val shortName = svgFile.removeSuffix(".svg")
            builder[shortName] = SvgToBitmapTask(
                shortName,
                tileSize,
                svgDirectory.resolve("$shortName.svg"),
                SoftTaskCache(shortName),
                ctx,
                stats
            )
        }
        svgTasks = builder.toMap()
    }

    @Suppress("UNCHECKED_CAST")
    tailrec fun <T, TTask : AbstractTask<T>> deduplicate(task: AbstractTask<T>): TTask {
        logger.debug("Deduplicating: {}", task)
        
        return when {
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
                logger.debug("Main deduplication branch for {}", task)
                val className = task::class.simpleName ?: "[unnamed class]"
                return taskDeduplicationLock.withLock {
                    taskDeduplicationMap.compute(task) { _, oldValue ->
                        if (oldValue === task) {
                            logger.debug("{} already points to itself in the deduplication map", task)
                            return@compute task
                        }
                        oldValue?.mergeWithDuplicate(task)?.also {
                            logger.info("Deduplicated: {}", task)
                            stats.dedupeSuccesses.add(className)
                        } ?: task.also {
                            logger.info("New task: {}", task)
                            stats.dedupeFailures.add(className)
                        }
                    }
                } as TTask
            }
        }
    }

    fun findSvgTask(name: String): SvgToBitmapTask {
        logger.debug("Looking up SvgToBitmapTask for {}", name)
        val task = svgTasks[name]
        requireNotNull(task) { "Missing SvgToBitmapTask for $name" }
        if (dedupedSvgTasks.add(name, 1) > 0) {
            stats.dedupeSuccesses.add("SvgToBitmapTask")
        } else {
            stats.dedupeFailures.add("SvgToBitmapTask")
        }
        logger.debug("Found SvgToBitmapTask for {}", name)
        return task
    }

    fun layer(name: String, paint: Paint? = null, alpha: Double = 1.0): AbstractImageTask
            = layer(findSvgTask(name), paint, alpha)

    fun layer(
        source: AbstractImageTask,
        paint: Paint? = null,
        alpha: Double = 1.0
    ): AbstractImageTask {
        logger.debug("layer({},{},{})", source, paint, alpha)
        return deduplicate(
            RepaintTask(deduplicate(source), paint, alpha, SoftTaskCache("$source@$paint@$alpha"), ctx, stats))
    }

    inline fun stack(init: LayerListBuilder.() -> Unit): AbstractImageTask {
        val layerTasksBuilder = LayerListBuilder(this)
        layerTasksBuilder.init()
        val layerTasks = layerTasksBuilder.build()
        return stack(layerTasks)
    }

    fun animate(background: AbstractImageTask, frames: List<AbstractImageTask>): AbstractImageTask {
        logger.debug("animate({}, {})", background, frames)
        return deduplicate(AnimationTask(
            deduplicate(background),
            frames.map(::deduplicate),
            tileSize,
            tileSize,
            frames.toString(),
            noopDeferredTaskCache(),
            ctx,
            stats
        ))
    }

    fun out(source: AbstractImageTask, names: Array<String>): PngOutputTask {
        logger.debug("out({}, {})", source, names)
        val lowercaseNames = names.map { it.lowercase(Locale.ENGLISH) }
        return deduplicate(PngOutputTask(
                lowercaseNames[0],
                deduplicate(source),
                lowercaseNames.map { outTextureRoot.resolve("$it.png") },
                ctx,
                stats
            )).also { logger.debug("Done creating output task: {}", it) } as PngOutputTask
    }

    fun out(source: AbstractImageTask, name: String): PngOutputTask = out(source, arrayOf(name))

    inline fun out(source: LayerListBuilder.() -> Unit, name: String): PngOutputTask
            = out(stack {source()}, arrayOf(name))

    fun stack(layers: LayerList): AbstractImageTask {
        logger.debug("stack({})", layers)
        return deduplicate(ImageStackingTask(
            layers, SoftTaskCache(layers.toString()), ctx, stats))
    }
}
