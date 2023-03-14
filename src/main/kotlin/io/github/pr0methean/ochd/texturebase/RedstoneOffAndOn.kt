package io.github.pr0methean.ochd.texturebase

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.OutputTaskEmitter
import io.github.pr0methean.ochd.materials.block.pickaxe.Ore
import javafx.scene.paint.Color

fun OutputTaskEmitter.redstoneOffAndOn(baseName: String,
                                       layers: LayerListBuilder.(redstoneStateColor: Color) -> Unit) {
    out(baseName) { layers(Color.BLACK) }
    out(baseName + "_on") { layers(Ore.REDSTONE.highlight) }
}
