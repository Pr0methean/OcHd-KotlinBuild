package io.github.pr0methean.ochd.packedimage

import io.github.pr0methean.ochd.*
import io.github.pr0methean.ochd.tasks.doJfx
import javafx.embed.swing.SwingFXUtils
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.scene.paint.Color
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.logging.log4j.LogManager
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import javax.imageio.ImageIO

private val logger = LogManager.getLogger("ImageNode")
abstract class ImageNode(
    initialUnpacked: Image? = null, initialPacked: ByteArray? = null,
    val name: String, val scope: CoroutineScope, val retryer: Retryer,
    val stats: ImageProcessingStats,
    val packer: ImagePacker
) {

    protected val pngBytes = StrongAsyncLazy(initialPacked) {
        ByteArrayOutputStream().use {
            retryer.retrying<ByteArray>("Compression of $name") {
                stats.onCompressPngImage(name)
                @Suppress("BlockingMethodInNonBlockingContext")
                ImageIO.write(SwingFXUtils.fromFXImage(unpacked(), null), "PNG", it)
                return@retrying it.toByteArray()
            }
        }.also {
            logger.info("Done compressing {}", name)
        }
    }

    open suspend fun renderTo(out: GraphicsContext, x: Int, y: Int) {
        val unpacked = unpacked()
        doJfx(name, retryer) {out.drawImage(unpacked, x.toDouble(), y.toDouble())}
    }

    open val unpacked: AsyncLazy<Image> = SoftAsyncLazy(initialUnpacked, this::unpack)

    suspend fun unpacked(): Image = unpacked.get()

    abstract suspend fun unpack(): Image

    suspend fun asPng(): ByteArray = pngBytes.get()
    suspend fun writePng(destination: File) = withContext(Dispatchers.IO) {
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

