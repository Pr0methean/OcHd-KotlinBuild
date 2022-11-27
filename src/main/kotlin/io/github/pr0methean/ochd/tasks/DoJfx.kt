package io.github.pr0methean.ochd.tasks

import javafx.application.Platform
import kotlinx.coroutines.*
import org.apache.logging.log4j.LogManager
import java.io.ByteArrayOutputStream
import java.io.PrintStream

private val LOGGER = LogManager.getLogger("doJfx")
private val DEFAULT_ERR = System.err
private val DEFAULT_CHARSET = DEFAULT_ERR.charset()
@Suppress("BlockingMethodInNonBlockingContext")
suspend fun <T> doJfx(name: String, jfxCode: CoroutineScope.() -> T): T = try {
    ByteArrayOutputStream().use { errorCatcher ->
        LOGGER.info("Starting JFX task: {}", name)
        val result = PrintStream(errorCatcher, true, DEFAULT_CHARSET).use { tempStderr ->
            withContext(Dispatchers.Main.plus(CoroutineName(name))) {
                try {
                    System.setErr(tempStderr)
                    jfxCode()
                } finally {
                    withContext(NonCancellable) {
                        System.setErr(DEFAULT_ERR)
                    }
                }
            }
        }
        if (errorCatcher.size() > 0) {
            val interceptedStdout = errorCatcher.toString(DEFAULT_CHARSET)
            if (interceptedStdout.contains("Exception:") || interceptedStdout.contains("Error:")) {
                throw RuntimeException(interceptedStdout)
            }
            DEFAULT_ERR.print(interceptedStdout)
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