package io.github.pr0methean.ochd.packedimage

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.SoftAsyncLazy
import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import kotlinx.coroutines.async
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class PngImage(input: Image, val ctx: ImageProcessingContext, val name: String): PackedImage {
    private val unpacked = SoftAsyncLazy(input) {
        println("Decompressing from PNG: $name")
        ctx.retrying("Decompression of $name") {ByteArrayInputStream(packed()).use { Image(it) }}
    }
    override suspend fun unpacked() = unpacked.get()
    private val packingTask = ctx.scope.async {
        ByteArrayOutputStream().use {
            println("Compressing to PNG: $name")
            ImageIO.write(SwingFXUtils.fromFXImage(input, null), "PNG", it)
            println("Done compressing to PNG: $name")
            return@async it.toByteArray()
        }
    }

    override suspend fun packed(): ByteArray = packingTask.await()
}