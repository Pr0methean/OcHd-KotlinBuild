package io.github.pr0methean.ochd.tasks.consumable

import javafx.application.Platform
import kotlinx.coroutines.*
import org.apache.logging.log4j.LogManager
import java.io.ByteArrayOutputStream
import java.io.PrintStream

private val logger = LogManager.getLogger("doJfx")
@Suppress("BlockingMethodInNonBlockingContext")
suspend fun <T> doJfx(name: String, jfxCode: CoroutineScope.() -> T): T = try {
    ByteArrayOutputStream().use { errorCatcher ->
        logger.info("Starting JFX task: {}", name)
        val result = PrintStream(errorCatcher, true, System.err.charset()).use { tempStderr ->
            withContext(Dispatchers.Main.plus(CoroutineName(name))) {
                val oldSystemErr = System.err
                try {
                    System.setErr(tempStderr)
                    jfxCode()
                } finally {
                    withContext(NonCancellable) {
                        System.setErr(oldSystemErr)
                    }
                }
            }
        }
        if (errorCatcher.size() > 0) {
            val interceptedStdout = errorCatcher.toString(System.err.charset())
            if (interceptedStdout.contains("Exception:") || interceptedStdout.contains("Error:")) {
                throw RuntimeException(interceptedStdout)
            }
            System.err.print(interceptedStdout)
        }
        logger.info("Finished JFX task: {}", name)
        result
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
}