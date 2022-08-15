package io.github.pr0methean.ochd

import com.github.benmanes.caffeine.cache.Caffeine
import com.google.common.collect.ConcurrentHashMultiset
import io.github.pr0methean.ochd.tasks.consumable.*
import io.github.pr0methean.ochd.tasks.consumable.caching.SemiStrongTaskCache
import io.github.pr0methean.ochd.tasks.consumable.caching.noopTaskCache
import javafx.scene.image.Image
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.apache.logging.log4j.LogManager
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap

fun color(web: String): Color = Color.web(web)

fun color(web: String, alpha: Double): Color = Color.web(web, alpha)

private val logger = LogManager.getLogger("ImageProcessingContext")
// Hard-ref cache will be able to contain MINIMUM_IMAGE_CACHE_4096x4096 * 64MiB of uncompressed 32-bit pixels
private const val MINIMUM_IMAGE_CACHE_4096x4096 = 24L
class ImageProcessingContext(
    val name: String,
    val tileSize: Int,
    val svgDirectory: File,
    val outTextureRoot: File
) {
    override fun toString(): String = name
    private val svgTasks: Map<String, SvgImportTask>
    private val taskDeduplicationMap = ConcurrentHashMap<ImageTask, ImageTask>()
    val stats: ImageProcessingStats = ImageProcessingStats()
    private val dedupedSvgTasks = ConcurrentHashMultiset.create<String>()

    private val backingCache = Caffeine.newBuilder().weakKeys().maximumSize(MINIMUM_IMAGE_CACHE_4096x4096.shl(24) / (tileSize * tileSize))
        .build<SemiStrongTaskCache<*>,Result<*>>()

    private fun <T> createSemiStrongTaskCache() = SemiStrongTaskCache<T>(backingCache)

    init {
        val builder = mutableMapOf<String, SvgImportTask>()
        svgDirectory.list()!!.forEach { svgFile ->
            val shortName = svgFile.removeSuffix(".svg")
            builder[shortName] = SvgImportTask(
                shortName,
                tileSize,
                svgDirectory.resolve("$shortName.svg"),
                stats,
                createSemiStrongTaskCache()
            )
        }
        svgTasks = builder.toMap()
    }

    suspend fun deduplicate(task: Task<Image>): ImageTask {
        if (task is SvgImportTask) {
            // svgTasks is populated eagerly
            val name = task.name
            return findSvgTask(name)
        }
        if (task is RepaintTask
            && (task.paint == null || task.paint == Color.BLACK)
            && task.alpha == 1.0
            && task.base is ImageTask) {
            return deduplicate(task.base)
        }
        if (task is ImageStackingTask
            && task.layers.layers.size == 1
            && task.layers.background == Color.TRANSPARENT) {
            return deduplicate(task.layers.layers[0])
        }
        if (task !is ImageTask) {
            logger.warn("Tried to deduplicate a task that's not an ImageTask")
            stats.dedupeFailures.add(task::class.simpleName ?: "[unnamed non-ImageTask class]")
            return object: AbstractImageTask(task.name, createSemiStrongTaskCache(), stats) {
                override suspend fun perform(): Image = task.await().getOrThrow()
            }
        }
        val className = task::class.simpleName ?: "[unnamed class]"
        val deduped = taskDeduplicationMap.computeIfAbsent(task) {
            logger.info("Failed to deduplicate: {}", task)
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

    private fun findSvgTask(name: String): SvgImportTask {
        val task = svgTasks[name] ?: throw RuntimeException("Missing SvgImportTask for $name")
        if (dedupedSvgTasks.add(name, 1) > 0) {
            task.enableCaching()
            stats.dedupeSuccesses.add("SvgImportTask")
        } else {
            stats.dedupeFailures.add("SvgImportTask")
        }
        return task
    }

    suspend fun layer(name: String, paint: Paint? = null, alpha: Double = 1.0): ImageTask
            = layer(findSvgTask(name), paint, alpha)

    suspend fun layer(source: Task<Image>, paint: Paint? = null, alpha: Double = 1.0): ImageTask {
        return deduplicate(RepaintTask(deduplicate(source), paint, alpha, createSemiStrongTaskCache(), stats))
    }

    suspend fun stack(init: suspend LayerListBuilder.() -> Unit): ImageTask {
        val layerTasksBuilder = LayerListBuilder(this)
        layerTasksBuilder.init()
        val layerTasks = layerTasksBuilder.build()
        return deduplicate(ImageStackingTask(layerTasks, tileSize, tileSize, layerTasks.toString(), createSemiStrongTaskCache(), stats))
    }

    suspend fun animate(frames: List<ImageTask>): ImageTask {
        return deduplicate(AnimationTask(frames.asFlow().map(::deduplicate).toList(), tileSize, tileSize, frames.toString(), noopTaskCache(), stats))
    }

    suspend fun out(source: ImageTask, vararg name: String): OutputTask {
        val lowercaseName = name.map {it.lowercase(Locale.ENGLISH)}
        return out(lowercaseName[0], source, lowercaseName.map{outTextureRoot.resolve("$it.png")})
    }

    suspend fun out(
        lowercaseName: String,
        source: ImageTask,
        vararg destinations: File
    ): OutputTask {
        return OutputTask(deduplicate(source).asPng, lowercaseName, stats, destinations.asList())
    }

    suspend fun out(
        lowercaseName: String,
        source: ImageTask,
        destination: List<File>
    ): OutputTask {
        return OutputTask(deduplicate(source).asPng, lowercaseName, stats, destination)
    }

    suspend fun out(source: suspend LayerListBuilder.() -> Unit, vararg names: String): OutputTask
            = out(stack {source()}, *names)

    suspend fun stack(layers: LayerList): ImageTask = deduplicate(ImageStackingTask(layers, tileSize, tileSize, layers.toString(), createSemiStrongTaskCache(), stats))
}