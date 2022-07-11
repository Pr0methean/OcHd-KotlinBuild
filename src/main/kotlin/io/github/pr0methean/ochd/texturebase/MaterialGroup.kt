package io.github.pr0methean.ochd.texturebase

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.tasks.OutputTask
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

open class MaterialGroup(val elements: Flow<Material>, val sequential: Boolean = false): Material {
    constructor(sequential: Boolean, vararg elements: Material): this(elements.asFlow(), sequential)

    constructor(vararg elements: Material): this(elements.asFlow(), false)

    @OptIn(FlowPreview::class)
    override fun outputTasks(ctx: ImageProcessingContext): Flow<OutputTask>
            = elements.map { it.outputTasks(ctx) }.flattenMerge(if (sequential) 1 else DEFAULT_CONCURRENCY)
}

@Suppress("UNCHECKED_CAST")
inline fun <reified E : Enum<out Material>> group()
        = MaterialGroup(E::class.java.enumConstants.asFlow() as Flow<Material>)
