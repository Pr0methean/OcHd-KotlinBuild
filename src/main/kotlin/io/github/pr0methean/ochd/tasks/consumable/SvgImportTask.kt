package io.github.pr0methean.ochd.tasks.consumable

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.tasks.consumable.caching.noopTaskCache
import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.plus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.apache.batik.transcoder.SVGAbstractTranscoder
import org.apache.batik.transcoder.TranscoderInput
import org.apache.batik.transcoder.TranscoderOutput
import org.apache.batik.transcoder.image.ImageTranscoder
import java.awt.image.BufferedImage
import java.io.File

private val batikTranscoder: ThreadLocal<ToImageTranscoder> = ThreadLocal.withInitial { ToImageTranscoder() }
/** SVG decoder that stores the last image it decoded, rather than passing it to an encoder. */
private class ToImageTranscoder: ImageTranscoder() {
    val mutex = Mutex()
    @Volatile
    private var lastImage: BufferedImage? = null
    override fun createImage(width: Int, height: Int): BufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)

    override fun writeImage(img: BufferedImage?, output: TranscoderOutput?) {
        lastImage = img
    }
    fun takeLastImage(): BufferedImage? {
        val lastImage = this.lastImage
        this.lastImage = null
        return lastImage
    }
}

class SvgImportTask(
    override val name: String,
    private val tileSize: Int,
    private val file: File,
    override val stats: ImageProcessingStats
): AbstractImageTask(name, noopTaskCache(), stats) {
    override suspend fun createCoroutineScope(attempt: Long): CoroutineScope {
        return super.createCoroutineScope(attempt).plus(batikTranscoder.asContextElement())
    }

    override suspend fun perform(): Image {
        stats.onTaskLaunched("SvgImportTask", name)
        val transcoder = batikTranscoder.get()
        val input = TranscoderInput(file.toURI().toString())
        val image = SwingFXUtils.toFXImage(transcoder.mutex.withLock {
            transcoder.setTranscodingHints(
                mapOf(
                    SVGAbstractTranscoder.KEY_WIDTH to tileSize.toFloat(),
                    SVGAbstractTranscoder.KEY_HEIGHT to tileSize.toFloat()
                )
            )
            transcoder.transcode(input, null)
            transcoder.takeLastImage()!!
        }, null)
        stats.onTaskCompleted("SvgImportTask", name)
        return image
    }
}