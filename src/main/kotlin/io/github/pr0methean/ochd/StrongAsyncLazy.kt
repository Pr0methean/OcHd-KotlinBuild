package io.github.pr0methean.ochd

class StrongAsyncLazy<T>(
    initialValue: T? = null,
    supplier: suspend () -> T
): AsyncLazy<T>() {
    @Volatile var supplier: (suspend () -> T)? = supplier
    @Volatile var result: T? = initialValue
    override suspend fun getFromSupplier(): T {
        val result = supplier!!()
        supplier = null
        return result
    }

    override fun getNow(): T? {
        return result
    }

    override fun set(value: T?) {
        result = value
    }
}