package io.github.pr0methean.ochd.packedimage

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.SoftAsyncLazy
import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import java.io.ByteArrayOutputStream
import java.util.logging.Logger
import javax.imageio.ImageIO

private val logger = Logger.getLogger("UncompressedImage")
class UncompressedImage(private val unpacked: Image, val name: String, val ctx: ImageProcessingContext): PackedImage {
    private val packed = SoftAsyncLazy {
        ctx.onCompressPngImage(name)
        ByteArrayOutputStream().use {
            ImageIO.write(SwingFXUtils.fromFXImage(unpacked, null), "png", it)
            logger.info("Done compressing to PNG: $name")
            it.toByteArray()
        }
    }

    override suspend fun unpacked(): Image = unpacked

    override suspend fun packed(): ByteArray = packed.get()
    override fun isAlreadyUnpacked(): Boolean = true
    override fun isAlreadyPacked(): Boolean = packed.isCompleted()
}