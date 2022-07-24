package io.github.pr0methean.ochd

import java.lang.ref.SoftReference

class SoftAsyncLazy<T>(
    initialValue: T? = null,
    val supplier: suspend () -> T
) : AsyncLazy<T>() {
    @Volatile
    private var currentValue: SoftReference<T?> = SoftReference<T?>(initialValue)
    override suspend fun getFromSupplierAndStore(): T {
        val result = supplier()
        set(result)
        return result
    }

    override fun getNow(): T? = currentValue.get()
    override fun set(value: T?) {
        currentValue = SoftReference(value)
    }
}