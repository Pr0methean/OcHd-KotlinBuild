package io.github.pr0methean.ochd.tasks.caching

import org.apache.logging.log4j.LogManager
import java.lang.ref.WeakReference

val nullRef = WeakReference<Nothing?>(null)
private val logger = LogManager.getLogger("AbstractTaskCache")
abstract class AbstractTaskCache<T>(override val name: String) : TaskCache<T> {
    @Volatile override var enabled: Boolean = false
        set(value) {
            if (!value) {
                disable()
            }
            field = value
        }

    override fun set(value: T?) {
        if (value == null) {
            clear()
        } else if (enabled) {
            logger.info("Caching result of {}", name)
            enabledSet(value)
        }
    }

}