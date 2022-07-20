package io.github.pr0methean.ochd.packedimage

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.Retryer
import io.github.pr0methean.ochd.tasks.AnimationColumnTask
import io.github.pr0methean.ochd.tasks.ImageStackingTask
import io.github.pr0methean.ochd.tasks.TextureTask
import javafx.scene.image.Image
import kotlinx.coroutines.CoroutineScope

private const val MAX_UNCOMPRESSED_TILESIZE = 512
private const val MAX_UNCOMPRESSED_TILESIZE_COMBINING_TASK = 512
class ImagePacker(val scope: CoroutineScope, private val retryer: Retryer, private val stats: ImageProcessingStats) {
    /**
     * Encapsulates the given image in a form small enough to fit on the heap.
     */
    fun packImage(input: Image, initialPng: ByteArray?, task: TextureTask, name: String): PackedImage {
        // Use PNG-compressed images more eagerly in ImageCombiningTask instances, since they're mostly consumed by
        // PNG output tasks.
        val maxUncompressedSize = if (task is AnimationColumnTask || task is ImageStackingTask) {
            MAX_UNCOMPRESSED_TILESIZE_COMBINING_TASK
        } else {
            MAX_UNCOMPRESSED_TILESIZE
        }
        return if (input.width <= maxUncompressedSize) {
            UncompressedImage(input, name, stats)
        } else {
            PngImage(input, initialPng, name, scope, retryer, stats)
        }
    }
}