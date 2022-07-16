package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.packedimage.PackedImage
import io.github.pr0methean.ochd.packedimage.PngImage
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.async
import kotlinx.coroutines.plus
import org.apache.batik.transcoder.SVGAbstractTranscoder.KEY_HEIGHT
import org.apache.batik.transcoder.SVGAbstractTranscoder.KEY_WIDTH
import org.apache.batik.transcoder.TranscoderInput
import org.apache.batik.transcoder.TranscoderOutput
import org.apache.batik.transcoder.image.PNGTranscoder
import java.io.ByteArrayOutputStream
import java.io.FileInputStream

// svgSalamander doesn't seem to be thread-safe even when loaded in a ThreadLocal<ClassLoader>
val batikTranscoder = ThreadLocal.withInitial {PNGTranscoder()}

data class SvgImportTask(
    val shortName: String,
    private val tileSize: Int,
    val ctx: ImageProcessingContext
): TextureTask {
    val coroutine: Deferred<PackedImage> by lazy {
        ctx.scope.plus(batikTranscoder.asContextElement()).async { ctx.retrying(shortName) {
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
                    return@retrying PngImage(outStream.toByteArray(), ctx, shortName)
                }
            }
        }}
    }

    val file = ctx.svgDirectory.resolve("$shortName.svg")
    override fun isComplete(): Boolean = coroutine.isCompleted

    override fun willExpandHeap(): Boolean = !isComplete()

    override suspend fun getImage(): PackedImage = coroutine.await()

    override fun toString(): String = shortName
}