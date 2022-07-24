package io.github.pr0methean.ochd.packedimage

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.Retryer
import javafx.scene.image.Image
import kotlinx.coroutines.CoroutineScope
import java.util.concurrent.ConcurrentHashMap

const val MAX_UNCOMPRESSED_TILESIZE = 512
const val MIN_SIZE_TO_DEDUP = 8

class ImagePacker(
    val scope: CoroutineScope, private val retryer: Retryer, private val stats: ImageProcessingStats,
    private val maxQuadtreeDepth: Int,
    val leafSize: Int
) {
    private val deduplicationCache = ConcurrentHashMap<ImageNode, ImageNode>()

    @Suppress("UNCHECKED_CAST")
    suspend fun <T : ImageNode> deduplicate(input: T): T {
        if (input.height >= MIN_SIZE_TO_DEDUP) {
            val original = deduplicationCache.putIfAbsent(input, input)?.also {it.mergeWithDuplicate(input)} ?: input
            return original as T
        }
        return input
    }

    /**
     * Encapsulates the given image in a form small enough to fit on the heap.
     */
    suspend fun packImage(input: Image, initialPng: ByteArray?, name: String): ImageNode {
        val basicImage = BitmapImageNode(
            input,
            initialPng,
            name,
            scope,
            retryer,
            stats,
            input.width.toInt(),
            input.height.toInt(),
            this)
        return deduplicate(if (input.height <= MAX_UNCOMPRESSED_TILESIZE) {
            basicImage
        } else {
            basicImage.asSolidOrQuadtreeRecursive(maxQuadtreeDepth, leafSize, leafSize)
        })
    }

    suspend fun quadtreeify(input: ImageNode): QuadtreeImageNode {
        return deduplicate(input.asSolidOrQuadtreeRecursive(maxQuadtreeDepth, leafSize, leafSize).asQuadtree())
    }
}