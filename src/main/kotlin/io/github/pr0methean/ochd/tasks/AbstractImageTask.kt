package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.DEFAULT_SNAPSHOT_PARAMS
import io.github.pr0methean.ochd.TaskPlanningContext
import io.github.pr0methean.ochd.tasks.caching.DeferredTaskCache
import javafx.scene.SnapshotParameters
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
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

const val ARGB_BITS_PER_CHANNEL: Int = Byte.SIZE_BITS
const val ARGB_ALPHA_BIT_SHIFT: Int = 3 * ARGB_BITS_PER_CHANNEL

private val logger = LogManager.getLogger("AbstractImageTask")

private val defaultErr: PrintStream = System.err
private val defaultErrCharset: Charset = defaultErr.charset()
private val errCatcher: ByteArrayOutputStream = ByteArrayOutputStream()
private val errCatcherStream: PrintStream = PrintStream(errCatcher, false, defaultErrCharset)
private val systemErrSwitched: AtomicBoolean = AtomicBoolean(false)
private val pendingSnapshotTasks: AtomicLong = AtomicLong(0)

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
        logger.info("Allocating a Canvas for {}", name)
        return Canvas(width.toDouble(), height.toDouble())
    }

    protected fun createWritableImage(): WritableImage {
        logger.info("Allocating a WritableImage for {}", name)
        return WritableImage(width, height)
    }

    protected suspend fun snapshotCanvas(canvas: Canvas, params: SnapshotParameters = DEFAULT_SNAPSHOT_PARAMS): Image {
        canvas.graphicsContext2D.isImageSmoothing = false
        if (systemErrSwitched.compareAndSet(false, true)) {
            System.setErr(errCatcherStream)
        }
        val caughtStderr = AtomicReference<String?>(null)
        val output = createWritableImage()
        logger.info("Waiting to snapshot canvas for {}. Pending snapshot tasks: {}",
                name, box(pendingSnapshotTasks.incrementAndGet()))
        val startWaitingTime = System.nanoTime()
        val snapshot = withContext(Dispatchers.Main.plus(CoroutineName("Snapshot of $name"))) {
            logger.info("Snapshotting canvas for {} after waiting {} ns", name,
                    box(System.nanoTime() - startWaitingTime))
            try {
                val snapshot: Image
                val ns = measureNanoTime {
                    snapshot = canvas.snapshot(params, output)
                }
                logger.info("Finished snapshotting canvas for {} after {} ns. Pending snapshot tasks: {}",
                    name, box(ns), box(pendingSnapshotTasks.decrementAndGet()))
                return@withContext snapshot
            } finally {
                errCatcherStream.flush()
                if (errCatcher.size() > 0) {
                    caughtStderr.set(errCatcher.toString(defaultErrCharset))
                    errCatcher.reset()
                }
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

    /**
     * If false, this task's output is the same as its output repainted black and can be deduplicated accordingly.
     */
    abstract fun hasColor(): Boolean

    /**
     * Returns a list of previousLayer and this if there's no benefit to combining them, or a combined
     * version if there is.
     */
    open fun tryCombineWith(previousLayer: AbstractImageTask, ctx: TaskPlanningContext): List<AbstractImageTask>
            = listOf(previousLayer, this)
}

fun pendingSnapshotTasks(): Long = pendingSnapshotTasks.get()

fun Paint.toOpaque(): Paint {
    if (this.isOpaque) {
        return this
    }
    if (this is Color) {
        return Color(red, green, blue, 1.0)
    }
    error("Can't convert $this to opaque")
}
