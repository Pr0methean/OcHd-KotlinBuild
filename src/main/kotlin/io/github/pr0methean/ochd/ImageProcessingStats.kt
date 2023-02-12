package io.github.pr0methean.ochd

import com.google.common.collect.ConcurrentHashMultiset
import com.google.common.collect.HashMultiset
import com.google.common.collect.Multiset
import com.google.common.collect.Multisets
import io.github.pr0methean.ochd.tasks.AbstractTask
import io.github.pr0methean.ochd.tasks.InvalidTask
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.debug.DebugProbes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.io.IoBuilder
import org.apache.logging.log4j.util.StringBuilderFormattable
import org.apache.logging.log4j.util.Unbox.box
import java.lang.management.ManagementFactory
import java.lang.management.ThreadInfo
import java.lang.management.ThreadMXBean
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.StampedLock
import javax.annotation.concurrent.GuardedBy
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private fun Multiset<*>.log() {
    var total = 0L
    toSet().forEach {
        if (it.toString() != InvalidTask::class.simpleName) {
            val count = count(it)
            total += count
            logger.info("{}: {}", it, box(count))
        }
    }
    logger.info("Total: {}", box(total))
}

private val logger = LogManager.getLogger("ImageProcessingStats")
private const val NEED_THREAD_MONITORING = false
private const val MAX_STACK_DEPTH = 20
private val needCoroutineDebug = logger.isDebugEnabled
private val reportingInterval: Duration = 1.minutes
val threadMxBean: ThreadMXBean = ManagementFactory.getThreadMXBean()
var monitoringJob: Job? = null
private val cacheLock = StampedLock()
private val cacheLoggingMinIntervalNanos = 2.seconds.inWholeNanoseconds
@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("DeferredResultUnused")
fun startMonitoring(scope: CoroutineScope) {
    if (needCoroutineDebug) {
        DebugProbes.install()
    }
    val loggerStream = IoBuilder.forLogger(logger).setLevel(Level.DEBUG).buildPrintStream()
    monitoringJob = scope.launch(CoroutineName("Stats monitoring job")) {
        while (true) {
            delay(reportingInterval)
            logger.info("Completed tasks:")
            ImageProcessingStats.taskCompletions.log()
            if (NEED_THREAD_MONITORING) {
                val deadlocks = threadMxBean.findDeadlockedThreads()
                if (deadlocks == null) {
                    logger.info("No deadlocked threads found.")
                    threadMxBean.allThreadIds.map { it to threadMxBean.getThreadInfo(it, MAX_STACK_DEPTH) }
                        .forEach { (id, threadInfo) ->
                            logThread(Level.INFO, id, threadInfo)
                        }
                } else {
                    logger.error("Deadlocked threads found!")
                    deadlocks.map {
                        it to threadMxBean.getThreadInfo(it, MAX_STACK_DEPTH)
                    }.forEach { (id, threadInfo) ->
                        logThread(Level.ERROR, id, threadInfo)
                    }
                }
            }
            if (needCoroutineDebug) {
                DebugProbes.dumpCoroutines(loggerStream)
            }
        }
    }
}

fun logThread(logLevel: Level, id: Long, threadInfo: ThreadInfo) {
    logger.log(logLevel, "Thread: {} (id: {})", threadInfo.threadName, box(id))
    threadInfo.stackTrace.forEach {logger.log(logLevel, "{}.{} ({} line {})",
        it.className, it.methodName, it.fileName, box(it.lineNumber))}
}

fun stopMonitoring() {
    monitoringJob?.cancel("Monitoring stopped")
}

object ImageProcessingStats {
    private val taskLaunches: ConcurrentHashMultiset<String> = ConcurrentHashMultiset.create()
    val taskCompletions: ConcurrentHashMultiset<String> = ConcurrentHashMultiset.create()
    val dedupeSuccesses: HashMultiset<String> = HashMultiset.create()
    private val dedupeFailures: HashMultiset<String> = HashMultiset.create()
    private val dedupeFailuresByName = HashMultiset.create<Pair<String, String>>()
    private val tasksByRunCount = ConcurrentHashMultiset.create<Pair<String, String>>()
    private val cacheableTasks = ConcurrentHashMap.newKeySet<AbstractTask<*>>()

    @GuardedBy("cacheLock")
    var lastCacheLogNanoTime: Long = System.nanoTime()

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
        logger.info("Tasks repeated due to cache misses:")
        val repeatedTasks = Multisets.copyHighestCountFirst(tasksByRunCount)
        repeatedTasks.toSet().forEach {
            val count = repeatedTasks.count(it)
            if (count >= 2) {
                val (typeName, name) = it
                logger.info("{}: {}: {}", typeName, name, box(count))
            }
        }
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
            logger.printf(Level.INFO, "%20s: %3.2f%% / %3.2f%%", className,
                    100.0 * efficiency, 100.0 * hitRate)
            totalUnique += unique
            totalActual += actual
            totalWorstCase += worstCase
        }
        dedupeFailuresByName.toSet().forEach {
            if (!tasksByRunCount.contains(it)) {
                val (typeName, name) = it
                if (typeName != InvalidTask::class.simpleName) {
                    logger.warn("Task in graph not launched: {}: {}", typeName, name)
                }
            }
        }
        val totalEfficiency = (totalUnique.toDouble() / totalActual)
        val totalHitRate = 1.0 - (totalActual - totalUnique).toDouble()/(totalWorstCase - totalUnique)
        logger.printf(Level.INFO, "Total               : %3.2f%% / %3.2f%%",
            100.0 * totalEfficiency, 100.0 * totalHitRate)
    }

    fun onTaskLaunched(typeName: String, name: String) {
        logger.info("Launched: {}: {}", typeName, name)
        tasksByRunCount.add(typeName to name)
        taskLaunches.add(typeName)
    }

    fun onTaskCompleted(typeName: String, name: String) {
        logger.info("Completed: {}: {}", typeName, name)
        taskCompletions.add(typeName)
        if (typeName != "PngOutputTask") {
            logCurrentlyCachedTasks()
        }
    }

    fun onDedupeFailed(typename: String, name: String) {
        dedupeFailures.add(typename)
        dedupeFailuresByName.add(typename to name)
    }

    fun onCachingEnabled(task: AbstractTask<*>) {
        logger.info("Enabled caching for: {}: {}", task::class.simpleName, task.name)
        cacheableTasks.add(task)
        logger.info("Currently cacheable tasks: {}", box(cacheableTasks.size))
    }

    fun onCachingDisabled(task: AbstractTask<*>) {
        logger.info("Disabled caching for: {}: {}", task::class.simpleName, task.name)
        cacheableTasks.remove(task)
        logger.info("Currently cacheable tasks: {}", box(cacheableTasks.size))
        logCurrentlyCachedTasks()
    }

    private fun logCurrentlyCachedTasks() {
        // This is a read operation, but we don't want threads redundantly running it in parallel.
        val stamp = cacheLock.tryWriteLock()
        if (stamp != 0L) {
            try {
                val now = System.nanoTime()
                if (now - lastCacheLogNanoTime < cacheLoggingMinIntervalNanos) {
                    return
                }
                lastCacheLogNanoTime = now
                val cachedTasks = cacheableTasks.filter { it.cache.isEnabled() && it.getNow() != null }
                        .sortedBy(AbstractTask<*>::name)
                logger.info("Currently cached tasks: {}: {}", box(cachedTasks.size), StringBuilderFormattable {
                    it.appendCollection(cachedTasks, "; ")
                })
            } finally {
                cacheLock.unlock(stamp)
            }
        }
    }
}
