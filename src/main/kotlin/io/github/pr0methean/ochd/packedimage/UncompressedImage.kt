package io.github.pr0methean.ochd.packedimage

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.SoftAsyncLazy
import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import org.apache.logging.log4j.LogManager
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

private val logger = LogManager.getLogger("UncompressedImage")
class UncompressedImage(private val unpacked: Image, val name: String, val stats: ImageProcessingStats): PackedImage {
    private val packed = SoftAsyncLazy {
        stats.onCompressPngImage(name)
        ByteArrayOutputStream().use {
            ImageIO.write(SwingFXUtils.fromFXImage(unpacked, null), "png", it)
            it.toByteArray()
        }
    }.also {logger.info("Done compressing to PNG: {}", name)}

    override suspend fun unpacked(): Image = unpacked

    override suspend fun packed(): ByteArray = packed.get()
    override fun isAlreadyUnpacked(): Boolean = true
    override fun isAlreadyPacked(): Boolean = packed.isCompleted()
}