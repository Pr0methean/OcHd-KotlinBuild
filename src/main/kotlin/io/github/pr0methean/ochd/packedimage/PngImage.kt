package io.github.pr0methean.ochd.packedimage

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.Retryer
import io.github.pr0methean.ochd.SoftAsyncLazy
import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import org.apache.logging.log4j.LogManager
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

private val logger = LogManager.getLogger("PngImage")
class PngImage(initialUnpacked: Image?, initialPacked: ByteArray?, name: String,
               scope: CoroutineScope, private val retryer: Retryer, private val stats: ImageProcessingStats) : PackedImage {

    private val unpacked = SoftAsyncLazy(initialUnpacked) {
        stats.onDecompressPngImage(name)
        return@SoftAsyncLazy retryer.retrying("Decompression of $name") {
            ByteArrayInputStream(packed()).use {
                Image(
                    it
                )
            }
        }.also { logger.info("Done decompressing {}", name) }
    }

    private val packingTask = if (initialPacked == null) {
        scope.async {
            ByteArrayOutputStream().use {
                retryer.retrying<ByteArray>("Compression of $name") {
                    stats.onCompressPngImage(name)
                    @Suppress("BlockingMethodInNonBlockingContext")
                    ImageIO.write(SwingFXUtils.fromFXImage(initialUnpacked!!, null), "PNG", it)
                    return@retrying it.toByteArray()
                }
            }.also {logger.info("Done compressing {}", name)}
        }
    } else CompletableDeferred(initialPacked)

    override suspend fun unpacked() = unpacked.get()


    override suspend fun packed(): ByteArray = packingTask.await()

}