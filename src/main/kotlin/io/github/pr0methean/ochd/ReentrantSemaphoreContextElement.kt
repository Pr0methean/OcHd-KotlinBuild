package io.github.pr0methean.ochd

import kotlin.coroutines.CoroutineContext

// From https://github.com/Kotlin/kotlinx.coroutines/issues/1686#issuecomment-777357672
class ReentrantSemaphoreContextElement(
    override val key: ReentrantSemaphoreContextKey
) : CoroutineContext.Element