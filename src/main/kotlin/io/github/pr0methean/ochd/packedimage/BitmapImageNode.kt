package io.github.pr0methean.ochd.packedimage

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.Retryer
import io.github.pr0methean.ochd.SoftAsyncLazy
import javafx.scene.image.Image
import kotlinx.coroutines.CoroutineScope
import org.apache.logging.log4j.LogManager
import java.io.ByteArrayInputStream

private val logger = LogManager.getLogger("SimpleImageNode")
class BitmapImageNode(
    initialUnpacked: Image?, initialPng: ByteArray?, name: String,
    scope: CoroutineScope, retryer: Retryer, stats: ImageProcessingStats,
    width: Int, height: Int, packer: ImagePacker) : ImageNode(
    width, height, initialUnpacked = initialUnpacked, initialPacked = initialPng,
    name = name, scope = scope, retryer = retryer, stats = stats, packer = packer
) {
    override val pixelReader = SoftAsyncLazy {
        unpacked().pixelReader
    }

    init {
        if (initialUnpacked != null && height > MAX_UNCOMPRESSED_TILESIZE) {
            pngBytes.start(scope)
        }
    }

    override suspend fun unpack(): Image {
        stats.onDecompressPngImage(name)
        return retryer.retrying("Decompression of $name") {
            ByteArrayInputStream(asPng()).use {
                Image(
                    it
                )
            }
        }.also { logger.info("Done decompressing {}", name) }
    }

    override fun shouldDeduplicate(): Boolean = false
}