package io.github.pr0methean.ochd.packedimage

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.Retryer
import io.github.pr0methean.ochd.SoftAsyncLazy
import javafx.scene.image.Image
import kotlinx.coroutines.CoroutineScope
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.util.Unbox.box
import java.io.ByteArrayInputStream

const val MIN_LOGGABLE_SIZE = 8
private val logger = LogManager.getLogger("SimpleImageNode")
class BitmapImageNode(
    initialUnpacked: Image?, initialPng: ByteArray?, name: String,
    scope: CoroutineScope, retryer: Retryer, stats: ImageProcessingStats,
    width: Int, height: Int, packer: ImagePacker) : ImageNode(
    width, height, initialUnpacked = initialUnpacked, initialPacked = initialPng,
    name = name, scope = scope, retryer = retryer, stats = stats, packer = packer
) {
    override val pixelReader = SoftAsyncLazy(unpacked.getNow()?.pixelReader) {
        unpacked().pixelReader
    }

    override suspend fun unpack(): Image {
        if (height >= MIN_LOGGABLE_SIZE) {
            stats.onDecompressPngImage("a ${width}×$height chunk of $name")
        }
        return retryer.retrying("Decompression of a ${width}×$height chunk of  $name") {
            ByteArrayInputStream(asPng()).use {
                Image(
                    it
                )
            }
        }.also { if (height >= MIN_LOGGABLE_SIZE) {
            logger.info("Done decompressing a {}×{} chunk of {}", box(width), box(height), name)
        } }
    }

    override fun shouldDeduplicate(): Boolean = false
}