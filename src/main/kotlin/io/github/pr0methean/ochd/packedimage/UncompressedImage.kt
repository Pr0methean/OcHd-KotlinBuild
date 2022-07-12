package io.github.pr0methean.ochd.packedimage

import io.github.pr0methean.ochd.SoftAsyncLazy
import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class UncompressedImage(private val unpacked: Image, val name: String): PackedImage {
    private val packed = SoftAsyncLazy {
        println("Compressing $name to PNG on demand")
        ByteArrayOutputStream().use {
            ImageIO.write(SwingFXUtils.fromFXImage(unpacked, null), "png", it)
            it.toByteArray()
        }
    }

    override suspend fun unpacked(): Image = unpacked

    override suspend fun packed(): ByteArray = packed.get()
    override fun isAlreadyUnpacked(): Boolean = true
    override fun isAlreadyPacked(): Boolean = packed.isCompleted()
}