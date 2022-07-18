package io.github.pr0methean.ochd.packedimage

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.SoftAsyncLazy
import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.logging.LogManager
import javax.imageio.ImageIO

private val logger = LogManager.getLogManager().getLogger("PngImage")
class PngImage(initialUnpacked: Image?, private val packingTask: Deferred<ByteArray>,
               val ctx: ImageProcessingContext, val name: String) : PackedImage {
    val unpacked = SoftAsyncLazy(initialUnpacked) {
        ctx.onDecompressPngImage(name)
        return@SoftAsyncLazy ctx.retrying("Decompression of $name") { ByteArrayInputStream(packed()).use { Image(it) } }
            .also { logger.info("Done decompressing $name") }
    }

    constructor(input: Image, name: String, ctx: ImageProcessingContext):
        this(initialUnpacked = input,
            packingTask = ctx.scope.async {
            ByteArrayOutputStream().use {
                ctx.retrying("Compression of $name") {
                    ctx.onCompressPngImage(name)
                    @Suppress("BlockingMethodInNonBlockingContext")
                    ImageIO.write(SwingFXUtils.fromFXImage(input, null), "PNG", it)
                    logger.info("Done compressing to PNG: $name")
                    return@retrying it.toByteArray()
                }
            }
        },
            ctx = ctx, name = name)

    constructor(pngInput: ByteArray, ctx: ImageProcessingContext, name: String):
        this(initialUnpacked = null,
            packingTask = CompletableDeferred(pngInput),
            ctx = ctx, name = name)

    override suspend fun unpacked() = unpacked.get()


    override suspend fun packed(): ByteArray = packingTask.await()
    override fun isAlreadyUnpacked(): Boolean = unpacked.isCompleted()

    override fun isAlreadyPacked(): Boolean = packingTask.isCompleted
}