package io.github.pr0methean.ochd

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.stats.CacheStats
import com.google.common.collect.ConcurrentHashMultiset
import com.google.common.collect.Multiset
import com.google.common.collect.Multisets
import io.github.pr0methean.ochd.tasks.caching.SemiStrongTaskCache
import kotlinx.coroutines.*
import kotlinx.coroutines.debug.DebugProbes
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.io.IoBuilder
import org.apache.logging.log4j.util.Unbox.box
import java.lang.management.ManagementFactory
import java.lang.management.ThreadInfo
import java.lang.management.ThreadMXBean
import java.util.concurrent.atomic.LongAdder
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

private fun Multiset<*>.log() {
    var total = 0L
    toSet().forEach {
        val count = count(it)
        total += count
        logger.info("{}: {}", it, box(count))
    }
    logger.info("Total: {}", box(total))
}
private fun <T> Multiset<T>.logIf(predicate: (T) -> Boolean) {
    toSet().forEach { if (predicate(it)) {logger.info("{}: {}", it, count(it))} }
}
private val logger = LogManager.getLogger("ImageProcessingStats")
private const val NEED_THREAD_MONITORING = false
private val NEED_COROUTINE_DEBUG = logger.isDebugEnabled
private val REPORTING_INTERVAL: Duration = 1.minutes
val threadMxBean: ThreadMXBean = ManagementFactory.getThreadMXBean()
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
    logger.log(logLevel, "Thread: {} (id: {})", threadInfo.threadName, box(id))
    threadInfo.stackTrace.forEach {logger.log(logLevel, "{}.{} ({} line {})",
        it.className, it.methodName, it.fileName, it.lineNumber)}
}

fun stopMonitoring() {
    monitoringJob?.cancel("Monitoring stopped")
}

fun CacheStats.log(name: String, estimatedSize: Long) {
    logger.info("Stats for {} cache: {} hits, {} misses, {} evictions, {} entries", name,
            box(hitCount()), box(missCount()), box(evictionCount()), box(estimatedSize))
}

class ImageProcessingStats(private val backingCache: Cache<SemiStrongTaskCache<*>, Result<*>>) {
    private val taskLaunches: ConcurrentHashMultiset<String> = ConcurrentHashMultiset.create()
    val taskCompletions: ConcurrentHashMultiset<String> = ConcurrentHashMultiset.create()
    val dedupeSuccesses: ConcurrentHashMultiset<String> = ConcurrentHashMultiset.create()
    val dedupeFailures: ConcurrentHashMultiset<String> = ConcurrentHashMultiset.create()
    private val retries = LongAdder()
    private val tasksByRunCount = ConcurrentHashMultiset.create<Pair<String, String>>()
    @Volatile lateinit var hugeCacheStats: CacheStats
    @Volatile var hugeCacheFinalSize: Long = -1
    init {
        dedupeFailures.add("Build task graph")

    }

    @Suppress("UnstableApiUsage")
    fun log() {
        logger.info("")
        logger.info("Task launches:")
        taskLaunches.log()
        logger.info("")
        logger.info("Task completions:")
        taskCompletions.log()
        logger.info("")
        logger.info("Necessary tasks:")
        dedupeFailures.log()
        logger.info("")
        logger.info("Deduplicated tasks:")
        dedupeSuccesses.log()
        logger.info("")
        logger.info("Tasks that would run with unlimited cache but no deduplication:")
        Multisets.sum(dedupeFailures, dedupeSuccesses).log()
        logger.info("")
        logger.info("Retries of failed tasks: {}", retries.sum())
        logger.info("Tasks repeated due to cache misses:")
        val repeatedTasks = Multisets.copyHighestCountFirst(tasksByRunCount)
        repeatedTasks.logIf {repeatedTasks.count(it) >= 2}
        logger.info("")
        logger.info("Task efficiency / hit rate")
        var totalUnique = 0L
        var totalActual = 0L
        var totalWorstCase = 0L
        dedupeSuccesses.toSet().forEach { className ->
            val unique = dedupeFailures.count(className)
            val actual = taskLaunches.count(className)
            val worstCase = unique + dedupeSuccesses.count(className)
            val efficiency = (unique.toDouble() / actual)
            val hitRate = 1.0 - (actual - unique).toDouble()/(worstCase - unique)
            logger.printf(Level.INFO, "%20s: %3.2f%% / %3.2f%%", className, 100.0 * efficiency, 100.0 * hitRate)
            totalUnique += unique
            totalActual += actual
            totalWorstCase += worstCase
        }
        val totalEfficiency = (totalUnique.toDouble() / totalActual)
        val totalHitRate = (totalActual - totalUnique).toDouble()/(totalWorstCase - totalUnique)
        logger.printf(Level.INFO, "Total               : %3.2f%% / %3.2f%%", 100.0 * totalEfficiency, 100.0 * totalHitRate)
        backingCache.stats().log("main", backingCache.estimatedSize())
        hugeCacheStats.log("huge-tile", hugeCacheFinalSize)
    }

    fun onTaskLaunched(typename: String, name: String) {
        logger.info("Launched: {}", name)
        tasksByRunCount.add(typename to name)
        taskLaunches.add(typename)
    }

    fun onTaskCompleted(typename: String, name: String) {
        logger.info("Completed: {}", name)
        taskCompletions.add(typename)
        if (typename == "OutputTask" || typename == "PngCompressionTask") {
            dedupeFailures.add(typename)
        }
    }

    fun recordRetries(howMany: Long) {
        retries.add(howMany)
    }

    fun readHugeTileCache(hugeTaskCache: Cache<SemiStrongTaskCache<*>, Result<*>>) {
        hugeCacheStats = hugeTaskCache.stats()
        hugeCacheFinalSize = hugeTaskCache.estimatedSize()
    }
}
