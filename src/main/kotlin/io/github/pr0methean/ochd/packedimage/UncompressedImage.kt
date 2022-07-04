package io.github.pr0methean.ochd.packedimage

import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import java.io.File
import javax.imageio.ImageIO

class UncompressedImage(val contents: Image): PackedImage {
    override fun unpack(): Image = contents

    override fun writePng(destination: File) {
        ImageIO.write(SwingFXUtils.fromFXImage(contents, null), "png", destination)
    }
}