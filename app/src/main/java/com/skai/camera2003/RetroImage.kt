package com.skai.camera2003

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Typeface
import androidx.camera.core.ImageProxy
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Low-resolution digital sensor look, without film scratches or sepia. */
object RetroImage {
    fun fromYuv(image: ImageProxy): Bitmap {
        val step = maxOf(1, image.width / 240)
        val width = image.width / step
        val height = image.height / step
        val pixels = IntArray(width * height)
        val y = image.planes[0]
        val u = image.planes[1]
        val v = image.planes[2]
        val yBuffer = y.buffer.duplicate()
        val uBuffer = u.buffer.duplicate()
        val vBuffer = v.buffer.duplicate()
        val yStart = yBuffer.position()
        val uStart = uBuffer.position()
        val vStart = vBuffer.position()
        for (row in 0 until height) {
            val sourceY = row * step
            for (col in 0 until width) {
                val sourceX = col * step
                val yy = (yBuffer.get(yStart + sourceY * y.rowStride + sourceX * y.pixelStride).toInt() and 255) - 16
                val uu = (uBuffer.get(uStart + sourceY / 2 * u.rowStride + sourceX / 2 * u.pixelStride).toInt() and 255) - 128
                val vv = (vBuffer.get(vStart + sourceY / 2 * v.rowStride + sourceX / 2 * v.pixelStride).toInt() and 255) - 128
                val luma = 298 * maxOf(0, yy)
                pixels[row * width + col] = Color.rgb(
                    ((luma + 409 * vv + 128) shr 8).coerceIn(0, 255),
                    ((luma - 100 * uu - 208 * vv + 128) shr 8).coerceIn(0, 255),
                    ((luma + 516 * uu + 128) shr 8).coerceIn(0, 255))
            }
        }
        val raw = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
        val rotated = Bitmap.createBitmap(raw, 0, 0, width, height,
            Matrix().apply { postRotate(image.imageInfo.rotationDegrees.toFloat()) }, false)
        if (rotated !== raw) raw.recycle()
        return rotated
    }

    fun finish(source: Bitmap, outputWidth: Int, date: Date, dateStamp: Boolean = true, style: Int = 0): Bitmap {
        val outputHeight = outputWidth * 4 / 3
        val cropWidth = minOf(source.width, source.height * 3 / 4)
        val cropHeight = minOf(source.height, source.width * 4 / 3)
        val cropped = Bitmap.createBitmap(source, (source.width - cropWidth) / 2,
            (source.height - cropHeight) / 2, cropWidth, cropHeight)
        // A 240 x 320 sensor, enlarged without smoothing for VGA output.
        val small = Bitmap.createScaledBitmap(cropped, 240, 320, false)
        val pixels = IntArray(240 * 320)
        small.getPixels(pixels, 0, 240, 0, 0, 240, 320)
        for (index in pixels.indices) {
            val color = pixels[index]
            val r = Color.red(color)
            val g = Color.green(color)
            val b = Color.blue(color)
            val gray = (r * 30 + g * 59 + b * 11) / 100
            val grain = ((index * 1103515245 + 2003) ushr 24 and 15) - 7
            val noise = if (style == 0) grain else grain / 2
            fun channel(value: Int, shift: Int): Int {
                val muted = gray + (value - gray) * if (style == 0) 0.82f else 0.95f
                return ((((muted - 128) * (if (style == 0) 1.12f else 1.02f) + 128 + noise + shift).toInt()
                    .coerceIn(0, 255) / 8) * 8).coerceIn(0, 255)
            }
            pixels[index] = Color.rgb(channel(r, if (style == 0) -2 else 3),
                channel(g, if (style == 0) 2 else 0), channel(b, if (style == 0) 4 else -2))
        }
        val filtered = Bitmap.createBitmap(pixels, 240, 320, Bitmap.Config.ARGB_8888)
        val scaled = Bitmap.createScaledBitmap(filtered, outputWidth, outputHeight, false)
        val output = scaled.copy(Bitmap.Config.ARGB_8888, true)
        if (scaled !== filtered) scaled.recycle()
        filtered.recycle()
        if (small !== source && small !== cropped) small.recycle()
        if (cropped !== source) cropped.recycle()
        if (!dateStamp) return output
        val paint = Paint().apply {
            color = Color.rgb(255, 184, 65)
            textSize = outputWidth / 24f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            isAntiAlias = false
            textAlign = Paint.Align.RIGHT
        }
        val text = SimpleDateFormat("yyyy.MM.dd", Locale.US).format(date)
        val canvas = Canvas(output)
        val margin = outputWidth / 30f
        paint.color = Color.BLACK
        canvas.drawText(text, outputWidth - margin + 1, outputHeight - margin + 1, paint)
        paint.color = Color.rgb(255, 184, 65)
        canvas.drawText(text, outputWidth - margin, outputHeight - margin, paint)
        return output
    }
}
