package io.github.pr0methean.ochd.tasks.caching

/**
 * If a task uses this, results will not be stored. Any requests for the result of the task will
 * trigger a new computation if there isn't already one in progress.
 */
object NoopTaskCache: TaskCache<Any> {
    override var enabled: Boolean
        get() = false
        set(_) {/* No-op. */}

    override fun getNow(): Result<Nothing>? = null

    override fun set(value: Result<Any>?) {
        // No-op.
    }
}

@Suppress("UNCHECKED_CAST")
fun <T> noopTaskCache(): TaskCache<T> = NoopTaskCache as TaskCache<T>