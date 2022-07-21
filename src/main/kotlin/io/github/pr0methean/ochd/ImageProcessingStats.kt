package io.github.pr0methean.ochd

import com.google.common.collect.ConcurrentHashMultiset
import com.google.common.collect.Multiset
import kotlinx.coroutines.*
import org.apache.logging.log4j.LogManager
import java.util.concurrent.atomic.LongAdder
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

private fun Multiset<*>.log() {
    toSet().forEach { logger.info("{}: {}", it, count(it)) }
}
private val REPORTING_INTERVAL: Duration = 1.minutes
private val logger = LogManager.getLogger("ImageProcessingStats")
var monitoringJob: Job? = null
@Suppress("DeferredResultUnused")
fun startMonitoring(stats: ImageProcessingStats, scope: CoroutineScope) {
    monitoringJob = scope.async {
        while (true) {
            delay(REPORTING_INTERVAL)
            logger.info("Completed tasks:")
            stats.taskCompletions.log()
        }
    }
}

fun stopMonitoring() {
    monitoringJob?.cancel("Monitoring stopped")
    monitoringJob = null
}

class ImageProcessingStats {
    val taskLaunches: ConcurrentHashMultiset<String> = ConcurrentHashMultiset.create()
    val taskCompletions: ConcurrentHashMultiset<String> = ConcurrentHashMultiset.create()
    val dedupeSuccesses: ConcurrentHashMultiset<String> = ConcurrentHashMultiset.create()
    val dedupeFailures: ConcurrentHashMultiset<String> = ConcurrentHashMultiset.create()
    val compressions = LongAdder()
    val decompressions = LongAdder()
    val retries = LongAdder()

    fun log() {
        logger.info("")
        logger.info("Task launches:")
        taskLaunches.log()
        logger.info("")
        logger.info("Deduplicated tasks:")
        dedupeSuccesses.log()
        logger.info("")
        logger.info("Non-deduplicated tasks:")
        dedupeFailures.log()
        logger.info("")
        logger.info("PNG compressions: {}", compressions.sum())
        logger.info("PNG decompressions: {}", decompressions.sum())
        logger.info("Retries of failed tasks: {}", retries.sum())
    }

    fun onDecompressPngImage(name: String) {
        logger.info("Decompressing {} from PNG", name)
        decompressions.increment()
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