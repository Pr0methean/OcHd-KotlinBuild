package io.github.pr0methean.ochd.tasks.caching

import org.apache.logging.log4j.LogManager

private val logger = LogManager.getLogger("AbstractTaskCache")
abstract class AbstractTaskCache<T>(val name: String) : TaskCache<T> {
    @Volatile override var enabled: Boolean = false
        set(value) {
            if (!value) {
                enabledSet(null)
            }
            field = value
        }

    override fun set(value: Result<T>?) {
        if (enabled) {
            logger.info("Caching result of {}", name)
            enabledSet(value)
        }
    }

    abstract fun enabledSet(value: Result<T>?)
}