package com.skai.camera2003

import android.content.Intent
import android.database.MatrixCursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PhotoRepositoryTest {
    @Test fun sharesOriginalContentUriWithTemporaryReadPermission() {
        val uri = Uri.parse("content://media/external/images/media/42")
        val intent = PhotoRepository.shareIntent(uri)
        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("image/jpeg", intent.type)
        assertEquals(uri, intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))
        assertEquals(uri, intent.clipData!!.getItemAt(0).uri)
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        assertEquals(0, intent.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }

    @Test(expected = IllegalArgumentException::class)
    fun refusesFileUrisForSharing() {
        PhotoRepository.shareIntent(Uri.parse("file:///sdcard/Pictures/SKAI.jpg"))
    }

    @Test fun galleryRowsExcludeOtherImagesAndRecoverOlderPhotoDates() {
        MatrixCursor(arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN, MediaStore.Images.Media.DATE_ADDED)).use { cursor ->
            cursor.addRow(arrayOf<Any?>(1L, "SKAI_old.jpg", 2000L, 1L))
            cursor.addRow(arrayOf<Any?>(2L, "SKAI_20261004_120000_123.jpg", null, 5L))
            cursor.addRow(arrayOf<Any?>(3L, "Screenshot.jpg", 999999L, 99L))
            cursor.addRow(arrayOf<Any?>(4L, "SKAI_video.mp4", 999999L, 99L))
            val entries = PhotoRepository.readRows(cursor)
            assertEquals(2, entries.size)
            assertEquals("SKAI_20261004_120000_123.jpg", entries[0].name)
            assertTrue(entries[0].takenAt > 1700000000000L)
            assertEquals(Uri.parse("content://media/external/images/media/2"), entries[0].uri)
            assertEquals(2000L, entries[1].takenAt)
        }
    }

    @Test fun olderAndroidCanListDecodeAndSharePhotosButProviderDoesNotExposeOtherFiles() {
        val context = RuntimeEnvironment.getApplication()
        // Robolectric defaults these two directories to different roots; real Android does not.
        org.robolectric.shadows.ShadowEnvironment.setExternalStoragePublicDirectory(
            Environment.getExternalStorageDirectory().toPath())
        @Suppress("DEPRECATION")
        val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "SKAI 2003")
        directory.mkdirs()
        val photo = File(directory, "SKAI_TEST_ALBUM.jpg")
        val unrelated = File(directory, "other.jpg")
        val privateFile = File(context.filesDir, "private-test.txt")
        try {
            val bitmap = Bitmap.createBitmap(32, 40, Bitmap.Config.ARGB_8888)
            photo.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 55, it)) }
            bitmap.recycle()
            unrelated.writeText("not a SKAI photo")
            privateFile.writeText("private")
            val repo = PhotoRepository(context)
            val entries = repo.listPhotos()
            val entry = entries.single { it.name == photo.name }
            assertFalse(entries.any { it.name == unrelated.name })
            assertEquals("content", entry.uri.scheme)
            assertEquals("${context.packageName}.photos", entry.uri.authority)
            val decoded = repo.loadBitmap(entry.uri, 20)
            assertEquals(16, decoded.width)
            assertEquals(20, decoded.height)
            decoded.recycle()
            assertEquals(entry.uri, PhotoRepository.shareIntent(entry.uri).clipData!!.getItemAt(0).uri)
            try {
                FileProvider.getUriForFile(context, "${context.packageName}.photos", privateFile)
                fail("Provider must only expose the SKAI photos folder")
            } catch (_: IllegalArgumentException) { /* expected */ }
        } finally { photo.delete(); unrelated.delete(); privateFile.delete() }
    }
}
