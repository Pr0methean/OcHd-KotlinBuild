package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.packedimage.PackedImage
import io.github.pr0methean.ochd.packedimage.PngImage
import javafx.embed.swing.SwingFXUtils
import kotlinx.coroutines.*
import org.apache.batik.transcoder.SVGAbstractTranscoder.KEY_HEIGHT
import org.apache.batik.transcoder.SVGAbstractTranscoder.KEY_WIDTH
import org.apache.batik.transcoder.TranscoderInput
import org.apache.batik.transcoder.TranscoderOutput
import org.apache.batik.transcoder.image.PNGTranscoder
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.lang.StringBuilder

// svgSalamander doesn't seem to be thread-safe even when loaded in a ThreadLocal<ClassLoader>
private val batikTranscoder: ThreadLocal<ImageRetainingTranscoder> = ThreadLocal.withInitial {ImageRetainingTranscoder()}

/** SVG-to-PNG transcoder that retains the last image it wrote, until it's retrieved by calling takeLastImage(). */
private class ImageRetainingTranscoder: PNGTranscoder() {
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
    val shortName: String,
    private val tileSize: Int,
    val ctx: ImageProcessingContext
): TextureTask {
    @Volatile
    var isAllocated: Boolean = false

    val coroutine: Deferred<PackedImage> by lazy {
        ctx.scope.plus(batikTranscoder.asContextElement()).async { ctx.retrying(shortName) {
            ctx.onTaskLaunched(this@SvgImportTask)
            val transcoder = batikTranscoder.get()
            transcoder.setTranscodingHints(
                mapOf(
                    KEY_WIDTH to ctx.tileSize.toFloat(),
                    KEY_HEIGHT to ctx.tileSize.toFloat()
                )
            )
            ByteArrayOutputStream().use { outStream ->
                val output = TranscoderOutput(outStream)
                FileInputStream(file).use {
                    val input = TranscoderInput(file.toURI().toString())
                    transcoder.transcode(input, output)
                    isAllocated = true
                    return@retrying PngImage(pngInput = outStream.toByteArray(),
                            initialUnpacked = SwingFXUtils.toFXImage(transcoder.takeLastImage()!!, null),
                            ctx = ctx, name = shortName)
                }
            }
        }}.also { ctx.onTaskCompleted(this@SvgImportTask) }
    }

    val file = ctx.svgDirectory.resolve("$shortName.svg")
    override fun isComplete(): Boolean = coroutine.isCompleted

    override fun willExpandHeap(): Boolean = isAllocated

    override suspend fun getImage(): PackedImage = coroutine.await()
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getImageNow(): PackedImage? = if (coroutine.isCompleted) coroutine.getCompleted() else null

    override fun toString(): String = shortName
    override fun formatTo(buffer: StringBuilder) {
        buffer.append(shortName)
    }
}