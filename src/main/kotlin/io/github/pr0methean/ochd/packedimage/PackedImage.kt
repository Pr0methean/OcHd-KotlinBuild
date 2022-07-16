package io.github.pr0methean.ochd.packedimage

import io.github.pr0methean.ochd.SoftAsyncLazy
import javafx.scene.image.Image
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

interface PackedImage {
    val packed: SoftAsyncLazy<ByteArray>
    suspend fun unpacked(): Image

    suspend fun writePng(destination: File) = withContext(Dispatchers.IO) {
        FileOutputStream(destination).use { it.write(packed()) }
    }

    fun isAlreadyUnpacked(): Boolean
    fun isAlreadyPacked(): Boolean
    suspend fun packed(): ByteArray = packed.get()
}