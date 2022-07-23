package io.github.pr0methean.ochd.packedimage

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.Retryer
import javafx.scene.image.Image
import javafx.scene.image.PixelReader
import kotlinx.coroutines.CoroutineScope
import org.apache.logging.log4j.LogManager
import java.io.ByteArrayInputStream

private val logger = LogManager.getLogger("SimpleImageNode")
class SimpleImageNode(initialUnpacked: Image?, initialPng: ByteArray?, name: String,
                      scope: CoroutineScope, retryer: Retryer, stats: ImageProcessingStats,
                      width: Int, height: Int
) : ImageNode(
    width, height, initialPacked = initialPng, initialUnpacked = initialUnpacked,
    name = name, scope = scope, retryer = retryer, stats = stats) {
    override suspend fun pixelReader(): PixelReader {
        return unpacked().pixelReader
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