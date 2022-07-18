package io.github.pr0methean.ochd

import io.github.pr0methean.ochd.packedimage.PackedImage
import io.github.pr0methean.ochd.packedimage.PngImage
import io.github.pr0methean.ochd.packedimage.UncompressedImage
import io.github.pr0methean.ochd.tasks.*
import javafx.scene.image.Image
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.yield
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.util.Unbox
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap

fun color(web: String): Color = Color.web(web)

fun color(web: String, alpha: Double): Color = Color.web(web, alpha)

private const val MAX_UNCOMPRESSED_TILESIZE = 512
private const val MAX_UNCOMPRESSED_TILESIZE_COMBINING_TASK = 512
private const val MIN_LIMIT_TO_SKIP_MULTI_SUBTASK_SEMAPHORE = 64
private val logger = LogManager.getLogger("ImageProcessingContext")

class ImageProcessingContext(
    val name: String,
    val tileSize: Int,
    val scope: CoroutineScope,
    val svgDirectory: File,
    val outTextureRoot: File
) {
    private val tasksWithMultipleSubtasksLimit = 1.shl(24) / (tileSize * tileSize)
    val needSemaphore = tasksWithMultipleSubtasksLimit < MIN_LIMIT_TO_SKIP_MULTI_SUBTASK_SEMAPHORE
    override fun toString(): String = name
    val svgTasks: Map<String, SvgImportTask>
    val taskDedupMap = ConcurrentHashMap<TextureTask, TextureTask>()
    val newTasksSemaphore = Semaphore(tasksWithMultipleSubtasksLimit)
    val stats = ImageProcessingStats()

    init {
        val builder = mutableMapOf<String, SvgImportTask>()
        svgDirectory.list()!!.forEach { svgFile ->
            val shortName = svgFile.removeSuffix(".svg")
            builder[shortName] = SvgImportTask(
                shortName,
                tileSize,
                this
            )
        }
        svgTasks = builder.toMap()
    }


    suspend fun <T> retrying(name: String, task: suspend () -> T): T {
        var completed = false
        var result: T? = null
        var failedAttempts = 0
        while (!completed) {
            try {
                result = task()
                completed = true
            } catch (t: CancellationException) {
                throw t
            } catch (t: Throwable) {
                failedAttempts++
                stats.retries.increment()
                logger.error("Yielding before retrying {} ({} failed attempts)", name, Unbox.box(failedAttempts), t)
                yield()
                logger.info("Retrying: {}", name)
            }
        }

        return result!!
    }

    /**
     * Encapsulates the given image in a form small enough to fit on the heap.
     */
    fun packImage(input: Image, task: TextureTask, name: String): PackedImage {
        // Use PNG-compressed images more eagerly in ImageCombiningTask instances, since they're mostly consumed by
        // PNG output tasks.
        val maxUncompressedSize = if (task is AnimationColumnTask || task is ImageStackingTask) {
            MAX_UNCOMPRESSED_TILESIZE_COMBINING_TASK
        } else {
            MAX_UNCOMPRESSED_TILESIZE
        }
        return if (tileSize <= maxUncompressedSize) {
            UncompressedImage(input, name, this)
        } else {
            PngImage(input, name, this)
        }
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
        return taskDedupMap.computeIfAbsent(task) {
            stats.dedupeSuccesses.remove(className)
            stats.dedupeFailures.add(className)
            task
        }
    }

    fun layer(name: String, paint: Paint? = null, alpha: Double = 1.0): TextureTask {
        val importTask = svgTasks[name] ?: throw IllegalArgumentException("No SVG task called $name")
        // NB: This means we can't create a black version of a precolored layer except by making it a separate SVG!
        if ((paint == Color.BLACK || paint == null) && alpha == 1.0) {
            return importTask
        }
        return deduplicate(RepaintTask(paint, importTask, tileSize, alpha, this))
    }

    fun stack(init: LayerListBuilder.() -> Unit): TextureTask {
        val layerTasks = LayerListBuilder(this)
        layerTasks.init()
        return deduplicate(ImageStackingTask(layerTasks.build(), this))
    }

    fun animate(frames: List<TextureTask>): TextureTask {
        return deduplicate(AnimationColumnTask(frames, this))
    }

    fun out(name: String, source: TextureTask) = OutputTask(source, name.lowercase(Locale.ENGLISH), this)

    fun out(name: String, source: LayerListBuilder.() -> Unit) = OutputTask(stack {source()}, name.lowercase(Locale.ENGLISH), this)

    fun onTaskLaunched(task: Any) {
        logger.info("Launched: {}", task)
        stats.taskLaunches.add(task::class.simpleName ?: "[unnamed class]")
    }

    fun onTaskCompleted(task: Any) {
        logger.info("Completed: {}", task)
        stats.taskCompletions.add(task::class.simpleName ?: "[unnamed class]")
    }

    fun onDecompressPngImage(name: String) {
        logger.info("Decompressing {} from PNG", name)
        stats.decompressions.increment()

    }

    fun onCompressPngImage(name: String) {
        logger.info("Compressing {} to PNG", name)
        stats.compressions.increment()
    }
}