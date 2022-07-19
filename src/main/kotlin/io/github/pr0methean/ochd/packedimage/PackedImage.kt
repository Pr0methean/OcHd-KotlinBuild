package io.github.pr0methean.ochd.packedimage

import javafx.scene.image.Image
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

interface PackedImage {
    suspend fun unpacked(): Image
    suspend fun packed(): ByteArray

    suspend fun writePng(destination: File) = withContext(Dispatchers.IO) {
        destination.parentFile.mkdirs()
        FileOutputStream(destination).use { it.write(packed()) }
    }
}