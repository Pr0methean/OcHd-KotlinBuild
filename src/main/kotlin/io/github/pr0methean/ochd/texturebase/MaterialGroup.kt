package io.github.pr0methean.ochd.texturebase

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.tasks.OutputTask
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.map

open class MaterialGroup(val elements: Flow<Material>): Material {
    constructor(vararg elements: Material): this(elements.asFlow())

    @OptIn(FlowPreview::class)
    override fun outputTasks(ctx: ImageProcessingContext): Flow<OutputTask>
            = elements.map { it.outputTasks(ctx) }.flattenConcat()
}

@Suppress("UNCHECKED_CAST")
inline fun <reified E : Enum<out Material>> group()
        = MaterialGroup(E::class.java.enumConstants.asFlow() as Flow<Material>)
