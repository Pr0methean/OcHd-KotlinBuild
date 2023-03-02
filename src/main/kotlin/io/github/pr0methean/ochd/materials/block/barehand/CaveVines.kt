package io.github.pr0methean.ochd.materials.block.barehand

import io.github.pr0methean.ochd.OutputTaskBuilder
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.texturebase.ShadowHighlightMaterial
import javafx.scene.paint.Color
import javafx.scene.paint.Paint

val berryHighlight: Color = c(0xffff6b)
val berryColor: Color = c(0xff8931)
object CaveVines: ShadowHighlightMaterial {
    override val color: Paint = c(0x507233)
    override val shadow: Paint = c(0x4f3200)
    override val highlight: Paint = c(0x70922d)
    override suspend fun OutputTaskBuilder.outputTasks() {
        val vinePlantTask = stack {
            layer("wavyVines", shadow)
            layer("waves", highlight)
        }
        val vineTask = stack {
            layer("wavyVinesBottom", shadow)
            layer("wavesBottom", highlight)
        }
        val berryTask = stack {
            layer("vineBerries", berryColor)
            layer("vineBerriesHighlight", berryHighlight)
        }
        out("block/cave_vines_plant", vinePlantTask)
        out("block/cave_vines_plant_lit") {
            copy(vinePlantTask)
            copy(berryTask)
        }
        out("block/cave_vines", vineTask)
        out("block/cave_vines_lit") {
            copy(vineTask)
            copy(berryTask)
        }
    }
}
