package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.tasks.caching.DeferredTaskCache
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart.LAZY
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.apache.logging.log4j.util.StringBuilderFormattable
import java.util.Collections.newSetFromMap
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.annotation.concurrent.GuardedBy
import kotlin.coroutines.CoroutineContext

val AT_LOGGER: Logger = LogManager.getLogger("AbstractTask")
private val SUPERVISOR_JOB = SupervisorJob()

/**
 * Unit of work that wraps its coroutine to support reuse (including under heap-constrained conditions) and retrying.
 */
abstract class AbstractTask<out T>(
    val name: String,
    val cache: DeferredTaskCache<@UnsafeVariance T>,
    val ctx: CoroutineContext
) : StringBuilderFormattable {
    val coroutineScope: CoroutineScope by lazy {
        CoroutineScope(ctx.plus(CoroutineName(name)).plus(SUPERVISOR_JOB))
    }

    val timesFailed: AtomicLong = AtomicLong(0)
    protected val mutex: Mutex = Mutex()

    @GuardedBy("mutex")
    val directDependentTasks: MutableSet<AbstractTask<*>> = newSetFromMap(WeakHashMap())
    suspend fun addDirectDependentTask(task: AbstractTask<*>) {
        if (mutex.withLock {
                directDependentTasks.add(task) && directDependentTasks.size >= 2 && cache.enable()
            }) {
            AT_LOGGER.info("Enabling caching for {}", name)
        }
    }

    suspend fun removeDirectDependentTask(task: AbstractTask<*>) {
        if (mutex.withLock {
                directDependentTasks.remove(task)
                directDependentTasks.isEmpty()
            }) {
            directDependencies.forEach { it.removeDirectDependentTask(this) }
            if (cache.disable()) {
                AT_LOGGER.info("Disabled caching for {}", name)
            }
        }
    }

    private val totalSubtasks: Int by lazy {
        var total = 1
        for (task in directDependencies) {
            total += task.totalSubtasks
        }
        total
    }
    abstract val directDependencies: Iterable<AbstractTask<*>>

    fun startedOrAvailableSubtasks(): Int {
        if (isStartedOrAvailable()) {
            return totalSubtasks
        }
        var subtasks = 0
        for (task in directDependencies) {
            subtasks += task.startedOrAvailableSubtasks()
        }
        return subtasks
    }

    suspend fun registerRecursiveDependencies(): Unit = mutex.withLock {
        directDependencies.forEach {
            it.addDirectDependentTask(this@AbstractTask)
            it.registerRecursiveDependencies()
        }
    }

    final override fun toString(): String = name

    final override fun formatTo(buffer: StringBuilder) {
        buffer.append(name)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getNow(): T? {
        val coroutine = cache.getNowAsync() ?: return null
        return if (coroutine.isCompleted) {
            coroutine.getCompleted()
        } else null
    }

    @Suppress("DeferredIsResult")
    suspend inline fun start(): Deferred<T> = cache.computeIfAbsent {
        coroutineScope.async(start = LAZY) {
            try {
                return@async perform()
            } catch (t: Throwable) {
                AT_LOGGER.error("{} failed due to {}: {}", name, t::class.simpleName, t.message)
                timesFailed.getAndIncrement()
                throw t
            }
        }
    }.apply(Deferred<T>::start)

    abstract suspend fun perform(): T

    @Suppress("UNCHECKED_CAST", "ReturnCount", "DeferredResultUnused")
    open suspend fun mergeWithDuplicate(other: AbstractTask<*>): AbstractTask<T> {
        if (other !== this && getNow() == null) {
            val otherCoroutine = other.cache.getNowAsync()
            if (otherCoroutine != null) {
                cache.computeIfAbsent { otherCoroutine as Deferred<T> }
            }
        }
        return this
    }

    fun isStartedOrAvailable(): Boolean = cache.getNowAsync() != null

    fun timesFailed(): Long = timesFailed.get()
    fun cacheableSubtasks(): Int {
        var subtasks = if (cache.isEnabled()) 1 else 0
        for (task in directDependencies) {
            subtasks += task.cacheableSubtasks()
        }
        return subtasks
    }

    fun clearCache() {
        cache.clear()
        directDependencies.forEach {
            try {
                it.getNow()
            } catch (t: Throwable) {
                AT_LOGGER.debug("Clearing failure in subtask {} of {}", it, this, t)
                it.clearCache()
            }
        }
    }

    suspend inline fun await(): T = start().await()
}
