package com.music.mavimusic.ui.graphics

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import coil3.Image
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap

actual fun ImageRequest.Builder.forPixelAccess(): ImageRequest.Builder =
    allowHardware(false)

actual fun Image.toImageBitmap(): ImageBitmap =
    toBitmap().asImageBitmap()

actual fun imageBitmapOf(pixels: IntArray, width: Int, height: Int): ImageBitmap {
    val bitmap = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    return bitmap.asImageBitmap()
}

actual fun paletteSwatches(image: ImageBitmap, maxColors: Int, clearFilters: Boolean): List<Swatch> =
    PaletteQuantizer.swatches(image, maxColors, clearFilters)
