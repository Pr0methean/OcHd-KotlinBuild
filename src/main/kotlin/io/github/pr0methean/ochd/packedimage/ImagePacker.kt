package io.github.pr0methean.ochd.packedimage

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.Retryer
import javafx.scene.image.Image
import kotlinx.coroutines.CoroutineScope
import java.util.concurrent.ConcurrentHashMap

const val MAX_UNCOMPRESSED_TILESIZE = 512
class ImagePacker(
    val scope: CoroutineScope, private val retryer: Retryer, private val stats: ImageProcessingStats,
    val maxQuadtreeDepth: Int,
    val leafSize: Int
) {
    val deduplicationMap = ConcurrentHashMap<ImageNode, ImageNode>()

    suspend fun deduplicate(input: ImageNode): ImageNode = if (input.shouldDeduplicate()) {
        val original = deduplicationMap.putIfAbsent(input, input)!!
        if (original !== input) {
            original.mergeWithDuplicate(input)
        }
        original
    } else {
        input
    }

    /**
     * Encapsulates the given image in a form small enough to fit on the heap.
     */
    suspend fun packImage(input: Image, initialPng: ByteArray?, name: String): ImageNode {
        val basicImage = SimpleImageNode(input, initialPng, name, scope, retryer, stats, input.width.toInt(), input.height.toInt())
        return deduplicate(if (input.height <= MAX_UNCOMPRESSED_TILESIZE) {
            basicImage
        } else {
            basicImage.asSolidOrQuadtreeRecursive(maxQuadtreeDepth, leafSize, leafSize)
        })
    }
}