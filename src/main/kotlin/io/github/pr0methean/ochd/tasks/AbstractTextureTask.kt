package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.packedimage.PackedImage
import kotlinx.coroutines.*
import org.apache.logging.log4j.LogManager
import java.io.ByteArrayOutputStream
import java.io.PrintStream

private val logger = LogManager.getLogger("AbstractTextureTask")
abstract class AbstractTextureTask(open val scope: CoroutineScope,
                                   open val stats: ImageProcessingStats
) : TextureTask {
    private val coroutine by lazy {
        val typeName = this::class.simpleName ?: "[unnamed AbstractTextureTask]"
        scope.async(CoroutineName(name), start = CoroutineStart.LAZY) {
            stats.onTaskLaunched(typeName, name)
            val image = createImage()
            stats.onTaskCompleted(typeName, name)
            image
        }
    }
    override val name by lazy { StringBuilder().also { formatTo(it) }.toString() }

    override fun isComplete() = coroutine.isCompleted
    override fun isStarted(): Boolean = coroutine.isActive || coroutine.isCompleted

    override suspend fun getImage(): PackedImage = coroutine.await()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getImageNow(): PackedImage? = if (coroutine.isCompleted) coroutine.getCompleted() else null

    /** Must be final to supersede the generated implementation for data classes */
    final override fun toString(): String = name
    protected abstract suspend fun createImage(): PackedImage
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
        System.setErr(oldSystemErr)
    }
}
