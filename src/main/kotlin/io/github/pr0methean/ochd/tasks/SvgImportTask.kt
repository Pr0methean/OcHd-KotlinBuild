package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.tasks.caching.TaskCache
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
    private val width: Int,
    private val file: File,
    override val stats: ImageProcessingStats,
    taskCache: TaskCache<Image>
): AbstractImageTask(name, taskCache, stats) {
    override suspend fun createCoroutineScope(): CoroutineScope {
        return super.createCoroutineScope().plus(batikTranscoder.asContextElement())
    }

    override fun registerDirectDependencies() {
        // No-op: SvgImportTask doesn't depend on any other task
    }

    override fun equals(other: Any?): Boolean {
        return (other === this) || other is SvgImportTask && other.file == file
    }

    override fun hashCode(): Int {
        return file.hashCode()
    }

    override suspend fun perform(): Image {
        stats.onTaskLaunched("SvgImportTask", name)
        val transcoder = batikTranscoder.get()
        val input = TranscoderInput(file.toURI().toString())
        doJfx("Reserve memory for import of $name") {
            awaitFreeMemory(16 * width.toLong() * width, name) // Height may be up to width*4
        }
        val image = SwingFXUtils.toFXImage(transcoder.mutex.withLock {
            transcoder.setTranscodingHints(mapOf(SVGAbstractTranscoder.KEY_WIDTH to width.toFloat()))
            transcoder.transcode(input, null)
            transcoder.takeLastImage()!!
        }, null)
        stats.onTaskCompleted("SvgImportTask", name)
        return image
    }
}