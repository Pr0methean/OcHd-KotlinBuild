package io.github.pr0methean.ochd.packedimage

import io.github.pr0methean.ochd.SoftAsyncLazy
import javafx.scene.image.Image
import javafx.scene.image.PixelFormat
import javafx.scene.image.PixelReader
import javafx.scene.image.WritablePixelFormat
import javafx.scene.paint.Color
import kotlinx.coroutines.runBlocking
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.IntBuffer

abstract class AbstractPixelReader(val unpacked: suspend () -> Image) : PixelReader {
    override fun getPixelFormat(): PixelFormat<IntBuffer> = PixelFormat.getIntArgbInstance()

    abstract override fun getArgb(x: Int, y: Int): Int

    abstract override fun getColor(x: Int, y: Int): Color

    abstract override fun <T : Buffer> getPixels(
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        pixelformat: WritablePixelFormat<T>,
        buffer: T,
        scanlineStride: Int
    )

    override fun getPixels(
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        pixelformat: WritablePixelFormat<ByteBuffer>,
        buffer: ByteArray,
        offset: Int,
        scanlineStride: Int
    ) {
        val bufferWrapper = ByteBuffer.wrap(buffer)
        bufferWrapper.position(offset)
        getPixels(x, y, w, h, pixelformat, bufferWrapper, scanlineStride)
    }

    override fun getPixels(
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        pixelformat: WritablePixelFormat<IntBuffer>,
        buffer: IntArray,
        offset: Int,
        scanlineStride: Int
    ) {
        val bufferWrapper = IntBuffer.wrap(buffer)
        bufferWrapper.position(offset)
        getPixels(x, y, w, h, pixelformat, bufferWrapper, scanlineStride)
    }

    private val sourceReader = SoftAsyncLazy {
        unpacked().pixelReader
    }

    protected fun sourceReader(): PixelReader = runBlocking { sourceReader.get() }
}