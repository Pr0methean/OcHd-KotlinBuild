package io.github.pr0methean.ochd.packedimage

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.Retryer
import io.github.pr0methean.ochd.SoftAsyncLazy
import io.github.pr0methean.ochd.StrongAsyncLazy
import javafx.scene.image.Image
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
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

    val hashCode = StrongAsyncLazy {
        asPng().contentHashCode()
    }

    init {
        if (initialUnpacked != null && height > MAX_UNCOMPRESSED_TILESIZE) {
            pngBytes.start(scope)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }
        if (other !is BitmapImageNode
            || width != other.width
            || height != other.height) {
            return false
        }
        val unpacked = unpacked.getNow()
        if (unpacked != null && other.unpacked.getNow() == unpacked) {
            return true
        }
        return runBlocking {asPng().contentEquals(other.asPng())}
    }

    override fun hashCode(): Int {
        return runBlocking {hashCode.get()}
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

    override suspend fun mergeWithDuplicate(other: ImageNode) {
        super.mergeWithDuplicate(other)
        hashCode.mergeWithDuplicate((other as BitmapImageNode).hashCode)
    }
}