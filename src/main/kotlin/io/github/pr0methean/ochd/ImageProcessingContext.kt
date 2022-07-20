package io.github.pr0methean.ochd

import io.github.pr0methean.ochd.packedimage.ImagePacker
import io.github.pr0methean.ochd.packedimage.PackedImage
import io.github.pr0methean.ochd.tasks.*
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import org.apache.logging.log4j.LogManager
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap

fun color(web: String): Color = Color.web(web)

fun color(web: String, alpha: Double): Color = Color.web(web, alpha)

private val logger = LogManager.getLogger("ImageProcCtx")
@Suppress("DeferredIsResult")
class ImageProcessingContext(
    val name: String,
    val tileSize: Int,
    val scope: CoroutineScope,
    val svgDirectory: File,
    val outTextureRoot: File
) {
    override fun toString(): String = name
    private val svgTasks: Map<String, Deferred<PackedImage>>
    private val taskDeduplicationMap = ConcurrentHashMap<TextureTask, Deferred<PackedImage>>()
    val stats = ImageProcessingStats()
    val retryer = Retryer(stats)
    val packer = ImagePacker(scope, retryer, stats)

    init {
        val builder = mutableMapOf<String, Deferred<PackedImage>>()
        svgDirectory.list()!!.forEach { svgFile ->
            val shortName = svgFile.removeSuffix(".svg")
            builder[shortName] = SvgImportTask(
                shortName,
                tileSize,
                svgDirectory.resolve("$shortName.svg"),
                scope,
                retryer,
                stats
            ).launchAsync()
            logger.info("{} is the SvgImportTask for {}", builder[shortName], shortName)
        }
        svgTasks = builder.toMap()
    }

    fun dedup(task: TextureTask): Deferred<PackedImage> {
        if (task is SvgImportTask) {
            // SvgImportTask duplication is impossible because svgTasks is populated eagerly
            return assertGetSvgTaskNamed(task.name)
        }
        if (task is RepaintTask && (task.paint == null || task.paint == Color.BLACK) && task.alpha == 1.0) {
            return task.base
        }
        if (task is ImageStackingTask && task.layers.layers.size == 1 && task.layers.background == Color.TRANSPARENT) {
            return task.layers.layers[0]
        }
        val className = task::class.simpleName ?: "[unnamed class]"
        stats.dedupeSuccesses.add(className)
        return taskDeduplicationMap.computeIfAbsent(task) {
            stats.dedupeSuccesses.remove(className)
            stats.dedupeFailures.add(className)
            val job = task.launchAsync()
            logger.info("$job is $task")
            return@computeIfAbsent job
        }
    }

    fun layer(name: String, paint: Paint? = null, alpha: Double = 1.0): Deferred<PackedImage> = dedup(RepaintTask(
            paint = paint,
            base = assertGetSvgTaskNamed(name),
            alpha = alpha,
            size = tileSize,
            packer = packer,
            scope = scope,
            stats = stats,
            retryer = retryer))

    private fun assertGetSvgTaskNamed(name: String) =
        svgTasks[name] ?: throw IllegalArgumentException("No SVG file named $name")

    fun layer(source: TextureTask, paint: Paint? = null, alpha: Double = 1.0): Deferred<PackedImage> {
        return layer(dedup(source), paint, alpha)
    }

    fun layer(source: Deferred<PackedImage>, paint: Paint? = null, alpha: Double = 1.0): Deferred<PackedImage> {
       // NB: This means we can't create a black version of a precolored layer except by making it a separate SVG!
        if ((paint == Color.BLACK || paint == null) && alpha == 1.0) {
            return source
        }
        return dedup(RepaintTask(
            paint = paint,
            base = source,
            alpha = alpha,
            size = tileSize,
            packer = packer,
            scope = scope,
            stats = stats,
            retryer = retryer))
    }

    fun stack(init: LayerListBuilder.() -> Unit): Deferred<PackedImage> {
        val layerTasks = LayerListBuilder(this)
        layerTasks.init()
        val layers = layerTasks.build()
        return stack(layers)

    }

    private fun stack(layers: LayerList): Deferred<PackedImage> {
        return dedup(ImageStackingTask(layers, tileSize, packer, scope, stats, retryer))
    }

    fun out(name: String, source: Deferred<PackedImage>): OutputTask {
        val lowercaseName = name.lowercase(Locale.ENGLISH)
        return OutputTask(source, lowercaseName, outTextureRoot.resolve("$lowercaseName.png"), stats, retryer)
    }

    fun out(name: String, source: TextureTask): OutputTask = out(name, dedup(source))

    fun out(name: String, source: LayerListBuilder.() -> Unit) = out(name, stack {source()})
    fun stack(layers: List<Deferred<PackedImage>>): Deferred<PackedImage> {
        return stack(LayerList(layers, Color.TRANSPARENT))
    }

    fun animate(frames: List<Deferred<PackedImage>>): Deferred<PackedImage> {
        return dedup(AnimationColumnTask(frames, tileSize, packer, scope, stats, retryer))
    }
}