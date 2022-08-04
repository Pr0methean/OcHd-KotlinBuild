package io.github.pr0methean.ochd.materials.block.hoe

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.materials.block.pickaxe.Ore
import io.github.pr0methean.ochd.texturebase.Block
import io.github.pr0methean.ochd.texturebase.ShadowHighlightMaterial
import io.github.pr0methean.ochd.texturebase.SingleTextureMaterial
import javafx.scene.paint.Color.WHITE
import javafx.scene.paint.Paint

@Suppress("unused")
enum class SimpleHoeBlock(
    override val color: Paint,
    override val shadow: Paint,
    override val highlight: Paint
): SingleTextureMaterial, ShadowHighlightMaterial, Block {
    SHROOMLIGHT(c(0xffac6d), c(0xd75100), c(0xffffb4)) {
        override suspend fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("borderSolid", highlight)
            layer("checksSmall", shadow)
            layer("shroomlightOn", highlight)
            /*
            shroomlight_h='ffffb4'
shroomlight='ffac6d'
shroomlight_s='d75100'
            push borderSolid ${shroomlight_h} sl0 ${shroomlight}
push checksSmall ${shroomlight_s} sl1
push shroomlightOn ${shroomlight_h} sl2
out_stack block/shroomlight
             */
        }
    },
    TARGET_SIDE(c(0xffd7ba), Ore.REDSTONE.shadow, WHITE) {
        override suspend fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("grassTall", highlight)
            layer("ringsCentralBullseye", shadow)
        }
    },
    TARGET_TOP(c(0xffd7ba), Ore.REDSTONE.shadow, WHITE) {
        override suspend fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("checksSmall", highlight)
            layer("ringsCentralBullseye", shadow)
        }
    },
    /*
    push grassTall $target_h targetSide1 $target_s
push ringsCentralBullseye $redstone_s targetSide2
out_stack block/target_side

push checksSmall $target_h targetTop1 $target_s
push ringsCentralBullseye $redstone_s targetTop2
out_stack block/target_top
    target_h='ffffff'
target_s='ffd7ba'
     */
}