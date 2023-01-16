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
import javax.annotation.concurrent.GuardedBy
import kotlin.coroutines.CoroutineContext
import kotlin.system.exitProcess

val AT_LOGGER: Logger = LogManager.getLogger("AbstractTask")
private val SUPERVISOR_JOB = SupervisorJob()

/**
 * Unit of work that wraps its coroutine to support reuse (including under heap-constrained conditions).
 */
@Suppress("EqualsWithHashCodeExist", "EqualsOrHashCode") // hashCode is cached in a lazy; equals isn't
abstract class AbstractTask<out T>(
    val name: String,
    val cache: DeferredTaskCache<@UnsafeVariance T>,
    val ctx: CoroutineContext
) : StringBuilderFormattable {
    val coroutineScope: CoroutineScope by lazy {
        CoroutineScope(ctx.plus(CoroutineName(name)).plus(SUPERVISOR_JOB))
    }

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
    private val hashCode: Int by lazy(::computeHashCode)

    protected abstract fun computeHashCode(): Int

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
        AT_LOGGER.debug("Creating a new coroutine for {}", name)
        coroutineScope.async(start = LAZY) {
            try {
                return@async perform()
            } catch (t: Throwable) {
                AT_LOGGER.fatal("{} failed due to {}: {}", name, t::class.simpleName, t.message)
                exitProcess(1)
            }
        }
    }.apply(Deferred<T>::start)

    abstract suspend fun perform(): T

    @Suppress("UNCHECKED_CAST", "DeferredResultUnused")
    open fun mergeWithDuplicate(other: AbstractTask<*>): AbstractTask<T> {
        AT_LOGGER.debug("Merging {} with duplicate {}", name, other.name)
        if (other !== this && getNow() == null) {
            val otherCoroutine = other.cache.getNowAsync()
            if (otherCoroutine != null) {
                cache.computeIfAbsent { otherCoroutine as Deferred<T> }
            }
        }
        AT_LOGGER.debug("Done merging {} with duplicate {}", name, other.name)
        return this
    }

    fun isStartedOrAvailable(): Boolean = cache.getNowAsync()?.run { isActive || isCompleted } ?: false

    fun cacheableSubtasks(): Int {
        var subtasks = if (cache.isEnabled()) 1 else 0
        for (task in directDependencies) {
            subtasks += task.cacheableSubtasks()
        }
        return subtasks
    }

    suspend inline fun await(): T = start().await()
    override fun hashCode(): Int = hashCode
}
