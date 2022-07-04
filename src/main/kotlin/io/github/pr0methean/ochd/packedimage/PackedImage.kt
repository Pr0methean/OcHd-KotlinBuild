package io.github.pr0methean.ochd.packedimage

import javafx.scene.image.Image
import java.io.File

interface PackedImage {
    fun unpack(): Image
    fun writePng(destination: File)
}