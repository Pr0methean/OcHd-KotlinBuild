package io.github.pr0methean.ochd.tasks.caching

/**
 * If a task uses this, results will not be stored. Any requests for the result of the task will
 * trigger a new computation if there isn't already one in progress.
 */
object NoopTaskCache: TaskCache<Any> {
    override var enabled: Boolean
        get() = false
        set(_) {/* No-op. */}
    override val name: String = "NoopTaskCache"

    override fun getNow(): Nothing? = null
    override fun disable() {
        // No-op.
    }

    override fun clear() {
        // No-op.
    }

    override fun enabledSet(value: Any) {
        // No-op.
    }

    override fun set(value: Any?) {
        // No-op.
    }
}

@Suppress("UNCHECKED_CAST")
fun <T> noopTaskCache(): TaskCache<T> = NoopTaskCache as TaskCache<T>
