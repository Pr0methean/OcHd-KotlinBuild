package io.github.pr0methean.ochd.packedimage

import io.github.pr0methean.ochd.SoftLazy
import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class UncompressedImage(override val unpacked: Image): PackedImage {
    override val packed: ByteArray by SoftLazy {
        ByteArrayOutputStream().use {
            ImageIO.write(SwingFXUtils.fromFXImage(unpacked, null), "png", it)
            it.toByteArray()
        }
    }
}