package io.github.pr0methean.ochd

import io.github.pr0methean.ochd.tasks.consumable.*
import io.github.pr0methean.ochd.tasks.consumable.caching.SoftTaskCache
import io.github.pr0methean.ochd.tasks.consumable.caching.noopTaskCache
import javafx.scene.image.Image
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap

fun color(web: String): Color = Color.web(web)

fun color(web: String, alpha: Double): Color = Color.web(web, alpha)

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

    suspend fun deduplicate(task: ImageTask): ImageTask {
        if (task is SvgImportTask) {
            stats.dedupeSuccesses.add("SvgImportTask")
            return task // SvgImportTask duplication is impossible because svgTasks is populated eagerly
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
        return deduplicate(RepaintTask(source, paint, alpha, SoftTaskCache(), stats))
    }

    suspend fun stack(init: suspend LayerListBuilder.() -> Unit): ImageTask {
        val layerTasksBuilder = LayerListBuilder(this)
        layerTasksBuilder.init()
        val layerTasks = layerTasksBuilder.build()
        return deduplicate(ImageStackingTask(layerTasks, tileSize, tileSize, layerTasks.toString(), SoftTaskCache(), stats))
    }

    suspend fun animate(frames: List<ImageTask>): ImageTask {
        return deduplicate(AnimationTask(frames, tileSize, tileSize, frames.toString(), noopTaskCache(), stats))
    }

    fun out(name: String, source: ImageTask): OutputTask {
        val lowercaseName = name.lowercase(Locale.ENGLISH)
        return out(lowercaseName, outTextureRoot.resolve("$lowercaseName.png"), source)
    }

    fun out(
        lowercaseName: String,
        destination: File,
        source: ImageTask
    ): OutputTask {
        return OutputTask(source.asPng, lowercaseName, destination, stats)
    }

    suspend fun out(name: String, source: suspend LayerListBuilder.() -> Unit) = out(name, stack {source()})
}