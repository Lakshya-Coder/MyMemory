package com.lakshyagupta7089.mymemory.utils

import android.graphics.Bitmap

object BitmapScaler {
    fun scaleToFitWidth(b: Bitmap, width: Int): Bitmap {
        val factory = width / b.width.toFloat()

        return Bitmap.createScaledBitmap(
            b,
            width,
            (b.height * factory).toInt(),
            true
        )
    }

    fun scaleToFitHeight(b: Bitmap, height: Int): Bitmap {
        val factory = height / b.height.toFloat()

        return Bitmap.createScaledBitmap(
            b,
            (b.width * factory).toInt(),
            height,
            true
        )
    }
}
