package io.github.pr0methean.ochd.texturebase

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.tasks.OutputTask
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.map
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

@OptIn(ExperimentalTime::class)
val CLASS_LOADING_DELAY = 1.seconds
open class MaterialGroup(val elements: Flow<Material>): Material {
    constructor(vararg elements: Material): this(elements.asSequence().asFlow())

    @OptIn(FlowPreview::class)
    override fun outputTasks(ctx: ImageProcessingContext): Flow<OutputTask>
            = elements.map { it.outputTasks(ctx) }.flattenMerge()
}

@Suppress("UNCHECKED_CAST")
inline fun <reified E : Enum<out Material>> group(): MaterialGroup {
    Class.forName(E::class.java.name, true, ClassLoader.getSystemClassLoader())
    return MaterialGroup(E::class.java.enumConstants.asFlow() as Flow<Material>)
}
