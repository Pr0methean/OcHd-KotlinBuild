package io.github.pr0methean.ochd.packedimage

import io.github.pr0methean.ochd.SoftLazy
import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import javax.imageio.ImageIO

class PngImage(input: Image): PackedImage {
    private val unpacked by SoftLazy(input) {
        ByteArrayInputStream(packed).use {Image(it)}
    }
    private val packed: ByteArray = ByteArrayOutputStream().use {
        ImageIO.write(SwingFXUtils.fromFXImage(input, null), "PNG", it)
        return@use it.toByteArray()
    }

    override fun unpack(): Image = unpacked

    override fun writePng(destination: File) = FileOutputStream(destination).use {it.write(packed)}
}