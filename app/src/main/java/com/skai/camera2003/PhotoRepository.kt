package com.skai.camera2003

import android.content.ClipData
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

data class PhotoEntry(val uri: Uri, val name: String, val takenAt: Long)

class PhotoRepository(private val context: Context) {
    /** On Android 10+ the platform exposes this app's own MediaStore photos without a read permission. */
    fun listPhotos(): List<PhotoEntry> {
        if (Build.VERSION.SDK_INT >= 29) {
            val columns = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_TAKEN, MediaStore.Images.Media.DATE_ADDED)
            return context.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                columns, "${MediaStore.Images.Media.RELATIVE_PATH} = ? AND ${MediaStore.Images.Media.IS_PENDING} = 0",
                arrayOf("Pictures/SKAI 2003/"), "${MediaStore.Images.Media.DATE_ADDED} DESC, ${MediaStore.Images.Media._ID} DESC")
                ?.use { readRows(it) } ?: emptyList()
        }
        @Suppress("DEPRECATION")
        val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "SKAI 2003")
        if (directory.exists() && !directory.canRead()) throw SecurityException("Photo directory is not readable")
        return directory.listFiles().orEmpty().filter { it.isFile && isSkaiPhoto(it.name) }
            .map { PhotoEntry(FileProvider.getUriForFile(context, "${context.packageName}.photos", it),
                it.name, filenameDate(it.name) ?: it.lastModified()) }
            .sortedWith(compareByDescending<PhotoEntry> { it.takenAt }.thenByDescending { it.name })
    }

    fun loadBitmap(uri: Uri, maxEdge: Int = 640): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds); Unit }
            ?: error("Photo is no longer available")
        check(bounds.outWidth > 0 && bounds.outHeight > 0) { "Not a decodable image" }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxEdge) sample *= 2
        return context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: error("Cannot decode photo")
    }

    companion object {
        fun isSkaiPhoto(name: String) = name.startsWith("SKAI_") && name.endsWith(".jpg", ignoreCase = true)
        private fun filenameDate(name: String): Long? = try {
            SimpleDateFormat("'SKAI_'yyyyMMdd_HHmmss_SSS'.jpg'", Locale.US)
                .apply { isLenient = false }.parse(name)?.time
        } catch (_: Exception) { null }

        internal fun readRows(cursor: Cursor): List<PhotoEntry> {
            val id = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val name = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val taken = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val added = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val result = mutableListOf<PhotoEntry>()
            while (cursor.moveToNext()) {
                val title = cursor.getString(name) ?: continue
                if (!isSkaiPhoto(title)) continue
                val date = cursor.getLong(taken).takeIf { it > 0 } ?: filenameDate(title) ?: cursor.getLong(added) * 1000
                result += PhotoEntry(ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    cursor.getLong(id)), title, date)
            }
            return result.sortedWith(compareByDescending<PhotoEntry> { it.takenAt }.thenByDescending { it.name })
        }

        /** Grants the chosen target temporary read access to the original JPEG, including its date stamp. */
        fun shareIntent(uri: Uri, mimeType: String = "image/jpeg"): Intent {
            require(uri.scheme == "content") { "Sharing requires a content URI" }
            return Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newRawUri("SKAI photo", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
    }
}
