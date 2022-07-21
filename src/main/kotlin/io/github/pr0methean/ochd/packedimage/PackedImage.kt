package io.github.pr0methean.ochd.packedimage

import io.github.pr0methean.ochd.DEFAULT_SNAPSHOT_PARAMS
import io.github.pr0methean.ochd.Retryer
import io.github.pr0methean.ochd.tasks.doJfx
import javafx.scene.CacheHint
import javafx.scene.canvas.Canvas
import javafx.scene.effect.Blend
import javafx.scene.effect.BlendMode
import javafx.scene.effect.ColorInput
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

suspend fun superimpose(background: Paint = Color.TRANSPARENT, layers: Iterable<PackedImage>, width: Double, height: Double,
                        name: String, retryer: Retryer, packer: ImagePacker): PackedImage {
    val canvas = Canvas(width, height)
    val canvasCtx = canvas.graphicsContext2D
    if (background != Color.TRANSPARENT) {
        canvasCtx.fill = background
        doJfx(name, retryer) {
            canvasCtx.fillRect(0.0, 0.0, width, height)
        }
    }
    val layerImages = layers.map { it.unpacked() }
    doJfx(name, retryer) {layerImages.forEach { canvasCtx.drawImage(it, 0.0, 0.0) }}
    return packer.packImage(doJfx(name, retryer) {canvas.snapshot(DEFAULT_SNAPSHOT_PARAMS, null)}, null, name)
}

interface PackedImage {
    suspend fun unpacked(): Image
    suspend fun packed(): ByteArray

    suspend fun writePng(destination: File) = withContext(Dispatchers.IO) {
        destination.parentFile.mkdirs()
        FileOutputStream(destination).use { it.write(packed()) }
    }

    suspend fun repaint(newPaint: Paint? = null, alpha: Double = 1.0, name: String, retryer: Retryer, packer: ImagePacker): PackedImage {
        val unpacked = unpacked()
        val view = ImageView(unpacked)
        val width = unpacked.width
        val height = unpacked.height
        if (newPaint != null) {
            val colorLayer = ColorInput(0.0, 0.0, width, height, newPaint)
            val blend = Blend()
            blend.mode = BlendMode.SRC_ATOP
            blend.topInput = colorLayer
            blend.bottomInput = null
            view.effect = blend
        }
        view.opacity = alpha
        view.cacheHint = CacheHint.QUALITY
        view.isSmooth = true
        return packer.packImage(doJfx(name, retryer) {view.snapshot(DEFAULT_SNAPSHOT_PARAMS, null)}, null, name)
    }
}