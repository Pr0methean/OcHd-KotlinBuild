package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.tasks.consumable.OutputTask
import io.github.pr0methean.ochd.texturebase.ShadowHighlightMaterial
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

@Suppress("unused")
enum class Polishable(
    override val color: Paint,
    override val shadow: Paint,
    override val highlight: Paint
): ShadowHighlightMaterial {
    ANDESITE(c(0x8b8b8b),c(0x686868),c(0xa9a99a)) {
        override fun LayerListBuilder.createTextureLayersBase() {
            /*push bigRingsBottomLeftTopRight ${andesite_h} a1 ${andesite}
push bigRingsTopLeftBottomRight ${andesite_s} a2*/
            background(color)
            layer("bigRingsBottomLeftTopRight", highlight)
            layer("bigRingsTopLeftBottomRight", shadow)
        }
    },
    DIORITE(c(0xbfbfbf),c(0x7b7b7b), Color.WHITE) {
        override fun LayerListBuilder.createTextureLayersBase() {
            background(color)
            layer("bigRingsBottomLeftTopRight", shadow)
            layer("bigRingsTopLeftBottomRight", highlight)
        }
    },
    GRANITE(c(0x9f6b58),c(0x5f4034),c(0xffc0af)) {
        override fun LayerListBuilder.createTextureLayersBase() {
            background(color)
            layer("bigDotsBottomLeftTopRight", highlight)
            layer("bigDotsTopLeftBottomRight", shadow)
            layer("bigRingsBottomLeftTopRight", shadow)
            layer("bigRingsTopLeftBottomRight", highlight)
        }
    },
    BLACKSTONE(c(0x312c36), Color.BLACK,c(0x4e4b54)) {
        override fun LayerListBuilder.createTextureLayersBase() {
            background(shadow)
            layer("bigDotsBottomLeftTopRight", highlight)
            layer("bigDotsTopLeftBottomRight", color)
        }
    };

    abstract fun LayerListBuilder.createTextureLayersBase()
    private fun LayerListBuilder.createBorderPolished() {
        layer("borderSolid", shadow)
        layer("borderSolidTopLeft", highlight)
    }

    override fun outputTasks(ctx: ImageProcessingContext): Flow<OutputTask> = flow {
        emit(ctx.out("block/$name", ctx.stack {createTextureLayersBase()}))
        emit(ctx.out("block/polished_$name", ctx.stack {createPolishedTexture()}))
    }

    internal fun LayerListBuilder.createPolishedTexture() {
        createTextureLayersBase()
        createBorderPolished()
    }
}