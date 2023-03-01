package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.TaskPlanningContext
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.materials.block.pickaxe.Ore.GOLD
import io.github.pr0methean.ochd.tasks.PngOutputTask
import io.github.pr0methean.ochd.texturebase.ShadowHighlightMaterial
import javafx.scene.paint.Color
import javafx.scene.paint.Paint

@Suppress("unused")
enum class Polishable(
    override val color: Paint,
    override val shadow: Paint,
    override val highlight: Paint
): ShadowHighlightMaterial {
    ANDESITE(c(0x8b8b8b),c(0x737373),c(0xaaaaaa)) {
        override fun LayerListBuilder.createTextureLayersBase() {
            background(color)
            layer("bigRingsBottomLeftTopRight", highlight)
            layer("bigRingsTopLeftBottomRight", shadow)
        }
    },
    DIORITE(c(0xbfbfbf),c(0x888888), Color.WHITE) {
        override fun LayerListBuilder.createTextureLayersBase() {
            background(color)
            layer("bigRingsBottomLeftTopRight", shadow)
            layer("bigRingsTopLeftBottomRight", highlight)
        }
    },
    GRANITE(c(0x9f6b58),c(0x624033),c(0xFFCDB2)) {
        override fun LayerListBuilder.createTextureLayersBase() {
            background(color)
            layer("bigDotsBottomLeftTopRight", highlight)
            layer("bigDotsTopLeftBottomRight", shadow)
            layer("bigRingsBottomLeftTopRight", shadow)
            layer("bigRingsTopLeftBottomRight", highlight)
        }
    },
    BLACKSTONE(c(0x2e2e36), Color.BLACK,c(0x515151)) {
        override fun LayerListBuilder.createTextureLayersBase() {
            background(shadow)
            layer("bigDotsBottomLeftTopRight", highlight)
            layer("bigDotsTopLeftBottomRight", color)
        }
        override fun outputTasks(ctx: TaskPlanningContext): Sequence<PngOutputTask> = sequence {
            val base = ctx.stack { createTextureLayersBase() }
            yield(ctx.out("block/blackstone", base))
            val polishedTextureTask = ctx.stack {
                copy(base)
                createBorderPolished()
            }
            yield(ctx.out("block/polished_blackstone", polishedTextureTask))
            yield(ctx.out("block/gilded_blackstone") {
                copy(polishedTextureTask)
                layer("bigRingsBottomLeftTopRight", GOLD.color)
            })
            yield(ctx.out("block/blackstone_top") {
                background(shadow)
                layer("bigRingsBottomLeftTopRight", color)
                layer("bigRingsTopLeftBottomRight", highlight)
            })
        }
    };

    abstract fun LayerListBuilder.createTextureLayersBase()
    protected fun LayerListBuilder.createBorderPolished() {
        layer("borderSolid", shadow)
        layer("borderSolidTopLeft", highlight)
    }

    override fun outputTasks(ctx: TaskPlanningContext): Sequence<PngOutputTask> = sequence {
        val base = ctx.stack { createTextureLayersBase() }
        yield(ctx.out("block/$name", base))
        yield(ctx.out("block/polished_$name") {
            copy(base)
            createBorderPolished()
        })
    }
}
