package io.github.pr0methean.ochd.materials.block.shears

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.texturebase.Block
import io.github.pr0methean.ochd.texturebase.SingleTextureMaterial
import javafx.scene.paint.Color

@Suppress("unused")
enum class SimpleShearBlock: SingleTextureMaterial, Block {
    COBWEB {
        override suspend fun LayerListBuilder.createTextureLayers() {
            /*
            push ringsCentralBullseye ${white} cobweb1
push x ${white} cobweb2
push cross ${white} cobweb3
out_stack block/cobweb
             */
            layer("ringsCentralBullseye", Color.WHITE, 0.75)
            layer("x", Color.WHITE, 0.85)
            layer("cross", Color.WHITE, 0.85)
        }
    };
}