package io.github.pr0methean.ochd.packedimage

import io.github.pr0methean.ochd.SoftAsyncLazy
import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class PngImage(input: Image): PackedImage {
    private val unpacked = SoftAsyncLazy(input) {
        println("Decompressing a PNG image")
        ByteArrayInputStream(packed).use { Image(it) }
    }
    override suspend fun unpacked() = unpacked.get()
    private val packed = ByteArrayOutputStream().use {
        ImageIO.write(SwingFXUtils.fromFXImage(input, null), "PNG", it)
        return@use it.toByteArray()
    }

    override suspend fun packed(): ByteArray = packed
}