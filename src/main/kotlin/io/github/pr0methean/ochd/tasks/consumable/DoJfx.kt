package io.github.pr0methean.ochd.tasks.consumable

import javafx.application.Platform
import kotlinx.coroutines.*
import org.apache.logging.log4j.LogManager
import java.io.ByteArrayOutputStream
import java.io.PrintStream

private val logger = LogManager.getLogger("doJfx")
@Suppress("BlockingMethodInNonBlockingContext")
suspend fun <T> doJfx(name: String, jfxCode: CoroutineScope.() -> T): T {
    return withContext(Dispatchers.Main.plus(CoroutineName(name))) {
        val oldSystemErr = System.err
        try {
            ByteArrayOutputStream().use { errorCatcher ->
                System.setErr(PrintStream(errorCatcher, true, oldSystemErr.charset()))
                logger.info("Starting JFX task: {}", name)
                val result = jfxCode()
                if (errorCatcher.size() > 0) {
                    throw RuntimeException(errorCatcher.toString(oldSystemErr.charset()))
                }
                logger.info("Finished JFX task: {}", name)
                return@use result
            }
        } catch (t: Throwable) {
            logger.error("Error from JFX task", t)
            // Start a new JFX thread if the old one crashed
            try {
                Platform.startup {}
            } catch (e: IllegalStateException) {
                logger.debug("Error trying to restart JFX thread", e)
            }
            throw t
        } finally {
            withContext(NonCancellable) {
                System.setErr(oldSystemErr)
            }
        }
    }
}