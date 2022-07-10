package io.github.pr0methean.ochd.texturebase

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.tasks.OutputTask

open class MaterialGroup(val elements: Iterable<Material>): Material {
    constructor(vararg elements: Material): this(elements.toList())
    override fun outputTasks(ctx: ImageProcessingContext): Sequence<OutputTask>
            = elements.asSequence().flatMap {it.outputTasks(ctx)}
}

@Suppress("UNCHECKED_CAST")
inline fun <reified E : Enum<out Material>> group()
        = MaterialGroup(E::class.java.enumConstants.toList() as List<Material>)
