package io.github.pr0methean.ochd

import com.google.common.collect.ConcurrentHashMultiset
import com.google.common.collect.Multiset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import org.apache.logging.log4j.LogManager
import java.util.concurrent.atomic.LongAdder
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

private fun Multiset<*>.log() {
    toSet().forEach { logger.info("{}: {}", it, count(it)) }
}
private val REPORTING_INTERVAL: Duration = 1.minutes
private val logger = LogManager.getLogger("ImageProcessingStats")

@Suppress("DeferredResultUnused")
fun startMonitoring(stats: ImageProcessingStats, scope: CoroutineScope) {
    scope.async {
        while (true) {
            delay(REPORTING_INTERVAL)
            logger.info("Completed tasks:")
            stats.taskCompletions.log()
        }
    }
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

    fun onTaskLaunched(task: Any) {
        logger.info("Launched: {}", task)
        taskLaunches.add(task as? String ?: task::class.simpleName ?: "[unnamed class]")
    }

    fun onTaskCompleted(task: Any) {
        logger.info("Completed: {}", task)
        taskCompletions.add(task as? String ?: task::class.simpleName ?: "[unnamed class]")
    }
}