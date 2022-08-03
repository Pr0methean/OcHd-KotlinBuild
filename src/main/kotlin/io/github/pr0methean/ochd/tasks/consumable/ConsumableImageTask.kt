package io.github.pr0methean.ochd.tasks.consumable

import javafx.scene.image.Image
import kotlinx.coroutines.*
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.util.StringBuilderFormattable
import java.io.ByteArrayOutputStream
import java.io.PrintStream

private val logger = LogManager.getLogger("ConsumableImageTask")
@Suppress("BlockingMethodInNonBlockingContext")
suspend fun <T> doJfx(name: String, jfxCode: CoroutineScope.() -> T): T {
    val oldSystemErr = System.err
    try {
        ByteArrayOutputStream().use { errorCatcher ->
            System.setErr(PrintStream(errorCatcher, true, oldSystemErr.charset()))
            logger.info("Starting JFX task: {}", name)
            val result = withContext(Dispatchers.Main.plus(CoroutineName(name))) {
                jfxCode()
            }
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

interface ConsumableImageTask: StringBuilderFormattable, ConsumableTask<Image> {
    val unpacked: ConsumableTask<Image>

    val asPng: ConsumableTask<ByteArray>

    override suspend fun checkSanity() {
        unpacked.checkSanity()
        asPng.checkSanity()
    }

    @Suppress("DeferredResultUnused")
    override suspend fun startAsync(): Deferred<Result<Image>>
}