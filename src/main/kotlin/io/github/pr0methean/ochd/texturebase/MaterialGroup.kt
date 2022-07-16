package io.github.pr0methean.ochd.texturebase

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.tasks.OutputTask
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

@OptIn(FlowPreview::class)
open class MaterialGroup(val elements: Flow<Material>, val concurrency: Int = DEFAULT_CONCURRENCY): Material {
    constructor(concurrency: Int, vararg elements: Material): this(elements.asFlow(), concurrency)

    constructor(vararg elements: Material): this(elements.asFlow())

    @OptIn(FlowPreview::class)
    override fun outputTasks(ctx: ImageProcessingContext): Flow<OutputTask>
            = elements.map { it.outputTasks(ctx) }.flattenMerge(concurrency)
}

@OptIn(FlowPreview::class)
@Suppress("UNCHECKED_CAST")
inline fun <reified E : Enum<out Material>> group(concurrency: Int = DEFAULT_CONCURRENCY)
        = MaterialGroup(E::class.java.enumConstants.asFlow() as Flow<Material>, concurrency)
