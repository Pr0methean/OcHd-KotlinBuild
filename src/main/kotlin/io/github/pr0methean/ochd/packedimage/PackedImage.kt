package io.github.pr0methean.ochd.packedimage

import javafx.scene.image.Image
import java.io.File
import java.io.FileOutputStream

interface PackedImage {
    suspend fun unpacked(): Image
    suspend fun packed(): ByteArray

    suspend fun writePng(destination: File) = FileOutputStream(destination).use { it.write(packed()) }
}