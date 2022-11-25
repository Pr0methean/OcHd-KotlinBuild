package io.github.pr0methean.ochd

import javafx.scene.canvas.Canvas
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart.LAZY
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import org.apache.logging.log4j.LogManager
import java.util.concurrent.atomic.AtomicBoolean

private val logger = LogManager.getLogger("CanvasManager")
class CanvasManager(private val tileSize: Int,
                    private val standardTileCapacity: Int,
                    private val hugeTileCapacity: Int,
                    scope: CoroutineScope) {
    private val hugeCanvasShutdown = AtomicBoolean(false)
    private val standardCanvasChannel = Channel<Canvas>(standardTileCapacity)
    private val hugeCanvasChannel = Channel<Canvas>(hugeTileCapacity)
    private val standardCanvasInit = scope.launch(start = LAZY) {
        logger.debug("Creating standard canvases")
        repeat(standardTileCapacity) {standardCanvasChannel.send(Canvas(tileSize.toDouble(), tileSize.toDouble()))}
    }
    private val hugeCanvasInit = scope.launch(start = LAZY) {
        logger.debug("Creating huge-tile canvases")
        repeat(hugeTileCapacity) {hugeCanvasChannel.send(Canvas(tileSize.toDouble(), (4 * tileSize).toDouble()))}
    }
    suspend fun hugeCanvasShutdown() {
        if (hugeCanvasShutdown.compareAndSet(false, true)) {
            var remaining = hugeTileCapacity
            repeat(hugeTileCapacity) {
                logger.debug("Waiting for {} huge-tile canvases to be returned", remaining)
                hugeCanvasChannel.receive()
                remaining--
            }
        }
        logger.debug("All canvases returned")
        hugeCanvasChannel.close()
    }
    private suspend fun borrowCanvas(width: Int, height: Int): Canvas {
        logger.debug("{} is waiting to borrow a {}x{} canvas", currentCoroutineContext(), width, height)
        try {
            if (width == tileSize) {
                if (height == tileSize) {
                    standardCanvasInit.start()
                    standardCanvasInit.join()
                    logger.debug("Lending a standard-tile canvas to {}", currentCoroutineContext())
                    return standardCanvasChannel.receive()
                }
                if (height == 4 * tileSize) {
                    hugeCanvasInit.start()
                    hugeCanvasInit.join()
                    if (hugeCanvasShutdown.get()) {
                        throw IllegalStateException("Already closed for huge canvases")
                    }
                    logger.debug("Lending a huge-tile canvas to {}", currentCoroutineContext())
                    return hugeCanvasChannel.receive()
                }
            }
            logger.debug("Lending an untracked canvas to {}", currentCoroutineContext())
            return Canvas(width.toDouble(), height.toDouble())
        } finally {
            logger.debug("Exiting borrowCanvas")
        }
    }
    private suspend fun returnCanvas(canvas: Canvas) {
        logger.debug("{} is returning a {}x{} canvas", currentCoroutineContext(), canvas.width, canvas.height)
        if (canvas.width == tileSize.toDouble()) {
            if (canvas.height == tileSize.toDouble()) {
                logger.debug("Putting the returned canvas in the standard-canvas channel")
                clear(canvas)
                standardCanvasChannel.send(canvas)
                logger.debug("Done putting the returned canvas in the standard-canvas channel")
                return
            } else if (canvas.height == (4 * tileSize).toDouble()) {
                logger.debug("Putting the returned canvas in the huge-canvas channel")
                clear(canvas)
                hugeCanvasChannel.send(canvas)
                logger.debug("Done putting the returned canvas in the huge-canvas channel")
                return
            }
        }
        logger.debug("Dropping an untracked canvas returned by {}", currentCoroutineContext())
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