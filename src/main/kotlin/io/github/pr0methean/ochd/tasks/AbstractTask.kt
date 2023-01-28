package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.tasks.caching.DeferredTaskCache
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart.LAZY
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

val abstractTaskLogger: Logger = LogManager.getLogger("AbstractTask")

/**
 * Unit of work that wraps its coroutine to support reuse (including under heap-constrained conditions).
 */
@Suppress("TooManyFunctions", "EqualsWithHashCodeExist", "EqualsOrHashCode")
// hashCode is cached in a lazy; equals isn't
abstract class AbstractTask<out T>(
    val name: String,
    val cache: DeferredTaskCache<@UnsafeVariance T>,
    val ctx: CoroutineContext
) : StringBuilderFormattable {
    val coroutineScope: CoroutineScope by lazy {
        CoroutineScope(ctx.plus(CoroutineName(name)))
    }

    protected val mutex: Mutex = Mutex()

    @GuardedBy("mutex")
    val directDependentTasks: MutableSet<AbstractTask<*>> = newSetFromMap(WeakHashMap())
    private suspend fun addDirectDependentTask(task: AbstractTask<*>) {
        if (mutex.withLock {
                directDependentTasks.add(task) && directDependentTasks.size >= 2 && cache.enable()
            }) {
            ImageProcessingStats.onCachingEnabled(this)
        }
    }

    /**
     * Called once a dependent task has retrieved the output, so that we can disable caching and free up heap space once
     * all dependents have done so.
     */
    suspend fun removeDirectDependentTask(task: AbstractTask<*>) {
        if (mutex.withLock {
                directDependentTasks.remove(task)
                directDependentTasks.isEmpty()
            }) {
            directDependencies.forEach { it.removeDirectDependentTask(this) }
            if (cache.disable()) {
                ImageProcessingStats.onCachingDisabled(this)
            }
        }
    }

    val totalSubtasks: Int by lazy {
        var total = 1
        for (task in directDependencies) {
            total += task.totalSubtasks
        }
        total
    }
    abstract val directDependencies: Iterable<AbstractTask<*>>
    private val hashCode: Int by lazy {
        abstractTaskLogger.debug("Computing hash code for {}", name)
        computeHashCode()
    }

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

    suspend fun cacheClearingCoefficient(): Double {
        var coefficient = if (!cache.isEnabled()) {
            0.0
        } else if (isStartedOrAvailable()) {
            // If this task has only one direct-dependent task that's yet to start, it's worth 1 point because
            // finishing it will clear a cache entry. If it has 2 direct-dependent tasks, it's worth 1/4 point;
            // if it has 3, it's worth 1/16 point, and so on.
            Math.scalb(1.0,
                    (2 - 2 * mutex.withLock { directDependentTasks.count { !it.isStartedOrAvailable() } })
                    .coerceAtLeast(java.lang.Double.MIN_EXPONENT))
        } else 0.0
        for (task in directDependencies) {
            coefficient += task.cacheClearingCoefficient()
        }
        return coefficient
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
    fun start(): Deferred<T> = cache.computeIfAbsent {
        abstractTaskLogger.debug("Creating a new coroutine for {}", name)
        coroutineScope.async(start = LAZY) { perform() }
    }.apply(Deferred<T>::start)

    abstract suspend fun perform(): T

    @Suppress("UNCHECKED_CAST", "DeferredResultUnused")
    open fun mergeWithDuplicate(other: AbstractTask<*>): AbstractTask<T> {
        abstractTaskLogger.debug("Merging {} with duplicate {}", name, other.name)
        if (other !== this && getNow() == null) {
            val otherCoroutine = other.cache.getNowAsync()
            if (otherCoroutine != null) {
                cache.computeIfAbsent { otherCoroutine as Deferred<T> }
            }
        }
        abstractTaskLogger.debug("Done merging {} with duplicate {}", name, other.name)
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
