package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.MEMORY_INTENSE_COROUTINE_CONTEXT
import io.github.pr0methean.ochd.StrongAsyncLazy
import io.github.pr0methean.ochd.packedimage.ImagePacker
import io.github.pr0methean.ochd.packedimage.PackedImage
import javafx.embed.swing.SwingFXUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.apache.batik.transcoder.SVGAbstractTranscoder.KEY_HEIGHT
import org.apache.batik.transcoder.SVGAbstractTranscoder.KEY_WIDTH
import org.apache.batik.transcoder.TranscoderInput
import org.apache.batik.transcoder.TranscoderOutput
import org.apache.batik.transcoder.image.PNGTranscoder
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success

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
    val stats: ImageProcessingStats,
    val packer: ImagePacker
): TextureTask {

    private val result = StrongAsyncLazy<Result<PackedImage>> {
        withContext(scope.coroutineContext.plus(batikTranscoder.asContextElement())) {
            stats.onTaskLaunched("SvgImportTask", name)
            val transcoder = batikTranscoder.get()
            return@withContext try {
                val packed = ByteArrayOutputStream().use { outStream ->
                    val output = TranscoderOutput(outStream)
                    val input = TranscoderInput(file.toURI().toString())
                    return@use withContext(MEMORY_INTENSE_COROUTINE_CONTEXT) {
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
                        packer.packImage(
                            SwingFXUtils.toFXImage(image, null),
                            outStream.toByteArray(), name
                        )
                    }
                }
                stats.onTaskCompleted("SvgImportTask", name)
                success(packed)
            } catch (t: Throwable) {
                failure(t)
            }
        }
    }
    override fun isComplete(): Boolean = result.getNow() != null
    override fun isStarted(): Boolean = result.isStarted()
    override fun isFailed(): Boolean = result.getNow()?.isFailure == true

    override suspend fun join(): Result<PackedImage> = result.get()

    override suspend fun getImage(): PackedImage = result.get().getOrThrow()
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getImageNow(): PackedImage? = result.getNow()?.getOrThrow()

    override fun toString(): String = name
    override fun formatTo(buffer: StringBuilder) {
        buffer.append(name)
    }
}