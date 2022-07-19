package io.github.pr0methean.ochd.texturebase

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.tasks.OutputTask
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

@OptIn(FlowPreview::class)
open class MaterialGroup(val elements: Flow<Material>): Material {
    constructor(vararg elements: Material): this(elements.asFlow())

    @OptIn(FlowPreview::class)
    override fun outputTasks(ctx: ImageProcessingContext): Flow<OutputTask>
            = elements.flatMapConcat { it.outputTasks(ctx) }
}

inline fun <reified E> group(): MaterialGroup where E : Material, E : Enum<E> =
    MaterialGroup(enumValues<E>().asFlow())
