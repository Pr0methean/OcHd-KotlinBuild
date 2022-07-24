package io.github.pr0methean.ochd

class StrongAsyncLazy<T>(
    initialValue: T? = null,
    supplier: suspend () -> T
): AsyncLazy<T>() {
    @Volatile var supplier: (suspend () -> T)? = supplier
    @Volatile var result: T? = initialValue
    override suspend fun getFromSupplierAndStore(): T {
        val result = supplier!!()
        set(result)
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