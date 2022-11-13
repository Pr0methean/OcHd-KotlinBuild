package io.github.pr0methean.ochd.tasks

import javafx.application.Platform
import kotlinx.coroutines.*
import org.apache.logging.log4j.LogManager
import java.io.ByteArrayOutputStream
import java.io.PrintStream

private val LOGGER = LogManager.getLogger("doJfx")
@Suppress("BlockingMethodInNonBlockingContext")
suspend fun <T> doJfx(name: String, jfxCode: CoroutineScope.() -> T): T = try {
    ByteArrayOutputStream().use { errorCatcher ->
        LOGGER.info("Starting JFX task: {}", name)
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
        LOGGER.info("Finished JFX task: {}", name)
        result
    }
} catch (t: Throwable) {
    LOGGER.error("Error from JFX task", t)
    // Start a new JFX thread if the old one crashed
    try {
        Platform.startup {}
    } catch (e: IllegalStateException) {
        LOGGER.debug("Error trying to restart JFX thread", e)
    }
    throw t
}