package io.github.pr0methean.ochd

import com.google.common.collect.ConcurrentHashMultiset
import com.google.common.collect.Multiset
import com.sun.glass.ui.Application
import kotlinx.coroutines.*
import kotlinx.coroutines.debug.DebugProbes
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.io.IoBuilder
import org.apache.logging.log4j.util.Unbox
import java.lang.management.ManagementFactory
import java.lang.management.ThreadInfo
import java.util.concurrent.atomic.LongAdder
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

private fun Multiset<*>.log() {
    toSet().forEach { logger.info("{}: {}", it, count(it)) }
}
private val logger = LogManager.getLogger("ImageProcessingStats")
private val NEED_THREAD_MONITORING = false
private val NEED_COROUTINE_DEBUG = logger.isDebugEnabled
private val REPORTING_INTERVAL: Duration = 1.minutes
val threadMxBean = ManagementFactory.getThreadMXBean()
var monitoringJob: Job? = null
@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("DeferredResultUnused")
fun startMonitoring(stats: ImageProcessingStats, scope: CoroutineScope) {
    if (NEED_COROUTINE_DEBUG) {
        DebugProbes.install()
    }
    val loggerStream = IoBuilder.forLogger(logger).setLevel(Level.DEBUG).buildPrintStream()
    monitoringJob = scope.launch(CoroutineName("Stats monitoring job")) {
        while (true) {
            delay(REPORTING_INTERVAL)
            logger.info("Completed tasks:")
            stats.taskCompletions.log()
            Application.GetApplication().notifyRenderingFinished()
            if (NEED_THREAD_MONITORING) {
                val deadlocks = threadMxBean.findDeadlockedThreads()
                if (deadlocks == null) {
                    logger.info("No deadlocked threads found.")
                    threadMxBean.allThreadIds.map { it to threadMxBean.getThreadInfo(it, 20) }
                        .forEach { (id, threadInfo) ->
                            logThread(Level.INFO, id, threadInfo)
                        }
                } else {
                    logger.error("Deadlocked threads found!")
                    deadlocks.map { it to threadMxBean.getThreadInfo(it, 20) }.forEach { (id, threadInfo) ->
                        logThread(Level.ERROR, id, threadInfo)
                    }
                }
            }
            if (NEED_COROUTINE_DEBUG) {
                DebugProbes.dumpCoroutines(loggerStream)
            }
        }
    }
}

fun logThread(logLevel: Level, id: Long, threadInfo: ThreadInfo) {
    logger.log(logLevel, "Thread: {} (id: {})", threadInfo.threadName, Unbox.box(id))
    threadInfo.stackTrace.forEach {logger.log(logLevel, "{}.{} ({} line {})",
        it.className, it.methodName, it.fileName, it.lineNumber)}
}

fun stopMonitoring() {
    monitoringJob?.cancel("Monitoring stopped")
}

class ImageProcessingStats {
    val taskLaunches: ConcurrentHashMultiset<String> = ConcurrentHashMultiset.create()
    val taskCompletions: ConcurrentHashMultiset<String> = ConcurrentHashMultiset.create()
    val dedupeSuccesses: ConcurrentHashMultiset<String> = ConcurrentHashMultiset.create()
    val dedupeFailures: ConcurrentHashMultiset<String> = ConcurrentHashMultiset.create()
    val compressions = LongAdder()
    val retries = LongAdder()

    fun log() {
        logger.info("")
        logger.info("Task launches:")
        taskLaunches.log()
        logger.info("")
        logger.info("Task completions:")
        taskCompletions.log()
        logger.info("")
        logger.info("Deduplicated tasks:")
        dedupeSuccesses.log()
        logger.info("")
        logger.info("Non-deduplicated tasks:")
        dedupeFailures.log()
        logger.info("")
        logger.info("PNG compressions: {}", compressions.sum())
        logger.info("Retries of failed tasks: {}", retries.sum())
    }

    fun onCompressPngImage(name: String) {
        logger.info("Compressing {} to PNG", name)
        compressions.increment()
    }

    fun onTaskLaunched(typename: String, name: String) {
        logger.info("Launched: {}", name)
        taskLaunches.add(typename)
    }

    fun onTaskCompleted(typename: String, name: String) {
        logger.info("Completed: {}", name)
        taskCompletions.add(typename)
    }
}