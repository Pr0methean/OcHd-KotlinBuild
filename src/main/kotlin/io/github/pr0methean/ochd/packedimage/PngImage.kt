package io.github.pr0methean.ochd.packedimage

import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import javax.imageio.ImageIO

class PngImage(input: Image): PackedImage {
    val pngBytes = ByteArrayOutputStream().use {
        ImageIO.write(SwingFXUtils.fromFXImage(input, null), "PNG", it)
        return@use it.toByteArray()
    }

    override fun unpack(): Image = ByteArrayInputStream(pngBytes).use {Image(it)}

    override fun writePng(destination: File) = FileOutputStream(destination).use {it.write(pngBytes)}
}