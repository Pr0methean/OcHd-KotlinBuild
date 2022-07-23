package io.github.pr0methean.ochd.packedimage

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.Retryer
import io.github.pr0methean.ochd.SoftAsyncLazy
import javafx.scene.image.Image
import javafx.scene.image.PixelReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import org.apache.logging.log4j.LogManager
import java.io.ByteArrayInputStream

private val logger = LogManager.getLogger("SimpleImageNode")
class SimpleImageNode(initialUncompressed: Image?, initialPng: ByteArray?, name: String,
                      scope: CoroutineScope, retryer: Retryer, stats: ImageProcessingStats,
                      width: Int, height: Int
) : ImageNode(width, height, initialPng, name, scope, retryer, stats,
        start = if (initialUncompressed != null) CoroutineStart.DEFAULT else CoroutineStart.LAZY) {
    override suspend fun pixelReader(): PixelReader {
        return unpacked().pixelReader
    }

    init {
        if (initialUncompressed != null && height > MAX_UNCOMPRESSED_TILESIZE) {
            packingTask.start()
        }
    }

    private val unpacked = SoftAsyncLazy(initialUncompressed) {
        stats.onDecompressPngImage(name)
        return@SoftAsyncLazy retryer.retrying("Decompression of $name") {
            ByteArrayInputStream(asPng()).use {
                Image(
                    it
                )
            }
        }.also { logger.info("Done decompressing {}", name) }
    }

    override suspend fun unpacked() = unpacked.get()
    override fun toString(): String = "Simple"
}