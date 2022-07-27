package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.SoftAsyncLazy
import io.github.pr0methean.ochd.packedimage.PackedImage
import kotlinx.coroutines.*
import org.apache.logging.log4j.LogManager
import java.io.ByteArrayOutputStream
import java.io.PrintStream

private val logger = LogManager.getLogger("AbstractTextureTask")
abstract class AbstractTextureTask(open val scope: CoroutineScope,
                                   open val stats: ImageProcessingStats,
                                   initialResult: PackedImage? = null
) : TextureTask {
    private val result = SoftAsyncLazy(initialResult?.let {Result.success(it)}) {
        val typeName = this::class.simpleName ?: "[unnamed AbstractTextureTask]"
        stats.onTaskLaunched(typeName, name)
        val image = try {
            createImage()
        } catch (t: Throwable) {
            logger.error("Error in {}", name, t)
            return@SoftAsyncLazy Result.failure(t)
        }
        stats.onTaskCompleted(typeName, name)
        return@SoftAsyncLazy Result.success(image)
    }

    override val name by lazy { StringBuilder().also { formatTo(it) }.toString() }
    override fun isFailed() = result.getNow()?.isFailure == true

    override fun isComplete() = result.getNow() != null
    override fun isStarted() = result.isStarted()

    override suspend fun getImage(): PackedImage = result.get().getOrThrow()

    override suspend fun join(): Result<PackedImage> = result.get()

    override fun getImageNow(): PackedImage? = result.getNow()?.getOrThrow()

    /** Must be final to supersede the generated implementation for data classes */
    final override fun toString(): String = name
    protected abstract suspend fun createImage(): PackedImage
    override fun clearResult() {
        result.set(null)
    }
}

@Suppress("BlockingMethodInNonBlockingContext")
suspend fun <T> doJfx(name: String, jfxCode: CoroutineScope.() -> T): T {
    val oldSystemErr = System.err
    try {
        ByteArrayOutputStream().use { errorCatcher ->
            System.setErr(PrintStream(errorCatcher, true, oldSystemErr.charset()))
            logger.info("Starting JFX task: {}", name)
            val result = withContext(Dispatchers.Main.plus(CoroutineName(name))) { jfxCode() }
            if (errorCatcher.size() > 0) {
                throw RuntimeException(errorCatcher.toString(oldSystemErr.charset()))
            }
            logger.info("Finished JFX task: {}", name)
            return result
        }
    } finally {
        withContext(NonCancellable) {
            System.setErr(oldSystemErr)
        }
    }
}
