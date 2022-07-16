package io.github.pr0methean.ochd.packedimage

import io.github.pr0methean.ochd.ImageProcessingContext
import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import javafx.scene.paint.Color
import kotlinx.coroutines.async

const val TILE_COLUMNS = 32
const val TILE_ROWS = 32
class TilePackedImage(source: Image, val ctx: ImageProcessingContext) : PackedImage {
    private val tiles = ctx.scope.async {
        val columnWidth = (source.width / TILE_COLUMNS).toInt()
        val rowHeight = (source.height / TILE_ROWS).toInt()
        val pixelReader = source.pixelReader
        return@async Array(TILE_COLUMNS) { y -> Array(TILE_ROWS) { x ->
            val top = rowHeight * y
            val left = columnWidth * x
            val firstPixelColor = pixelReader.getArgb(left, top)
            for (py in top until top + rowHeight) {
                for (px in left until left + columnWidth) {
                    if (pixelReader.getArgb(px, py) != firstPixelColor) {
                        val imageChunk = WritableImage(pixelReader, left, top, columnWidth, rowHeight)
                        return@Array UncompressedTile(imageChunk, ctx)
                    }
                }
            }
            return@Array SolidTile(pixelReader.getColor(left, top), columnWidth, rowHeight, ctx)
        }}
    }

    override suspend fun unpacked(): Image {
        TODO("Not yet implemented")
    }

    override suspend fun packed(): ByteArray {
        TODO("Not yet implemented")
    }

    override fun isAlreadyUnpacked(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isAlreadyPacked(): Boolean {
        TODO("Not yet implemented")
    }
}