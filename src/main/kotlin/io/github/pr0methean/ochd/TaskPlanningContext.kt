package io.github.pr0methean.ochd

import com.google.common.collect.HashMultiset
import io.github.pr0methean.ochd.tasks.AbstractImageTask
import io.github.pr0methean.ochd.tasks.AbstractTask
import io.github.pr0methean.ochd.tasks.AnimationTask
import io.github.pr0methean.ochd.tasks.ImageStackingTask
import io.github.pr0methean.ochd.tasks.InvalidTask
import io.github.pr0methean.ochd.tasks.MakeSemitransparentTask
import io.github.pr0methean.ochd.tasks.PngOutputTask
import io.github.pr0methean.ochd.tasks.RepaintTask
import io.github.pr0methean.ochd.tasks.SvgToBitmapTask
import io.github.pr0methean.ochd.tasks.caching.HardTaskCache
import io.github.pr0methean.ochd.tasks.caching.noopDeferredTaskCache
import javafx.scene.paint.Color
import javafx.scene.paint.Color.BLACK
import javafx.scene.paint.Paint
import org.apache.logging.log4j.LogManager
import java.io.File
import java.util.Locale
import kotlin.coroutines.CoroutineContext

fun color(web: String): Color = Color.web(web)

fun color(web: String, alpha: Double): Color = Color.web(web, alpha)

private val logger = LogManager.getLogger("TaskPlanningContext")

/**
 * Holds info needed to build and deduplicate the task graph. Needs to become unreachable once the graph is built.
 */
@Suppress("TooManyFunctions") // This class needs lots of overloads of layer(), stack() and out()
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
    private val dedupedSvgTasks = HashMultiset.create<String>()

    init {
        val builder = mutableMapOf<String, SvgToBitmapTask>()
        svgDirectory.list()!!.forEach { svgFile ->
            val shortName = svgFile.removeSuffix(".svg")
            builder[shortName] = SvgToBitmapTask(
                shortName,
                tileSize,
                svgDirectory.resolve("$shortName.svg"),
                HardTaskCache(shortName),
                ctx
            )
        }
        svgTasks = builder.toMap()
    }

    fun <T, TTask : AbstractTask<T>> deduplicate(task: AbstractTask<T>): TTask {
        val other = deduplicateInternal<T, TTask>(task)
        if (other !== task) {
            logger.info("Deduplicated/simplified: {} -> {}", task, other)
        }
        return other
    }

    @Suppress("UNCHECKED_CAST")
    private tailrec fun <T, TTask : AbstractTask<T>> deduplicateInternal(task: AbstractTask<T>): TTask {
        return when {
            task is SvgToBitmapTask
            -> task as TTask
            task is InvalidTask
            -> InvalidTask as TTask
            task is RepaintTask
                    && task.paint == BLACK
                    && !task.base.hasColor()
            -> deduplicateInternal(task.base)
            task is RepaintTask
                    && (task.base is RepaintTask && task.paint == task.base.paint)
            -> deduplicateInternal(if (task.paint.isOpaque) {
                    task.base
                } else if (task.paint is Color) {
                    RepaintTask(task.base, task.base.paint * task.paint.opacity, ::HardTaskCache, ctx)
                } else task)
            task is ImageStackingTask
                    && task.layers.layers.size == 1
                    && task.layers.background == Color.TRANSPARENT
            -> deduplicateInternal(task.layers.layers[0] as TTask)
            else -> {
                logger.debug("Main deduplication branch for {}", task)
                val className = task::class.simpleName ?: "[unnamed class]"
                return taskDeduplicationMap.compute(task) { _, oldValue ->
                    oldValue?.run {
                        if (this === task) {
                            this
                        } else {
                            ImageProcessingStats.dedupeSuccesses.add(className)
                            mergeWithDuplicate(task).also {
                                logger.info("Deduplicated: {} -> {}", this@run.name, it.name)
                            }
                        }
                    } ?: task.also {
                        logger.info("New task: {}", task)
                        ImageProcessingStats.onDedupeFailed(className, task.name)
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
            ImageProcessingStats.dedupeSuccesses.add("SvgToBitmapTask")
        } else {
            ImageProcessingStats.onDedupeFailed("SvgToBitmapTask", name)
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
        if (paint != null) {
            return deduplicate(
                RepaintTask(deduplicate(source), paint * alpha, ::HardTaskCache, ctx)
            )
        }
        if (alpha != 1.0) {
            return deduplicate(
                MakeSemitransparentTask(deduplicate(source), alpha, ::HardTaskCache, ctx))
        }
        return deduplicate(source)
    }

    fun layer(
        source: LayerListBuilder.() -> Unit,
        paint: Paint? = null,
        alpha: Double = 1.0
    ): AbstractImageTask = layer(stack {source()}, paint, alpha)

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
            ctx
        ))
    }

    fun out(vararg names: String, source: AbstractImageTask): PngOutputTask {
        logger.debug("out({}, {})", names.toList(), source)
        val lowercaseNames = names.map { it.lowercase(Locale.ENGLISH) }
        return deduplicate(PngOutputTask(
                lowercaseNames[0],
                deduplicate(source),
                lowercaseNames.map { outTextureRoot.resolve("$it.png") },
                ctx
        )).also { logger.debug("Done creating output task: {}", it) } as PngOutputTask
    }

    fun out(name: String, source: AbstractImageTask): PngOutputTask
            = out(names = arrayOf(name), source = source)

    fun out(vararg names: String, sourceSvgName: String): PngOutputTask
            = out(*names, source = findSvgTask(sourceSvgName))

    fun out(name: String, sourceSvgName: String): PngOutputTask
            = out(names = arrayOf(name), sourceSvgName = sourceSvgName)

    fun out(vararg names: String, source: LayerListBuilder.() -> Unit): PngOutputTask
            = out(*names, source = stack {source()})

    fun out(name: String, source: LayerListBuilder.() -> Unit): PngOutputTask
            = out(names = arrayOf(name), source = source)

    fun stack(layers: LayerList): AbstractImageTask {
        logger.debug("stack({})", layers)
        return deduplicate(
            ImageStackingTask(
                layers, HardTaskCache(layers.toString()), ctx
            )
        )
    }

}
