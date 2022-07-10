package io.github.pr0methean.ochd

import com.google.common.collect.ConcurrentHashMultiset
import io.github.pr0methean.ochd.packedimage.PackedImage
import io.github.pr0methean.ochd.packedimage.PngImage
import io.github.pr0methean.ochd.packedimage.UncompressedImage
import io.github.pr0methean.ochd.tasks.*
import javafx.scene.image.Image
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ThreadLocalRandom
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

fun color(web: String) = Color.web(web)

fun color(web: String, alpha: Double) = Color.web(web, alpha)

private const val MAX_UNCOMPRESSED_TILESIZE = 4096
private const val MAX_UNCOMPRESSED_TILESIZE_COMBINING_TASK = 1024
private fun getOomeDelay() = 10.seconds.plus(ThreadLocalRandom.current().nextInt(10_000).milliseconds)
private val MAX_RETRIES = 25
private val AVG_RETRIES_PER_GC = 5

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
    val dedupeSuccesses: ConcurrentHashMultiset<String> = ConcurrentHashMultiset.create()
    val dedupeFailures: ConcurrentHashMultiset<String> = ConcurrentHashMultiset.create()
    suspend fun shouldRetry(cause: Throwable, attempt: Long): Boolean {
        if (attempt > MAX_RETRIES) {
            return false
        }
        val delay = getOomeDelay()
        println("Retrying a task due to $cause. This is attempt $attempt")
        withContext(Dispatchers.IO) {
            delay(delay)
            if (ThreadLocalRandom.current().nextInt(AVG_RETRIES_PER_GC) == 0) {
                System.gc()
            }
        }
        return true
    }
    fun <T> decorateFlow(flow: Flow<T>): Flow<T> = flow.retryWhen {cause, attempt -> shouldRetry(cause, attempt)}
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
    fun packImage(input: Image, task: TextureTask): PackedImage {
        if (task is AnimationColumnTask || task is ImageStackingTask) {
            // Use PNG-compressed images more eagerly in ImageCombiningTask instances, since they're mostly consumed by
            // PNG output tasks.
            return if (tileSize <= MAX_UNCOMPRESSED_TILESIZE_COMBINING_TASK) UncompressedImage(input) else PngImage(input)
        }
        return if (tileSize <= MAX_UNCOMPRESSED_TILESIZE) UncompressedImage(input) else PngImage(input)
    }

    fun deduplicate(task: TextureTask): TextureTask {
        if (task is SvgImportTask) {
            return task // SvgImportTask duplication is impossible because svgTasks is populated eagerly
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
        val importTask = svgTasks[name] ?: throw IllegalArgumentException("No SVG task called $name")
        return if (paint == null && alpha == 1.0) importTask
                else deduplicate(RepaintTask(paint, importTask, tileSize, alpha, this))
    }

    fun stack(init: LayerListBuilder.() -> Unit): TextureTask {
        val layerTasks = LayerListBuilder(this)
        layerTasks.init()
        return deduplicate(
            if (layerTasks.layers.size == 1 && layerTasks.background == Color.TRANSPARENT)
                layerTasks.layers[0]
            else
                ImageStackingTask(layerTasks.build(), this))
    }

    fun animate(init: LayerListBuilder.() -> Unit): TextureTask {
        val frames = LayerListBuilder(this)
        frames.init()
        return deduplicate(AnimationColumnTask(frames.build(), this))
    }

    fun out(name: String, source: TextureTask) = BasicOutputTask(source, name, this)

    fun out(name: String, source: LayerListBuilder.() -> Unit) = BasicOutputTask(stack {source()}, name, this)

    fun onTaskGraphFinished() {
        taskDedupMap.clear()
    }
}