package io.github.pr0methean.ochd

import com.github.benmanes.caffeine.cache.Caffeine
import com.google.common.collect.ConcurrentHashMultiset
import io.github.pr0methean.ochd.tasks.AnimationTask
import io.github.pr0methean.ochd.tasks.FileOutputTask
import io.github.pr0methean.ochd.tasks.ImageStackingTask
import io.github.pr0methean.ochd.tasks.ImageTask
import io.github.pr0methean.ochd.tasks.RepaintTask
import io.github.pr0methean.ochd.tasks.SvgToBitmapTask
import io.github.pr0methean.ochd.tasks.Task
import io.github.pr0methean.ochd.tasks.caching.CaffeineDeferredTaskCache
import io.github.pr0methean.ochd.tasks.caching.DeferredTaskCache
import io.github.pr0methean.ochd.tasks.caching.VictimReferenceDeferredTaskCache
import javafx.scene.image.Image
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.apache.logging.log4j.LogManager
import java.io.File
import java.lang.ref.SoftReference
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext

fun color(web: String): Color = Color.web(web)

fun color(web: String, alpha: Double): Color = Color.web(web, alpha)

private val logger = LogManager.getLogger("TaskPlanningContext")
private const val MAIN_CACHE_SIZE_BYTES = 1L.shl(31)
private const val HUGE_TILE_CACHE_SIZE_BYTES = 1L.shl(30)

fun isHugeTileTask(name: String): Boolean = name.contains("commandBlock") || name.contains("4x")

private const val REGULAR_TILES_PER_HUGE_TILE = 4

private const val BYTES_PER_PIXEL = 4

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
    private fun bytesPerTile() = BYTES_PER_PIXEL * tileSize * tileSize
    override fun toString(): String = name
    private val svgTasks: Map<String, SvgToBitmapTask>
    private val taskDeduplicationMap = ConcurrentHashMap<Task<*>, Task<*>>()
    private val dedupedSvgTasks = ConcurrentHashMultiset.create<String>()
    private val backingCache = Caffeine.newBuilder()
        .recordStats()
        .weakKeys()
        .executor(Runnable::run) // keep eviction on same thread as population
        .maximumSize(MAIN_CACHE_SIZE_BYTES / bytesPerTile())
        .build<DeferredTaskCache<Image>, Deferred<Image>>()
    internal val hugeTileBackingCache = Caffeine.newBuilder()
        .recordStats()
        .weakKeys()
        .executor(Runnable::run) // keep eviction on same thread as population
        .maximumSize(HUGE_TILE_CACHE_SIZE_BYTES / (REGULAR_TILES_PER_HUGE_TILE * bytesPerTile()))
        .build<DeferredTaskCache<Image>, Deferred<Image>>()
    val stats: ImageProcessingStats = ImageProcessingStats(backingCache)

    fun createTaskCache(name: String): DeferredTaskCache<Image> {
        return VictimReferenceDeferredTaskCache(
                CaffeineDeferredTaskCache(if (isHugeTileTask(name)) hugeTileBackingCache else backingCache),
                ::SoftReference)
    }

    init {
        val builder = mutableMapOf<String, SvgToBitmapTask>()
        svgDirectory.list()!!.forEach { svgFile ->
            val shortName = svgFile.removeSuffix(".svg")
            builder[shortName] = SvgToBitmapTask(
                shortName,
                tileSize,
                svgDirectory.resolve("$shortName.svg"),
                createTaskCache(shortName),
                ctx,
                stats
            )
        }
        svgTasks = builder.toMap()
    }

    @Suppress("UNCHECKED_CAST")
    tailrec suspend fun <T, TTask : Task<T>> deduplicate(task: Task<T>): TTask = when {
        task is SvgToBitmapTask -> {
            // svgTasks is populated eagerly
            val name = task.name
            findSvgTask(name) as TTask
        }
        task is RepaintTask
                && (task.paint == null || task.paint == Color.BLACK)
                && task.alpha == 1.0
        -> {
            deduplicate(task.base)
        }
        task is ImageStackingTask
                && task.layers.layers.size == 1
                && task.layers.background == Color.TRANSPARENT -> {
            deduplicate(task.layers.layers[0] as TTask)
        }
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

    suspend inline fun layer(name: String, paint: Paint? = null, alpha: Double = 1.0): ImageTask
            = layer(findSvgTask(name), paint, alpha)

    suspend inline fun layer(source: Task<Image>, paint: Paint? = null, alpha: Double = 1.0): ImageTask {
        return deduplicate(
            RepaintTask(
                deduplicate(source) as ImageTask,
                paint,
                alpha,
                createTaskCache("$source@$paint@$alpha"),
                ctx,
                stats
            )) as ImageTask
    }

    suspend inline fun stack(init: LayerListBuilder.() -> Unit): ImageTask {
        val layerTasksBuilder = LayerListBuilder(this)
        layerTasksBuilder.init()
        val layerTasks = layerTasksBuilder.build()
        return stack(layerTasks)
    }

    suspend inline fun animate(background: ImageTask, frames: List<ImageTask>): ImageTask {
        return deduplicate(
            AnimationTask(
                deduplicate(background) as ImageTask,
                frames.asFlow().map { deduplicate(it) as ImageTask }.toList(),
                tileSize,
                tileSize,
                frames.toString(),
                createTaskCache(frames.toString()),
                ctx,
                stats
            )) as ImageTask
    }

    suspend inline fun out(source: ImageTask, names: Array<String>): FileOutputTask {
        val lowercaseName = names.map { it.lowercase(Locale.ENGLISH) }
        val pngSource = deduplicate((deduplicate(source) as ImageTask).asPng)
        val orig =
            FileOutputTask(
                lowercaseName[0],
                pngSource,
                lowercaseName.map { outTextureRoot.resolve("$it.png") },
                ctx,
                stats
            )
        val deduped = deduplicate(orig) as FileOutputTask
        if (deduped === orig) {
            pngSource.addDirectDependentTask(deduped)
        }
        return deduped
    }

    suspend inline fun out(source: ImageTask, name: String): FileOutputTask = out(source, arrayOf(name))

    suspend inline fun out(source: LayerListBuilder.() -> Unit, name: String): FileOutputTask
            = out(stack {source()}, arrayOf(name))

    suspend inline fun stack(layers: LayerList): ImageTask
            = deduplicate(
        ImageStackingTask(
            layers,
            createTaskCache(layers.toString()),
            ctx,
            stats
        )) as ImageTask
}
