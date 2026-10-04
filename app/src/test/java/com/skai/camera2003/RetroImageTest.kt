package com.skai.camera2003

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RetroImageTest {
    @Test fun immutableSourceProducesVgaJpegWithBurnedInDate() {
        val source = Bitmap.createBitmap(IntArray(800 * 600) { Color.rgb(70, 100, 120) },
            800, 600, Bitmap.Config.ARGB_8888)
        val output = RetroImage.finish(source, 480, Date(0))
        assertEquals(480, output.width)
        assertEquals(640, output.height)
        assertFalse(source.isRecycled)
        var amberPixels = 0
        for (y in 590 until 640) for (x in 250 until 480) {
            val pixel = output.getPixel(x, y)
            if (Color.red(pixel) > 200 && Color.green(pixel) > 100 && Color.blue(pixel) < 100) amberPixels++
        }
        assertTrue("Date must be written into the image pixels", amberPixels > 20)
        val bytes = ByteArrayOutputStream().also {
            assertTrue(output.compress(Bitmap.CompressFormat.JPEG, 55, it))
        }.toByteArray()
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        assertEquals(480, decoded.width)
        assertEquals(640, decoded.height)
        decoded.recycle(); output.recycle(); source.recycle()
    }

    @Test fun dateChangesOnlyTheStampAndPreviewKeepsSameAspectRatio() {
        val source = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888)
        source.eraseColor(Color.rgb(80, 120, 170))
        val first = RetroImage.finish(source, 240, Date(0))
        val second = RetroImage.finish(source, 240, Date(172800000))
        assertEquals(240, first.width)
        assertEquals(320, first.height)
        val a = IntArray(240 * 320)
        val b = IntArray(240 * 320)
        first.getPixels(a, 0, 240, 0, 0, 240, 320)
        second.getPixels(b, 0, 240, 0, 0, 240, 320)
        assertFalse("Changing the date must change saved pixels", a.contentEquals(b))
        assertArrayEquals(a.copyOfRange(0, 240 * 280), b.copyOfRange(0, 240 * 280))
        first.recycle(); second.recycle(); source.recycle()
    }
    @Test fun dateOffIgnoresDateAndDigitalStyleChangesThePhoto() {
        val source = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888)
        source.eraseColor(Color.rgb(90, 130, 170))
        val first = RetroImage.finish(source, 240, Date(0), dateStamp = false)
        val second = RetroImage.finish(source, 240, Date(172800000), dateStamp = false)
        val digital = RetroImage.finish(source, 240, Date(0), dateStamp = false, style = 1)
        val a = IntArray(240 * 320)
        val b = IntArray(a.size)
        val c = IntArray(a.size)
        first.getPixels(a, 0, 240, 0, 0, 240, 320)
        second.getPixels(b, 0, 240, 0, 0, 240, 320)
        digital.getPixels(c, 0, 240, 0, 0, 240, 320)
        assertArrayEquals(a, b)
        assertFalse(a.contentEquals(c))
        first.recycle(); second.recycle(); digital.recycle(); source.recycle()
    }

    @Test fun cameraSettingsSurviveRestart() {
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        context.getSharedPreferences("camera", android.content.Context.MODE_PRIVATE).edit().clear().commit()
        val first = RetroCamera(context)
        val chosen = PhotoSettings(dateStamp = false, width = 240, style = 1)
        first.updateSettings(chosen)
        first.close()
        val second = RetroCamera(context)
        assertEquals(chosen, second.settings)
        second.close()
        context.getSharedPreferences("camera", android.content.Context.MODE_PRIVATE).edit().clear().commit()
    }
}
