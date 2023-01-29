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
        mutex.withLock {
            if (directDependentTasks.remove(task)) {
                abstractTaskLogger.info("Removed dependency of {} on {}", task.name, name)
            }
            if (directDependentTasks.isEmpty()) {
                directDependencies.forEach { it.removeDirectDependentTask(this) }
                if (cache.disable()) {
                    ImageProcessingStats.onCachingDisabled(this)
                }
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

    /**
     * If this task has only one direct-dependent task that's yet to start, we're worth 1 point to our direct and
     * transitive dependencies of that task because finishing that task will cause us to disable our cache.
     * If it has 2 direct-dependent tasks left to start, it's worth 1/4 point; if it has 3, it's worth 1/16 point,
     * and so on. Tasks with the highest cache-clearing coefficient are launched first when we're not immediately low
     * on memory.
     */
    suspend fun cacheClearingCoefficient(): Double {
        var coefficient = if (!cache.isEnabled()) {
            0.0
        } else if (isStartedOrAvailable()) {
            (1.0 / mutex.withLock { directDependentTasks.count { !it.isStartedOrAvailable() } })
                    .coerceAtLeast(java.lang.Double.MIN_NORMAL)
        } else 0.0
        for (task in directDependencies) {
            coefficient += task.cacheClearingCoefficient()
        }
        return coefficient
    }

    /**
     * True when this task should prepare to output an Image so that that Image can be cached, rather than rendering
     * onto a consuming task's canvas.
     */
    protected suspend fun shouldRenderForCaching(): Boolean = isStartedOrAvailable()
            || (cache.isEnabled() && mutex.withLock { directDependentTasks.size } > 1)
            || isStartedOrAvailable()

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

    /**
     * Number of new entries that will be added to the cache if this task runs now, minus the number that
     * will be removed. Used to prioritize tasks when memory is low.
     */
    suspend fun netAddedToCache(): Int {
        var netAdded = if (!cache.isEnabled()) {
            0
        } else if (isStartedOrAvailable()) {
            if (mutex.withLock { directDependentTasks.size == 1 }) -1 else 0
        } else if (mutex.withLock { directDependentTasks.size >= 2 }) {
            1
        } else 0
        for (task in directDependencies) {
            netAdded += task.netAddedToCache()
        }
        return netAdded
    }

    fun isCacheAllocationFreeOnMargin(): Boolean {
        if (isStartedOrAvailable() || !cache.isEnabled()) {
            return true
        }
        return directDependencies.all(AbstractTask<*>::isCacheAllocationFreeOnMargin)
    }

    suspend inline fun await(): T = start().await()
    override fun hashCode(): Int = hashCode
}
