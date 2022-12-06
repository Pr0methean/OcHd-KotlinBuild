package io.github.pr0methean.ochd.tasks

import javafx.application.Platform
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.logging.log4j.LogManager
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.util.concurrent.atomic.AtomicBoolean

private val LOGGER = LogManager.getLogger("doJfx")
private val DEFAULT_ERR = System.err
private val DEFAULT_CHARSET = DEFAULT_ERR.charset()
private val ERR_CATCHER = ByteArrayOutputStream()
private val ERR_CATCHER_STREAM = PrintStream(ERR_CATCHER, true, DEFAULT_CHARSET)
private val SYSERR_SWITCHED = AtomicBoolean(false)
@Suppress("BlockingMethodInNonBlockingContext")
suspend fun <T> doJfx(name: String, jfxCode: CoroutineScope.() -> T): T = try {
    if (SYSERR_SWITCHED.compareAndSet(false, true)) {
        System.setErr(ERR_CATCHER_STREAM)
    }
    LOGGER.info("Starting JFX task: {}", name)
    val result = withContext(Dispatchers.Main.plus(CoroutineName(name))) {
        jfxCode()
    }
    ERR_CATCHER_STREAM.flush()
    if (ERR_CATCHER.size() > 0) {
        val interceptedStderr = ERR_CATCHER.toString(DEFAULT_CHARSET)
        ERR_CATCHER.reset()
        check(!interceptedStderr.contains("Exception:") && !interceptedStderr.contains("Error:")) {
            interceptedStderr.lineSequence().first()
        }
        DEFAULT_ERR.print(interceptedStderr)
    }
    LOGGER.info("Finished JFX task: {}", name)
    result
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
