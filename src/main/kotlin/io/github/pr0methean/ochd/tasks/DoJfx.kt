package io.github.pr0methean.ochd.tasks

import com.sun.prism.impl.Disposer
import javafx.application.Platform
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.Charset
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

val LOGGER: Logger = LogManager.getLogger("doJfx")
val DEFAULT_ERR: PrintStream = System.err
val DEFAULT_CHARSET: Charset = DEFAULT_ERR.charset()
val ERR_CATCHER: ByteArrayOutputStream = ByteArrayOutputStream()
val ERR_CATCHER_STREAM: PrintStream = PrintStream(ERR_CATCHER, true, DEFAULT_CHARSET)
val SYSERR_SWITCHED: AtomicBoolean = AtomicBoolean(false)

/**
 * Dispatches the given block of code to the JavaFX rendering thread, waits for it to execute, and checks for errors
 * logged to System.err while running.
 */
@Suppress("BlockingMethodInNonBlockingContext")
suspend inline fun <T> doJfx(name: String, crossinline jfxCode: CoroutineScope.() -> T): T = runCatching {
    if (SYSERR_SWITCHED.compareAndSet(false, true)) {
        System.setErr(ERR_CATCHER_STREAM)
    }
    LOGGER.info("Starting JFX task: {}", name)
    val caughtStderr = AtomicReference<String?>(null)
    val result = withContext(Dispatchers.Main.plus(CoroutineName(name))) {
        try {
            jfxCode()
        } finally {
            ERR_CATCHER_STREAM.flush()
            Disposer.cleanUp()
            if (ERR_CATCHER.size() > 0) {
                caughtStderr.set(ERR_CATCHER.toString(DEFAULT_CHARSET))
                ERR_CATCHER.reset()
            }
        }
    }
    val interceptedStderr = caughtStderr.getAndSet(null)
    if (interceptedStderr != null) {
        try {
            check(!interceptedStderr.contains("Exception:") && !interceptedStderr.contains("Error:")) {
                interceptedStderr.lineSequence().first()
            }
        } finally {
            DEFAULT_ERR.print(interceptedStderr)
        }
    }
    LOGGER.info("Finished JFX task: {}", name)
    result
}.onFailure{ t ->
    LOGGER.error("Error from JFX task", t)
    // Start a new JFX thread if the old one crashed
    try {
        Platform.startup {}
    } catch (e: IllegalStateException) {
        LOGGER.debug("Error trying to restart JFX thread", e)
    }
}.getOrThrow()
