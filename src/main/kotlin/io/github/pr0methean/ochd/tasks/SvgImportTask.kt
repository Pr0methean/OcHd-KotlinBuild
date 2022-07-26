package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.MEMORY_INTENSE_COROUTINE_CONTEXT
import io.github.pr0methean.ochd.Retryer
import io.github.pr0methean.ochd.packedimage.ImagePacker
import io.github.pr0methean.ochd.packedimage.PackedImage
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

// svgSalamander doesn't seem to be thread-safe even when loaded in a ThreadLocal<ClassLoader>
private val batikTranscoder: ThreadLocal<ImageRetainingTranscoder> = ThreadLocal.withInitial {ImageRetainingTranscoder()}

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
    val scope: CoroutineScope,
    val retryer: Retryer,
    val stats: ImageProcessingStats,
    val packer: ImagePacker
): TextureTask {

    private val coroutine: Deferred<PackedImage> by lazy {
        scope.plus(batikTranscoder.asContextElement())
                .async(CoroutineName("SvgImportTask for $name"), start = CoroutineStart.LAZY) {
            stats.onTaskLaunched("SvgImportTask", name)
            val result = retryer.retrying(name) {
                val transcoder = batikTranscoder.get()
                ByteArrayOutputStream().use { outStream ->
                    val output = TranscoderOutput(outStream)
                    val input = TranscoderInput(file.toURI().toString())
                    withContext(MEMORY_INTENSE_COROUTINE_CONTEXT) {
                        val image = transcoder.mutex.withLock {
                            transcoder.setTranscodingHints(
                                mapOf(
                                    KEY_WIDTH to tileSize.toFloat(),
                                    KEY_HEIGHT to tileSize.toFloat()
                                )
                            )
                            transcoder.transcode(input, output)
                            transcoder.takeLastImage()!!
                        }
                        return@withContext packer.packImage(
                            SwingFXUtils.toFXImage(image, null),
                            outStream.toByteArray(), name
                        )
                    }
                }
            }
            stats.onTaskCompleted("SvgImportTask", name)
            result
        }
    }
    override fun isComplete(): Boolean = coroutine.isCompleted
    override fun isStarted(): Boolean = coroutine.isActive || coroutine.isCompleted
    override fun dependencies(): Collection<Task> = listOf()

    override suspend fun getImage(): PackedImage = coroutine.await()
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getImageNow(): PackedImage? = if (coroutine.isCompleted) coroutine.getCompleted() else null

    override fun toString(): String = name
    override fun formatTo(buffer: StringBuilder) {
        buffer.append(name)
    }
}