package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.tasks.caching.DeferredTaskCache
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.util.Collections.newSetFromMap
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.annotation.concurrent.GuardedBy
import kotlin.coroutines.CoroutineContext

val AT_LOGGER: Logger = LogManager.getLogger("AbstractTask")
private val SUPERVISOR_JOB = SupervisorJob()
abstract class AbstractTask<T>(
    final override val name: String,
    val cache: DeferredTaskCache<T>,
    val ctx: CoroutineContext
) : Task<T> {
    protected val coroutineScope: CoroutineScope by lazy {
        CoroutineScope(ctx.plus(CoroutineName(name)).plus(SUPERVISOR_JOB))
    }

    val timesFailed: AtomicLong = AtomicLong(0)
    private val mutex: Mutex = Mutex()
    @GuardedBy("mutex")
    val directDependentTasks: MutableSet<Task<*>> = newSetFromMap(WeakHashMap())
    override suspend fun addDirectDependentTask(task: Task<*>) {
        if (mutex.withLock {
                directDependentTasks.add(task) && directDependentTasks.size >= 2 && cache.enable()
        }) {
            AT_LOGGER.info("Enabling caching for {}", name)
        }
    }

    override suspend fun removeDirectDependentTask(task: Task<*>) {
        if (mutex.withLock {
                directDependentTasks.remove(task) && directDependentTasks.isEmpty() && cache.disable()
        }) {
            AT_LOGGER.info("Disabled caching for {}", name)
        }
    }

    override val totalSubtasks: Int by lazy {
        var total = 1
        for (task in directDependencies) {
            total += task.totalSubtasks
        }
        total
    }

    override fun startedOrAvailableSubtasks(): Int {
        if (isStartedOrAvailable()) {
            return totalSubtasks
        }
        var subtasks = 0
        for (task in directDependencies) {
            subtasks += task.startedOrAvailableSubtasks()
        }
        return subtasks
    }

    override suspend fun registerRecursiveDependencies(): Unit = mutex.withLock {
        directDependencies.forEach {
            it.addDirectDependentTask(this@AbstractTask)
            it.registerRecursiveDependencies()
        }
    }

    final override fun toString(): String = name

    override fun formatTo(buffer: StringBuilder) {
        buffer.append(name)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getNow(): T? {
        val coroutine = cache.getNowAsync() ?: return null
        return if (coroutine.isCompleted) {
            coroutine.getCompleted()
        } else null
    }
    override suspend fun await(): T = cache.computeIfAbsent { createCoroutineAsync() }.await()

    protected abstract fun createCoroutineAsync(): Deferred<T>

    fun logFailure(throwable: Throwable) {
        AT_LOGGER.error("{} failed due to {}: {}", name, throwable::class.simpleName, throwable.message)
        timesFailed.getAndIncrement()
    }

    @Suppress("UNCHECKED_CAST", "ReturnCount")
    override suspend fun mergeWithDuplicate(other: Task<*>): Task<T> {
        return if (other === this || getNow() != null) {
            this
        } else {
            if (other is AbstractTask) {
                val otherCoroutine = other.cache.getNowAsync()
                if (otherCoroutine != null
                        && cache.computeIfAbsent { otherCoroutine as Deferred<T> } == otherCoroutine) {
                    return this
                }
            }
            return this
        }
    }

    override fun isStartedOrAvailable(): Boolean = cache.getNowAsync()?.run { isActive || isCompleted } ?: false

    override fun timesFailed(): Long = timesFailed.get()
    override fun cacheableSubtasks(): Int {
        var subtasks = if (cache.isEnabled()) 1 else 0
        for (task in directDependencies) {
            subtasks += task.cacheableSubtasks()
        }
        return subtasks
    }

    override fun clearCache() {
        cache.clear()
        directDependencies.forEach {
            try {
                it.getNow()
            } catch (t: Throwable) {
                it.clearCache()
            }
        }
    }
}
