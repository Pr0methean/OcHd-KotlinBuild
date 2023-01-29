package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.DEFAULT_SNAPSHOT_PARAMS
import io.github.pr0methean.ochd.tasks.caching.DeferredTaskCache
import javafx.scene.SnapshotParameters
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.util.Unbox.box
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.Charset
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.CoroutineContext
import kotlin.system.measureNanoTime

private val logger = LogManager.getLogger("AbstractImageTask")

private val defaultErr: PrintStream = System.err
private val defaultErrCharset: Charset = defaultErr.charset()
private val errCatcher: ByteArrayOutputStream = ByteArrayOutputStream()
private val errCatcherStream: PrintStream = PrintStream(errCatcher, true, defaultErrCharset)
private val systemErrSwitched: AtomicBoolean = AtomicBoolean(false)
private val pendingSnapshotTasks: AtomicLong = AtomicLong(0)

fun countPendingSnapshotTasks() = pendingSnapshotTasks.get()

/** Specialization of [AbstractTask]&lt;[Image]&gt;. */
abstract class AbstractImageTask(
    name: String, cache: DeferredTaskCache<Image>,
    ctx: CoroutineContext,
    val width: Int,
    val height: Int
)
    : AbstractTask<Image>(name, cache, ctx) {
    override fun mergeWithDuplicate(other: AbstractTask<*>): AbstractImageTask {
        return super.mergeWithDuplicate(other) as AbstractImageTask
    }

    open suspend fun renderOnto(contextSupplier: () -> GraphicsContext, x: Double, y: Double) {
        val image = await()
        contextSupplier().drawImage(image, x, y)
    }

    protected fun createCanvas(): Canvas {
        logger.info("Allocating a canvas for {}", name)
        return Canvas(width.toDouble(), height.toDouble())
    }

    protected suspend fun snapshotCanvas(canvas: Canvas, params: SnapshotParameters = DEFAULT_SNAPSHOT_PARAMS): Image {
        canvas.graphicsContext2D.isImageSmoothing = false
        if (systemErrSwitched.compareAndSet(false, true)) {
            System.setErr(errCatcherStream)
        }
        val caughtStderr = AtomicReference<String?>(null)
        val output = WritableImage(canvas.width.toInt(), canvas.height.toInt())
        pendingSnapshotTasks.getAndIncrement()
        logger.info("Waiting to snapshot canvas for {}", name)
        val startWaitingTime = System.nanoTime()
        val snapshot = withContext(Dispatchers.Main.plus(CoroutineName("Snapshot of $name"))) {
            logger.info("Snapshotting canvas for {} after waiting {} ns", name,
                    box(System.nanoTime() - startWaitingTime))
            try {
                val snapshot: Image
                val ns = measureNanoTime {
                    snapshot = canvas.snapshot(params, output)
                }
                logger.info("Finished snapshotting canvas for {} after {} ns", name, box(ns))
                return@withContext snapshot
            } finally {
                errCatcherStream.flush()
                if (errCatcher.size() > 0) {
                    caughtStderr.set(errCatcher.toString(defaultErrCharset))
                    errCatcher.reset()
                }
                pendingSnapshotTasks.getAndDecrement()
            }
        }
        val interceptedStderr = caughtStderr.get()
        if (interceptedStderr != null) {
            try {
                check(!interceptedStderr.contains("Exception:") && !interceptedStderr.contains("Error:")) {
                    interceptedStderr.lineSequence().first()
                }
            } finally {
                defaultErr.print(interceptedStderr)
            }
        }
        logger.info("Collected snapshot for {}", name)
        if (snapshot.isError) {
            throw output.exception
        }
        return snapshot
    }
}
