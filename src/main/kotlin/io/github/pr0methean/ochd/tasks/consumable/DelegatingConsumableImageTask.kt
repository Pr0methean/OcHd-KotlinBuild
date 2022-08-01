package io.github.pr0methean.ochd.tasks.consumable

class DelegatingConsumableImageTask(private val original: ConsumableImageTask): ConsumableImageTask by original {
    override val name = "Delegating copy of $original"
}