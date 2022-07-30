package io.github.pr0methean.ochd.tasks.consumable

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.tasks.consumable.caching.SoftTaskCache
import io.github.pr0methean.ochd.tasks.consumable.caching.StrongTaskCache
import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.apache.batik.transcoder.SVGAbstractTranscoder
import org.apache.batik.transcoder.TranscoderInput
import org.apache.batik.transcoder.TranscoderOutput
import org.apache.batik.transcoder.image.PNGTranscoder
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success

private val batikTranscoder: ThreadLocal<ImageRetainingTranscoder> = ThreadLocal.withInitial { ImageRetainingTranscoder() }
/** SVG-to-PNG transcoder that retains the last image it wrote, until it's retrieved by calling takeLastImage(). */
private class ImageRetainingTranscoder: PNGTranscoder() {
    val mutex = Mutex()
    @Volatile
    private var lastImage: BufferedImage? = null
    override fun writeImage(img: BufferedImage?, output: TranscoderOutput?) {
        lastImage = img
        super.writeImage(img, output)
    }
    fun takeLastImage(): BufferedImage? {
        val lastImage = this.lastImage
        this.lastImage = null
        return lastImage
    }
}

data class SvgImportTask(
    override val name: String,
    private val tileSize: Int,
    val file: File,
    val stats: ImageProcessingStats
): SimpleConsumableTask<ByteArray>(name, StrongTaskCache()), ConsumableImageTask {

    override val asPng = this

    override val unpacked = TransformingConsumableTask(
        base = this,
        cache = SoftTaskCache(),
        transform = { bytes -> ByteArrayInputStream(bytes).use {Image(it)} }
    )

    override suspend fun perform(): ByteArray {
        val result = withContext(currentCoroutineContext().plus(batikTranscoder.asContextElement())) {
            try {
                stats.onTaskLaunched("SvgImportTask", name)
                val transcoder = batikTranscoder.get()
                ByteArrayOutputStream().use { outStream ->
                    val output = TranscoderOutput(outStream)
                    val input = TranscoderInput(file.toURI().toString())
                    val image = SwingFXUtils.toFXImage(transcoder.mutex.withLock {
                        transcoder.setTranscodingHints(
                            mapOf(
                                SVGAbstractTranscoder.KEY_WIDTH to tileSize.toFloat(),
                                SVGAbstractTranscoder.KEY_HEIGHT to tileSize.toFloat()
                            )
                        )
                        transcoder.transcode(input, output)
                        transcoder.takeLastImage()!!
                    }, null)
                    unpacked.emit(success(image))
                    return@withContext outStream.toByteArray()
                }
            } catch (t: Throwable) {
                unpacked.emit(failure(t))
                throw t
            }
        }
        stats.onTaskCompleted("SvgImportTask", name)
        return result
    }
}