package io.github.pr0methean.ochd

import io.github.pr0methean.ochd.tasks.consumable.*
import io.github.pr0methean.ochd.tasks.consumable.caching.SoftTaskCache
import io.github.pr0methean.ochd.tasks.consumable.caching.WeakTaskCache
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
class ImageProcessingContext(
    val name: String,
    val tileSize: Int,
    val svgDirectory: File,
    val outTextureRoot: File
) {
    override fun toString(): String = name
    private val svgTasks: Map<String, SvgImportTask>
    private val taskDeduplicationMap = ConcurrentHashMap<ImageTask, ImageTask>()
    val stats = ImageProcessingStats()
    private val dedupedSvgTasks = ConcurrentHashMap.newKeySet<String>()

    init {
        val builder = mutableMapOf<String, SvgImportTask>()
        svgDirectory.list()!!.forEach { svgFile ->
            val shortName = svgFile.removeSuffix(".svg")
            builder[shortName] = SvgImportTask(
                shortName,
                tileSize,
                svgDirectory.resolve("$shortName.svg"),
                stats)
        }
        svgTasks = builder.toMap()
    }

    suspend fun deduplicate(task: Task<Image>): ImageTask {
        if (task is SvgImportTask) {
            // svgTasks is populated eagerly
            val match = svgTasks[task.name] ?: throw RuntimeException("Missing SvgImportTask for $name")
            if (match === task) {
                if (dedupedSvgTasks.add(task.name)) {
                    stats.dedupeFailures.add("SvgImportTask")
                }
            } else {
                stats.dedupeSuccesses.add("SvgImportTask")
            }
            return match
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
            return object: AbstractImageTask(task.name, SoftTaskCache(), stats) {
                override suspend fun perform(): Image = task.await().getOrThrow()
            }
        }
        val className = task::class.simpleName ?: "[unnamed class]"
        stats.dedupeSuccesses.add(className)
        val deduped = taskDeduplicationMap.computeIfAbsent(task) {
            stats.dedupeSuccesses.remove(className)
            stats.dedupeFailures.add(className)
            task
        }
        return deduped.mergeWithDuplicate(task)
    }

    suspend fun layer(name: String, paint: Paint? = null, alpha: Double = 1.0): ImageTask
            = layer(svgTasks[name]?.unpacked ?: throw IllegalArgumentException("No SVG task called $name"), paint, alpha)

    suspend fun layer(source: Task<Image>, paint: Paint? = null, alpha: Double = 1.0): ImageTask {
        return deduplicate(RepaintTask(deduplicate(source), paint, alpha, SoftTaskCache(), stats))
    }

    suspend fun stack(init: suspend LayerListBuilder.() -> Unit): ImageTask {
        val layerTasksBuilder = LayerListBuilder(this)
        layerTasksBuilder.init()
        val layerTasks = layerTasksBuilder.build()
        return deduplicate(ImageStackingTask(layerTasks, tileSize, tileSize, layerTasks.toString(), WeakTaskCache(), stats))
    }

    suspend fun animate(frames: List<ImageTask>): ImageTask {
        return deduplicate(AnimationTask(frames.asFlow().map(::deduplicate).toList(), tileSize, tileSize, frames.toString(), noopTaskCache(), stats))
    }

    suspend fun out(name: String, source: ImageTask): OutputTask {
        val lowercaseName = name.lowercase(Locale.ENGLISH)
        return out(lowercaseName, outTextureRoot.resolve("$lowercaseName.png"), source)
    }

    suspend fun out(
        lowercaseName: String,
        destination: File,
        source: ImageTask
    ): OutputTask {
        return OutputTask(deduplicate(source).asPng, lowercaseName, destination, stats)
    }

    suspend fun out(name: String, source: suspend LayerListBuilder.() -> Unit) = out(name, stack {source()})
}