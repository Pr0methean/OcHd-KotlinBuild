package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.Retryer
import io.github.pr0methean.ochd.packedimage.PackedImage
import io.github.pr0methean.ochd.packedimage.PngImage
import javafx.embed.swing.SwingFXUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.apache.batik.transcoder.SVGAbstractTranscoder.KEY_HEIGHT
import org.apache.batik.transcoder.SVGAbstractTranscoder.KEY_WIDTH
import org.apache.batik.transcoder.TranscoderInput
import org.apache.batik.transcoder.TranscoderOutput
import org.apache.batik.transcoder.image.PNGTranscoder
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.lang.StringBuilder
import java.util.concurrent.atomic.AtomicReference

// svgSalamander doesn't seem to be thread-safe even when loaded in a ThreadLocal<ClassLoader>
private val batikTranscoder: ThreadLocal<ImageRetainingTranscoder> = ThreadLocal.withInitial {ImageRetainingTranscoder()}

/** SVG-to-PNG transcoder that retains the last image it wrote, until it's retrieved by calling takeLastImage(). */
private class ImageRetainingTranscoder: PNGTranscoder() {
    val mutex = Mutex()
    private val lastImage = AtomicReference<BufferedImage?>()
    override fun writeImage(img: BufferedImage?, output: TranscoderOutput?) {
        lastImage.set(img)
        super.writeImage(img, output)
    }
    fun takeLastImage(): BufferedImage? = lastImage.getAndSet(null)
}

data class SvgImportTask(
    val shortName: String,
    private val tileSize: Int,
    val file: File,
    val scope: CoroutineScope,
    val retryer: Retryer,
    val stats: ImageProcessingStats
): TextureTask {

    private val coroutine: Deferred<PackedImage> by lazy {
        scope.plus(batikTranscoder.asContextElement()).async {
            retryer.retrying(shortName) {
                stats.onTaskLaunched(this@SvgImportTask)
                val transcoder = batikTranscoder.get()
                transcoder.setTranscodingHints(
                    mapOf(
                        KEY_WIDTH to tileSize.toFloat(),
                        KEY_HEIGHT to tileSize.toFloat()
                    )
                )
                ByteArrayOutputStream().use { outStream ->
                    val output = TranscoderOutput(outStream)
                    FileInputStream(file).use {
                        val input = TranscoderInput(file.toURI().toString())
                        val image = transcoder.mutex.withLock {
                            transcoder.transcode(input, output)
                            transcoder.takeLastImage()!!
                        }
                        return@retrying PngImage(
                            initialPacked = outStream.toByteArray(),
                            initialUnpacked = SwingFXUtils.toFXImage(image, null),
                            name = shortName, scope = scope, retryer = retryer, stats = stats
                        )
                    }
                }
            }
        }.also { stats.onTaskCompleted(this@SvgImportTask) }
    }
    override fun isComplete(): Boolean = coroutine.isCompleted
    override fun isStarted(): Boolean = coroutine.isActive || coroutine.isCompleted

    override suspend fun getImage(): PackedImage = coroutine.await()
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getImageNow(): PackedImage? = if (coroutine.isCompleted) coroutine.getCompleted() else null

    override fun toString(): String = shortName
    override fun formatTo(buffer: StringBuilder) {
        buffer.append(shortName)
    }
}