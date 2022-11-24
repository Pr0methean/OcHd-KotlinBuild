package io.github.pr0methean.ochd

import javafx.scene.canvas.Canvas
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart.LAZY
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class CanvasManager(private val tileSize: Int,
                    private val standardTileCapacity: Int,
                    private val hugeTileCapacity: Int,
                    scope: CoroutineScope) {
    private val hugeCanvasShutdown = AtomicBoolean(false)
    private val standardCanvasChannel = Channel<Canvas>(standardTileCapacity)
    private val hugeCanvasChannel = Channel<Canvas>(hugeTileCapacity)
    private val standardCanvasInit = scope.launch(start = LAZY) {
        repeat(standardTileCapacity) {standardCanvasChannel.send(Canvas(tileSize.toDouble(), tileSize.toDouble()))}
    }
    private val hugeCanvasInit = scope.launch(start = LAZY) {
        repeat(hugeTileCapacity) {hugeCanvasChannel.send(Canvas(tileSize.toDouble(), (4 * tileSize).toDouble()))}
    }
    suspend fun hugeCanvasShutdown() {
        if (hugeCanvasShutdown.compareAndSet(false, true)) {
            repeat(hugeTileCapacity) {hugeCanvasChannel.receive()}
        }
    }
    suspend fun borrowCanvas(width: Int, height: Int): Canvas {
        if (width == tileSize) {
            if (height == tileSize) {
                standardCanvasInit.join()
                return standardCanvasChannel.receive()
            }
            if (height == 4 * tileSize) {
                if (hugeCanvasShutdown.get()) {
                    throw IllegalStateException("Already closed for huge canvases")
                }
                hugeCanvasInit.join()
                return hugeCanvasChannel.receive()
            }
        }
        return Canvas(width.toDouble(), height.toDouble())
    }
    suspend fun returnCanvas(canvas: Canvas) {
        if (canvas.width == tileSize.toDouble()) {
            if (canvas.height == tileSize.toDouble()) {
                clear(canvas)
                standardCanvasChannel.send(canvas)
            } else if (canvas.height == (4 * tileSize).toDouble()) {
                clear(canvas)
                hugeCanvasChannel.send(canvas)
            }
        }
    }
    suspend fun <T> withCanvas(width: Int, height: Int, block: suspend (Canvas) -> T): T {
        val canvas = borrowCanvas(width, height)
        try {
            return block(canvas)
        } finally {
            returnCanvas(canvas)
        }
    }
    private fun clear(canvas: Canvas) {
        canvas.graphicsContext2D.clearRect(0.0, 0.0, canvas.width, canvas.height)
    }
}