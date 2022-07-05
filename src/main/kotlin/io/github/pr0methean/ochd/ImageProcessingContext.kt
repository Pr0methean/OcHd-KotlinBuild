package io.github.pr0methean.ochd

import com.google.common.collect.ConcurrentHashMultiset
import com.kitfox.svg.SVGUniverse
import io.github.pr0methean.ochd.packedimage.PackedImage
import io.github.pr0methean.ochd.packedimage.PngImage
import io.github.pr0methean.ochd.packedimage.UncompressedImage
import io.github.pr0methean.ochd.tasks.*
import javafx.scene.image.Image
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import kotlinx.coroutines.CoroutineScope
import java.io.File
import java.util.concurrent.ConcurrentHashMap

fun color(web: String) = Color.web(web)

fun color(web: String, alpha: Double) = Color.web(web, alpha)

private const val MAX_UNCOMPRESSED_TILESIZE = 1024

class ImageProcessingContext(
    val name: String,
    val tileSize: Int,
    val scope: CoroutineScope,
    val svgDirectory: File,
    val outTextureRoot: File
) {
    override fun toString(): String = name

    val svg = SVGUniverse()
    val svgTasks = ConcurrentHashMap<String, SvgImportTask>()
    val taskDedupMap = ConcurrentHashMap<TextureTask<*>, TextureTask<*>>()
    val taskLaunches: ConcurrentHashMultiset<String> = ConcurrentHashMultiset.create()
    val dedupeSuccesses: ConcurrentHashMultiset<String> = ConcurrentHashMultiset.create()
    val dedupeFailures: ConcurrentHashMultiset<String> = ConcurrentHashMultiset.create()
    init {
        svgDirectory.list()!!.forEach { svgFile ->
            val shortName = svgFile.removeSuffix(".svg")
            svgTasks[shortName] = SvgImportTask(
                shortName,
                svg,
                tileSize,
                this
            )
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
    fun packImage(input: Image): PackedImage =
            if (tileSize <= MAX_UNCOMPRESSED_TILESIZE) UncompressedImage(input) else PngImage(input)

    fun deduplicate(task: TextureTask<*>): TextureTask<*> {
        val className = task::class.simpleName
        dedupeSuccesses.add(className)
        return taskDedupMap.computeIfAbsent(task) {
            dedupeSuccesses.remove(className)
            dedupeFailures.add(className)
            task
        }
    }

    fun layer(name: String, paint: Paint? = null, alpha: Double = 1.0): TextureTask<*> {
        val importTask = svgTasks[name] ?: throw IllegalArgumentException("No SVG task called $name")
        return if (paint == null && alpha == 1.0) importTask
                else deduplicate(RepaintTask(paint, importTask, tileSize, alpha, this))
    }

    fun stack(init: LayerListBuilder.() -> Unit): TextureTask<*> {
        val layerTasks = LayerListBuilder(this)
        layerTasks.init()
        return deduplicate(
            if (layerTasks.layers.size == 1 && layerTasks.background == Color.TRANSPARENT)
                layerTasks.layers[0]
            else
                ImageStackingTask(layerTasks.build(), tileSize, this))
    }

    fun animate(init: LayerListBuilder.() -> Unit): TextureTask<*> {
        val frames = LayerListBuilder(this)
        frames.init()
        return deduplicate(AnimationColumnTask(frames.build(), tileSize, this))
    }

    fun out(name: String, source: TextureTask<*>)
            = BasicOutputTask(source, name, this)

    fun out(name: String, source: LayerListBuilder.() -> Unit) = BasicOutputTask(
            stack {source()}, name, this)

    fun onTaskGraphFinished() {
        svgTasks.clear()
        taskDedupMap.clear()
    }
}