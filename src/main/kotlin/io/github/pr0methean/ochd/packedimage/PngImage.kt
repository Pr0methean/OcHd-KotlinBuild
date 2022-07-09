package io.github.pr0methean.ochd.packedimage

import io.github.pr0methean.ochd.SoftLazy
import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class PngImage(input: Image): PackedImage {
    override val unpacked by SoftLazy(input) {
        ByteArrayInputStream(packed).use {Image(it)}
    }
    override val packed: ByteArray = ByteArrayOutputStream().use {
        ImageIO.write(SwingFXUtils.fromFXImage(input, null), "PNG", it)
        return@use it.toByteArray()
    }
}