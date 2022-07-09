package io.github.pr0methean.ochd

import java.lang.ref.SoftReference
import kotlin.reflect.KProperty

class SoftLazy<T>(
    initialValue: T? = null,
    val supplier: () -> T
) {
    @Volatile
    var currentValue: SoftReference<T> = SoftReference<T>(initialValue)
    operator fun getValue(thisRef: Any, property: KProperty<*>): T = currentValue.get() ?: synchronized(this) {
        currentValue.get() ?: supplier().also { currentValue = SoftReference(it) }
    }
}