package io.github.pr0methean.ochd

import io.github.pr0methean.ochd.packedimage.ImagePacker
import io.github.pr0methean.ochd.tasks.*
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.sync.Semaphore
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

fun color(web: String): Color = Color.web(web)

fun color(web: String, alpha: Double): Color = Color.web(web, alpha)

private const val MIN_LIMIT_TO_SKIP_MULTI_SUBTASK_SEMAPHORE = 64

class ImageProcessingContext(
    val name: String,
    val tileSize: Int,
    val scope: CoroutineScope,
    val svgDirectory: File,
    val outTextureRoot: File
) {
    private val outputTasksWithNewSubtasksLimit = 1.shl(24) / (tileSize * tileSize)
    private val needSemaphore = outputTasksWithNewSubtasksLimit < MIN_LIMIT_TO_SKIP_MULTI_SUBTASK_SEMAPHORE
    override fun toString(): String = name
    private val svgTasks: Map<String, SvgImportTask>
    private val taskDeduplicationMap = ConcurrentHashMap<TextureTask, TextureTask>()
    private val newTasksSemaphore = if (needSemaphore) Semaphore(outputTasksWithNewSubtasksLimit) else null
    private val leafImageSize = max(2, tileSize.shr(5))
    val stats = ImageProcessingStats()
    val retryer = Retryer(stats)
    val packer = ImagePacker(scope, retryer, stats, 5, leafImageSize)

    init {
        val builder = mutableMapOf<String, SvgImportTask>()
        svgDirectory.list()!!.forEach { svgFile ->
            val shortName = svgFile.removeSuffix(".svg")
            builder[shortName] = SvgImportTask(
                shortName,
                tileSize,
                svgDirectory.resolve("$shortName.svg"),
                scope,
                retryer,
                stats,
                packer
            )
        }
        svgTasks = builder.toMap()
    }

    fun deduplicate(task: TextureTask): TextureTask {
        if (task is SvgImportTask) {
            return task // SvgImportTask duplication is impossible because svgTasks is populated eagerly
        }
        if (task is RepaintTask && (task.paint == null || task.paint == Color.BLACK) && task.alpha == 1.0) {
            return deduplicate(task.base)
        }
        if (task is ImageStackingTask && task.layers.layers.size == 1 && task.layers.background == Color.TRANSPARENT) {
            return deduplicate(task.layers.layers[0])
        }
        val className = task::class.simpleName ?: "[unnamed class]"
        stats.dedupeSuccesses.add(className)
        return taskDeduplicationMap.computeIfAbsent(task) {
            stats.dedupeSuccesses.remove(className)
            stats.dedupeFailures.add(className)
            task
        }
    }

    fun layer(name: String, paint: Paint? = null, alpha: Double = 1.0): TextureTask
            = layer(svgTasks[name] ?: throw IllegalArgumentException("No SVG task called $name"), paint, alpha)

    fun layer(source: TextureTask, paint: Paint? = null, alpha: Double = 1.0): TextureTask {
       // NB: This means we can't create a black version of a precolored layer except by making it a separate SVG!
        if ((paint == Color.BLACK || paint == null) && alpha == 1.0) {
            return source
        }
        return deduplicate(RepaintTask(paint, source, tileSize, alpha, packer, scope, stats, retryer))
    }

    fun stack(init: LayerListBuilder.() -> Unit): TextureTask {
        val layerTasks = LayerListBuilder(this)
        layerTasks.init()
        return deduplicate(ImageStackingTask(layerTasks.build(), tileSize, packer, scope, stats, retryer))
    }

    fun animate(frames: List<TextureTask>): TextureTask {
        return deduplicate(AnimationColumnTask(frames, tileSize, packer, scope, stats, retryer))
    }

    fun out(name: String, source: TextureTask): OutputTask {
        val lowercaseName = name.lowercase(Locale.ENGLISH)
        return out(lowercaseName, outTextureRoot.resolve("$lowercaseName.png"), source)
    }

    fun out(
        lowercaseName: String,
        destination: File,
        source: TextureTask
    ): OutputTask {
        return OutputTask(source, lowercaseName, destination, newTasksSemaphore, stats, retryer)
    }

    fun out(name: String, source: LayerListBuilder.() -> Unit) = out(name, stack {source()})
}