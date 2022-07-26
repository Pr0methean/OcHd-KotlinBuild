package io.github.pr0methean.ochd.packedimage

import io.github.pr0methean.ochd.ImageProcessingStats
import javafx.scene.image.Image
import kotlinx.coroutines.CoroutineScope

const val MAX_UNCOMPRESSED_TILESIZE = 512

class ImagePacker(
    val scope: CoroutineScope, private val stats: ImageProcessingStats
) {

    /**
     * Encapsulates the given image in a form small enough to fit on the heap.
     */
    fun packImage(input: Image, initialPng: ByteArray?, name: String): PackedImage {
        return BitmapPackedImage(
            input,
            initialPng,
            name,
            scope,
            stats,
            this)
    }
}