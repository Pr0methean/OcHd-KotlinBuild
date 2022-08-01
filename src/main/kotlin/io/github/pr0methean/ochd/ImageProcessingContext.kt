package io.github.pr0methean.ochd

import io.github.pr0methean.ochd.tasks.consumable.*
import io.github.pr0methean.ochd.tasks.consumable.caching.NoopTaskCache
import io.github.pr0methean.ochd.tasks.consumable.caching.SoftTaskCache
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
    private val taskDeduplicationMap = ConcurrentHashMap<ConsumableImageTask, ConsumableImageTask>()
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

    fun deduplicate(task: ConsumableImageTask): ConsumableImageTask {
        if (task is SvgImportTask) {
            return task // SvgImportTask duplication is impossible because svgTasks is populated eagerly
        }
        if (task is RepaintTask
            && (task.paint == null || task.paint == Color.BLACK)
            && task.alpha == 1.0
            && task.base is ConsumableImageTask) {
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
        return if (deduped === task) task
        else DelegatingConsumableImageTask(deduped)
    }

    fun layer(name: String, paint: Paint? = null, alpha: Double = 1.0): ConsumableImageTask
            = layer(svgTasks[name]?.unpacked ?: throw IllegalArgumentException("No SVG task called $name"), paint, alpha)

    fun layer(source: ConsumableTask<Image>, paint: Paint? = null, alpha: Double = 1.0): ConsumableImageTask {
        return deduplicate(RepaintTask(source, paint, alpha, SoftTaskCache(), stats))
    }

    fun stack(init: LayerListBuilder.() -> Unit): ConsumableImageTask {
        val layerTasksBuilder = LayerListBuilder(this)
        layerTasksBuilder.init()
        val layerTasks = layerTasksBuilder.build()
        return deduplicate(ImageStackingTask(layerTasks, tileSize, tileSize, layerTasks.toString(), SoftTaskCache(), stats))
    }

    fun animate(frames: List<ConsumableImageTask>): ConsumableImageTask {
        return deduplicate(AnimationConsumableTask(frames, tileSize, tileSize, frames.toString(), NoopTaskCache(), stats))
    }

    fun out(name: String, source: ConsumableImageTask): OutputConsumableTask {
        val lowercaseName = name.lowercase(Locale.ENGLISH)
        return out(lowercaseName, outTextureRoot.resolve("$lowercaseName.png"), source)
    }

    fun out(
        lowercaseName: String,
        destination: File,
        source: ConsumableImageTask
    ): OutputConsumableTask {
        return OutputConsumableTask(source.asPng, lowercaseName, destination, stats)
    }

    fun out(name: String, source: LayerListBuilder.() -> Unit) = out(name, stack {source()})
}