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
import org.apache.logging.log4j.util.Unbox.box
import java.lang.management.ManagementFactory
import java.lang.management.ThreadInfo
import java.lang.management.ThreadMXBean
import java.util.concurrent.ConcurrentHashMap
import kotlin.system.exitProcess
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

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
var monitoringJob: Job? = null
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
                val threadMxBean: ThreadMXBean = ManagementFactory.getThreadMXBean()
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
    private val tasksByRunCount = ConcurrentHashMultiset.create<String>()
    private val cacheableTasks = ConcurrentHashMap.newKeySet<String>()
    private val cachedTasks = ConcurrentHashMap.newKeySet<String>()

    init {
        dedupeFailures.add("Build task graph")
    }

    @Suppress("UnstableApiUsage")
    fun finalChecks() {
        if (taskLaunches != taskCompletions || dedupeFailures != taskCompletions) {
            logger.error("A task has failed to run or run more than once!")
            logger.info("")
            logger.info("Task launches:")
            taskLaunches.log()
            logger.info("")
            logger.info("Task completions:")
            taskCompletions.log()
            logger.info("")
            logger.info("Necessary tasks:")
            dedupeFailures.log()
            exitProcess(1)
        }
        logger.info("")
        logger.info("Total task counts:")
        dedupeFailures.log()
        logger.info("")
        logger.info("Deduplicated tasks:")
        dedupeSuccesses.log()
        logger.info("")
        logger.info("Tasks that would run with unlimited cache but no deduplication:")
        Multisets.sum(dedupeFailures, dedupeSuccesses).log()
    }

    fun onTaskLaunched(typeName: String, name: String) {
        logger.info("Launched: {}: {}", typeName, name)
        if (!tasksByRunCount.add(name)) {
            logger.fatal("Task {} launched twice!", name)
            exitProcess(1)
        }
        taskLaunches.add(typeName)
    }

    fun onTaskCompleted(typeName: String, name: String) {
        logger.info("Completed: {}: {}", typeName, name)
        taskCompletions.add(typeName)
    }

    fun onDedupeFailed(typename: String, name: String) {
        dedupeFailures.add(typename)
        dedupeFailuresByName.add(typename to name)
    }

    fun onCachingEnabled(task: AbstractTask<*>) {
        logger.info("Enabled caching for: {}: {}", task::class.simpleName, task.name)
        cacheableTasks.add(task.name)
        logger.info("Currently cacheable tasks: {}", box(cacheableTasks.size))
    }

    fun onCachingDisabled(task: AbstractTask<*>) {
        cacheableTasks.remove(task.name)
        cachedTasks.remove(task.name)
        logger.info("Removed {} from cache. Cached tasks: {}. Cacheable tasks: {}",
            task.name, box(cachedTasks.size), box(cacheableTasks.size))
    }

    fun onCache(task: AbstractTask<*>) {
        cachedTasks.add(task.name)
        logger.info("Added {} to cache. Cached tasks: {}. Cacheable tasks: {}",
            task.name, box(cachedTasks.size), box(cacheableTasks.size))
    }

    fun cachedTasks(): Int = cachedTasks.size
}
