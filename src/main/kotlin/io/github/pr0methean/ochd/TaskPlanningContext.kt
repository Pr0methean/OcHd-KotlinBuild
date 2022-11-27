package io.github.pr0methean.ochd

import com.github.benmanes.caffeine.cache.Caffeine
import com.google.common.collect.ConcurrentHashMultiset
import io.github.pr0methean.ochd.tasks.AnimationTask
import io.github.pr0methean.ochd.tasks.ImageStackingTask
import io.github.pr0methean.ochd.tasks.ImageTask
import io.github.pr0methean.ochd.tasks.OutputTask
import io.github.pr0methean.ochd.tasks.RepaintTask
import io.github.pr0methean.ochd.tasks.SvgImportTask
import io.github.pr0methean.ochd.tasks.Task
import io.github.pr0methean.ochd.tasks.caching.SemiStrongTaskCache
import io.github.pr0methean.ochd.tasks.caching.TaskCache
import io.github.pr0methean.ochd.tasks.caching.WeakTaskCache
import javafx.scene.image.Image
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.apache.logging.log4j.LogManager
import java.io.File
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

fun color(web: String): Color = Color.web(web)

fun color(web: String, alpha: Double): Color = Color.web(web, alpha)

private val logger = LogManager.getLogger("TaskPlanningContext")
// Main Caffeine cache will be able to contain this * 16 MPx * 4 bytes/Px
private const val MINIMUM_CACHE_4096x4096 = 22L

fun isHugeTileImportTask(name: String): Boolean = name.startsWith("commandBlock") || name.endsWith("4x")

/**
 * Holds info needed to build and deduplicate the task graph. Needs to become unreachable once the graph is built.
 */
class TaskPlanningContext(
    val name: String,
    val tileSize: Int,
    val svgDirectory: File,
    val outTextureRoot: File
) {
    override fun toString(): String = name
    private val svgTasks: Map<String, SvgImportTask>
    private val taskDeduplicationMap = ConcurrentHashMap<ImageTask, ImageTask>()
    private val dedupedSvgTasks = ConcurrentHashMultiset.create<String>()
    private val backingCache = Caffeine.newBuilder()
        .recordStats()
        .weakKeys()
        .executor(Runnable::run) // keep eviction on same thread as population
        .maximumSize(MINIMUM_CACHE_4096x4096.shl(24) / (tileSize * tileSize))
        .build<SemiStrongTaskCache<Image>,Image>()
    internal val hugeTileBackingCache = Caffeine.newBuilder()
        .recordStats()
        .weakKeys()
        .executor(Runnable::run) // keep eviction on same thread as population
        .softValues()
        .build<SemiStrongTaskCache<Image>,Image>()
    val stats: ImageProcessingStats = ImageProcessingStats(backingCache)

    fun createStandardTaskCache(name: String): TaskCache<Image> {
        if (name.contains("4x") || name.contains("commandBlock")) {
            // Tasks using these images are too large for the main cache to manage
            return SemiStrongTaskCache(WeakTaskCache(name), hugeTileBackingCache)
        }
        return SemiStrongTaskCache(WeakTaskCache(name), backingCache)
    }
    private fun createSvgImportCache(name: String): TaskCache<Image> {
        if (isHugeTileImportTask(name)) {
            // These images are too large for the main cache to manage
            return SemiStrongTaskCache(WeakTaskCache(name), hugeTileBackingCache)
        }
        return SemiStrongTaskCache(WeakTaskCache(name), backingCache)
    }

    init {
        val builder = mutableMapOf<String, SvgImportTask>()
        svgDirectory.list()!!.forEach { svgFile ->
            val shortName = svgFile.removeSuffix(".svg")
            builder[shortName] = SvgImportTask(
                shortName,
                tileSize,
                svgDirectory.resolve("$shortName.svg"),
                stats,
                createSvgImportCache(shortName)
            )
        }
        svgTasks = builder.toMap()
    }

    tailrec suspend fun deduplicate(task: Task<Image>): ImageTask {
        if (task is SvgImportTask) {
            // svgTasks is populated eagerly
            val name = task.name
            return findSvgTask(name)
        }
        if (task is RepaintTask
            && (task.paint == null || task.paint == Color.BLACK)
            && task.alpha == 1.0
        ) {
            return deduplicate(task.base)
        }
        if (task is ImageStackingTask
            && task.layers.layers.size == 1
            && task.layers.background == Color.TRANSPARENT) {
            return deduplicate(task.layers.layers[0])
        }
        if (task !is ImageTask) {
            throw RuntimeException("Tried to deduplicate a task that wasn't an ImageTask")
        }
        val className = task::class.simpleName ?: "[unnamed class]"
        val deduped = taskDeduplicationMap.computeIfAbsent(task) {
            logger.info("New task: {}", task)
            stats.dedupeFailures.add(className)
            task
        }
        if (deduped !== task) {
            logger.info("Deduplicated: {}", task)
            stats.dedupeSuccesses.add(className)
            return deduped.mergeWithDuplicate(task)
        }
        return deduped
    }

    fun findSvgTask(name: String): SvgImportTask {
        val task = svgTasks[name] ?: throw RuntimeException("Missing SvgImportTask for $name")
        if (dedupedSvgTasks.add(name, 1) > 0) {
            stats.dedupeSuccesses.add("SvgImportTask")
        } else {
            stats.dedupeFailures.add("SvgImportTask")
        }
        return task
    }

    suspend inline fun layer(name: String, paint: Paint? = null, alpha: Double = 1.0): ImageTask
            = layer(findSvgTask(name), paint, alpha)

    suspend inline fun layer(source: Task<Image>, paint: Paint? = null, alpha: Double = 1.0): ImageTask {
        return deduplicate(RepaintTask(deduplicate(source), paint, alpha, createStandardTaskCache("$source@$paint@$alpha"), stats))
    }

    suspend inline fun stack(init: LayerListBuilder.() -> Unit): ImageTask {
        val layerTasksBuilder = LayerListBuilder(this)
        layerTasksBuilder.init()
        val layerTasks = layerTasksBuilder.build()
        return stack(layerTasks)
    }

    suspend inline fun animate(frames: List<ImageTask>): ImageTask {
        return deduplicate(AnimationTask(frames.asFlow().map(::deduplicate).toList(), tileSize, tileSize, frames.toString(), createStandardTaskCache(frames.toString()), stats))
    }

    suspend inline fun out(source: ImageTask, vararg name: String): OutputTask {
        val lowercaseName = name.map {it.lowercase(Locale.ENGLISH)}
        return out(lowercaseName[0], source, lowercaseName.map{outTextureRoot.resolve("$it.png")})
    }

    suspend inline fun out(
        lowercaseName: String,
        source: ImageTask,
        destination: List<File>
    ): OutputTask {
        val pngSource = deduplicate(source).asPng
        val outputTask = OutputTask(pngSource, lowercaseName, stats, destination)
        pngSource.addDirectDependentTask(outputTask)
        return outputTask
    }

    suspend inline fun out(source: LayerListBuilder.() -> Unit, vararg names: String): OutputTask
            = out(stack {source()}, *names)

    suspend inline fun stack(layers: LayerList): ImageTask
            = deduplicate(ImageStackingTask(layers,
        createStandardTaskCache(layers.toString()), stats))
}