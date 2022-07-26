package io.github.pr0methean.ochd.packedimage

import io.github.pr0methean.ochd.ImageProcessingStats
import javafx.scene.image.Image
import kotlinx.coroutines.CoroutineScope
import java.io.ByteArrayInputStream

class BitmapPackedImage(
    initialUnpacked: Image?, initialPng: ByteArray?, name: String,
    scope: CoroutineScope, stats: ImageProcessingStats,
    packer: ImagePacker) : PackedImage(
    initialUnpacked = initialUnpacked, initialPacked = initialPng,
    name = name, scope = scope, stats = stats, packer = packer
) {

    init {
        if (initialUnpacked != null && initialUnpacked.height > MAX_UNCOMPRESSED_TILESIZE) {
            pngBytes.start(scope)
        }
    }

    override suspend fun unpack(): Image {
        stats.onDecompressPngImage(name)
        return ByteArrayInputStream(asPng()).use {Image(it)}
    }

}