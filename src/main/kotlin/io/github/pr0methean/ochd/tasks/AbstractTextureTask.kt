package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.Retryer
import io.github.pr0methean.ochd.packedimage.ImageNode
import kotlinx.coroutines.*
import org.apache.logging.log4j.LogManager
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets

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
    val name by lazy { StringBuilder().also { formatTo(it) }.toString() }

    override fun isComplete() = coroutine.isCompleted
    override fun isStarted(): Boolean = coroutine.isActive || coroutine.isCompleted

    override suspend fun getImage(): ImageNode = coroutine.await()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getImageNow(): ImageNode? = if (coroutine.isCompleted) coroutine.getCompleted() else null

    /** Must be final to supersede the generated implementation for data classes */
    final override fun toString(): String = name
    protected abstract suspend fun createImage(): ImageNode
}

@Suppress("BlockingMethodInNonBlockingContext")
suspend fun <T> doJfx(name: String, retryer: Retryer, jfxCode: CoroutineScope.() -> T): T
        = retryer.retrying(name) { withContext(Dispatchers.Main.plus(CoroutineName(name))) {
            val oldSystemErr = System.err
            try {
                ByteArrayOutputStream().use { errorCatcher ->
                    System.setErr(PrintStream(errorCatcher))
                    logger.info("Starting JFX task: {}", name)
                    val result = jfxCode()
                    logger.info("Finished JFX task: {}", name)
                    if (errorCatcher.size() > 0) {
                        throw RuntimeException(errorCatcher.toString(StandardCharsets.UTF_8))
                    }
                    return@withContext result
                }
            } finally {
                System.setErr(oldSystemErr)
            }
}}
