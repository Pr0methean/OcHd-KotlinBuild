package io.github.pr0methean.ochd.packedimage

import javafx.scene.canvas.Canvas
import javafx.scene.image.Image
import javafx.scene.paint.Paint

interface ImageTile {
    suspend fun asImage(): Image

    suspend fun repainted(paint: Paint, alpha: Double = 1.0): ImageTile

    suspend fun drawOnCanvas(canvas: Canvas, x: Int, y: Int)
}