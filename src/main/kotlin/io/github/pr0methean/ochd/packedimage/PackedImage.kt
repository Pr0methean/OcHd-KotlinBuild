package io.github.pr0methean.ochd.packedimage

import io.github.pr0methean.ochd.AsyncLazy
import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.SoftAsyncLazy
import io.github.pr0methean.ochd.StrongAsyncLazy
import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import javafx.scene.paint.Color
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.logging.log4j.LogManager
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import javax.imageio.ImageIO

private val logger = LogManager.getLogger("ImageNode")
abstract class PackedImage(
    initialUnpacked: Image? = null, initialPacked: ByteArray? = null,
    val name: String, val scope: CoroutineScope,
    val stats: ImageProcessingStats,
    val packer: ImagePacker
) {

    protected val pngBytes = StrongAsyncLazy(initialPacked) {
        return@StrongAsyncLazy ByteArrayOutputStream().use {
            stats.onCompressPngImage(name)
            @Suppress("BlockingMethodInNonBlockingContext")
            ImageIO.write(SwingFXUtils.fromFXImage(initialUnpacked ?: unpacked(), null), "PNG", it)
            val packed = it.toByteArray()
            logger.info("Done compressing {}", name)
            packed
        }
    }

    @Suppress("LeakingThis")
    open val unpacked: AsyncLazy<Image> = SoftAsyncLazy(initialUnpacked, this::unpack)

    suspend fun unpacked(): Image {
        val unpacked = unpacked.get()
        if (unpacked.isError) {
            this.unpacked.set(null)
            throw unpacked.exception
        }
        return unpacked
    }

    abstract suspend fun unpack(): Image

    suspend fun asPng(): ByteArray = pngBytes.get()
    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun writePng(destination: File) = withContext(Dispatchers.IO.plus(CoroutineName(name))) {
        val pngBytes = asPng()
        destination.parentFile?.mkdirs()
        FileOutputStream(destination).use { it.write(pngBytes) }
    }

}

fun alphaBlend(foreground: Color, background: Color): Color {
    if (foreground.opacity == 1.0) {
        return foreground
    }
    val outOpacity = foreground.opacity + (1 - foreground.opacity) * background.opacity
    if (outOpacity == 0.0) {
        return Color.TRANSPARENT
    }
    val outRed = alphaBlendChannel(foreground, background, outOpacity, Color::getRed)
    val outGreen = alphaBlendChannel(foreground, background, outOpacity, Color::getGreen)
    val outBlue = alphaBlendChannel(foreground, background, outOpacity, Color::getBlue)
    return Color(outRed, outGreen, outBlue, outOpacity)
}

private fun alphaBlendChannel(
    foreground: Color,
    background: Color,
    outOpacity: Double,
    channelSelector: (Color) -> Double
) = (channelSelector(foreground) * foreground.opacity + channelSelector(background) * background.opacity * (1 - foreground.opacity)) / outOpacity

