package io.github.pr0methean.ochd.tasks.consumable

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.tasks.consumable.caching.SoftTaskCache
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
import java.io.File

private val batikTranscoder: ThreadLocal<ToImageTranscoder> = ThreadLocal.withInitial { ToImageTranscoder() }
/** SVG decoder that stores the last image it decoded, rather than passing it to an encoder. */
private class ToImageTranscoder: PNGTranscoder() {
    val mutex = Mutex()
    @Volatile
    private var lastImage: BufferedImage? = null
    override fun writeImage(img: BufferedImage?, output: TranscoderOutput?) {
        lastImage = img
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
    override val stats: ImageProcessingStats
): AbstractConsumableImageTask(name, SoftTaskCache(), stats) {

    override suspend fun perform(): Image {
        val result = withContext(currentCoroutineContext().plus(batikTranscoder.asContextElement())) {
            try {
                stats.onTaskLaunched("SvgImportTask", name)
                val transcoder = batikTranscoder.get()
                val output = TranscoderOutput()
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
                return@withContext image
            } catch (t: Throwable) {
                throw t
            }
        }
        stats.onTaskCompleted("SvgImportTask", name)
        return result
    }
}