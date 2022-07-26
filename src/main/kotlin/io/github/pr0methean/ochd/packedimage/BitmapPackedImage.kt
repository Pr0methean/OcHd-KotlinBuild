package io.github.pr0methean.ochd.packedimage

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.Retryer
import javafx.scene.image.Image
import kotlinx.coroutines.CoroutineScope
import org.apache.logging.log4j.LogManager
import java.io.ByteArrayInputStream

private val logger = LogManager.getLogger("SimpleImageNode")
class BitmapPackedImage(
    initialUnpacked: Image?, initialPng: ByteArray?, name: String,
    scope: CoroutineScope, retryer: Retryer, stats: ImageProcessingStats,
    packer: ImagePacker) : PackedImage(
    initialUnpacked = initialUnpacked, initialPacked = initialPng,
    name = name, scope = scope, retryer = retryer, stats = stats, packer = packer
) {

    init {
        if (initialUnpacked != null && initialUnpacked.height > MAX_UNCOMPRESSED_TILESIZE) {
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
        }.also {
            logger.info("Done decompressing {}", name)
        }
    }

}