package io.github.pr0methean.ochd.packedimage

import io.github.pr0methean.ochd.SoftLazy
import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import javax.imageio.ImageIO

class UncompressedImage(val contents: Image): PackedImage {
    val packed by SoftLazy {
        ByteArrayOutputStream().use {
            ImageIO.write(SwingFXUtils.fromFXImage(contents, null), "png", it)
            it.toByteArray()
        }
    }
    override fun unpack(): Image = contents

    override fun writePng(destination: File) = FileOutputStream(destination).use {it.write(packed)}
}