package io.github.pr0methean.ochd.packedimage

import javafx.scene.image.Image
import java.io.File
import java.io.FileOutputStream

interface PackedImage {
    val unpacked: Image
    val packed: ByteArray
    fun writePng(destination: File) = FileOutputStream(destination).use {it.write(packed)}
}