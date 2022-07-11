package io.github.pr0methean.ochd

import com.google.common.collect.ConcurrentHashMultiset
import io.github.pr0methean.ochd.packedimage.PackedImage
import io.github.pr0methean.ochd.packedimage.PngImage
import io.github.pr0methean.ochd.packedimage.UncompressedImage
import io.github.pr0methean.ochd.tasks.*
import javafx.scene.image.Image
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import kotlinx.coroutines.*
import java.io.File
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ThreadLocalRandom
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

fun color(web: String) = Color.web(web)

fun color(web: String, alpha: Double) = Color.web(web, alpha)

private const val MAX_UNCOMPRESSED_TILESIZE = 512
private const val MAX_UNCOMPRESSED_TILESIZE_COMBINING_TASK = 512
private val REPORTING_INTERVAL: Duration = 1.minutes
private fun getRetryDelay() = 10.seconds.plus(ThreadLocalRandom.current().nextInt(10_000).milliseconds)
class ImageProcessingContext(
    val name: String,
    val tileSize: Int,
    val scope: CoroutineScope,
    val svgDirectory: File,
    val outTextureRoot: File
) {
    override fun toString(): String = name
    val svgTasks: Map<String, SvgImportTask>
    val taskDedupMap = ConcurrentHashMap<TextureTask, TextureTask>()
    val taskLaunches: ConcurrentHashMultiset<String> = ConcurrentHashMultiset.create()
    val taskCompletions: ConcurrentHashMultiset<String> = ConcurrentHashMultiset.create()
    val dedupeSuccesses: ConcurrentHashMultiset<String> = ConcurrentHashMultiset.create()
    val dedupeFailures: ConcurrentHashMultiset<String> = ConcurrentHashMultiset.create()
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
        while (!completed) {
            try {
                result = task()
                completed = true
            } catch (t: CancellationException) {
                throw t
            } catch (t: Throwable) {
                println("Yielding before retrying: caught $t in $name")
                yield()
                println("Retrying: $name")
            }
        }
        return result!!
    }

    @Suppress("DeferredResultUnused")
    fun startMonitoringStats() {
        scope.async {
            while(true) {
                delay(REPORTING_INTERVAL)
                println()
                println("[${Instant.now()}] Task completions:")
                taskCompletions.toSet().forEach {println("$it: ${taskCompletions.count(it)}")}
                println()
            }
        }
    }

    fun printStats() {
        println()
        println("Task launches:")
        taskLaunches.toSet().forEach {println("$it: ${taskLaunches.count(it)}")}
        println()
        println("Deduplication successes:")
        dedupeSuccesses.toSet().forEach {println("$it: ${dedupeSuccesses.count(it)}")}
        println()
        println("Deduplication failures:")
        dedupeFailures.toSet().forEach {println("$it: ${dedupeFailures.count(it)}")}
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
            UncompressedImage(input, name)
        } else {
            PngImage(input, this, name)
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
        dedupeSuccesses.add(className)
        return taskDedupMap.computeIfAbsent(task) {
            dedupeSuccesses.remove(className)
            dedupeFailures.add(className)
            task
        }
    }

    fun layer(name: String, paint: Paint? = null, alpha: Double = 1.0): TextureTask {
        val importTask = (svgTasks ?: throw IllegalStateException("SVG tasks are already cleared"))[name]
                ?: throw IllegalArgumentException("No SVG task called $name")
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

    fun out(name: String, source: TextureTask) = BasicOutputTask(source, name, this)

    fun out(name: String, source: LayerListBuilder.() -> Unit) = BasicOutputTask(stack {source()}, name, this)

    fun onTaskLaunched(task: Any) {
        println("Launched: $task")
        taskLaunches.add(task::class.simpleName ?: "[unnamed class]")
    }

    fun onTaskCompleted(task: Any) {
        println("Completed: $task")
        taskCompletions.add(task::class.simpleName ?: "[unnamed class]")
    }

}