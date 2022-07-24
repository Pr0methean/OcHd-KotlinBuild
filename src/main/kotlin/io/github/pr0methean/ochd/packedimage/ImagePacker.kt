package io.github.pr0methean.ochd.packedimage

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.Retryer
import javafx.scene.image.Image
import kotlinx.coroutines.CoroutineScope

const val MAX_UNCOMPRESSED_TILESIZE = 512
const val MIN_SIZE_TO_DEDUP = 64
private const val DEDUP_CACHE_SIZE = 5_000L

class ImagePacker(
    val scope: CoroutineScope, private val retryer: Retryer, private val stats: ImageProcessingStats,
    private val maxQuadtreeDepth: Int,
    val leafSize: Int
) {
    private val deduplicationMap: LoadingCache<ImageNode, ImageNode> = Caffeine.newBuilder().maximumSize(DEDUP_CACHE_SIZE).build<ImageNode, ImageNode> { it }

    suspend fun deduplicate(input: ImageNode): ImageNode {
        if (input.shouldDeduplicate()
               && input.height >= MIN_SIZE_TO_DEDUP) {
            val original = deduplicationMap.get(input)
            if (original !== input) {
                original.mergeWithDuplicate(input)
                return original
            }
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
}