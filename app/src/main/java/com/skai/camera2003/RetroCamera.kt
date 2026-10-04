package com.skai.camera2003

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import android.media.MediaScannerConnection

class RetroCamera(private val context: Context) {
    var frame by mutableStateOf<Bitmap?>(null)
        private set
    var ready by mutableStateOf(false)
        private set
    var busy by mutableStateOf(false)
        private set
    private val preferences = context.getSharedPreferences("camera", Context.MODE_PRIVATE)
    var settings by mutableStateOf(PhotoSettings(
        preferences.getBoolean("date", true),
        preferences.getInt("width", 480).let { if (it == 240) 240 else 480 },
        preferences.getInt("style", 0).coerceIn(0, 1)))
        private set
    fun updateSettings(value: PhotoSettings) {
        settings = value
        preferences.edit().putBoolean("date", value.dateStamp)
            .putInt("width", value.width).putInt("style", value.style).apply()
    }
    var message by mutableStateOf("")
    private val main = ContextCompat.getMainExecutor(context)
    private val worker = Executors.newSingleThreadExecutor()
    private var provider: ProcessCameraProvider? = null
    private var capture: ImageCapture? = null
    private var analysis: ImageAnalysis? = null
    private var generation = 0
    private var closed = false
    private var lastFrame = 0L

    @Suppress("DEPRECATION")
    fun bind(owner: LifecycleOwner) {
        val current = ++generation
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            if (!closed && current == generation) {
                try {
                    val cameras = future.get()
                    provider = cameras
                    val photo = ImageCapture.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                        .setTargetRotation(Surface.ROTATION_0)
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
                    val stream = ImageAnalysis.Builder().setTargetResolution(Size(640, 480))
                        .setTargetRotation(Surface.ROTATION_0)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                    stream.setAnalyzer(worker) { image ->
                        try {
                            val now = System.currentTimeMillis()
                            if (now - lastFrame >= 110) {
                                lastFrame = now
                                val small = RetroImage.fromYuv(image)
                                val chosen = settings
                                val shown = RetroImage.finish(small, 240, Date(now), chosen.dateStamp, chosen.style)
                                small.recycle()
                                main.execute {
                                    if (!closed && current == generation) frame = shown
                                }
                            }
                        } catch (exception: Exception) {
                            Log.e("SKAI", "Preview processing failed", exception)
                        } finally { image.close() }
                    }
                    cameras.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, photo, stream)
                    capture = photo
                    analysis = stream
                    ready = true
                    message = ""
                } catch (exception: Exception) {
                    Log.e("SKAI", "Camera binding failed", exception)
                    message = "카메라를 시작할 수 없습니다. 앱을 다시 실행해 주세요."
                }
            }
        }, main)
    }

    fun shoot() {
        val camera = capture ?: return
        if (busy || closed) return
        busy = true
        message = ""
        val date = Date()
        val chosen = settings
        camera.takePicture(worker, object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                try {
                    val buffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                    var sample = 1
                    while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1600) sample *= 2
                    val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size,
                        BitmapFactory.Options().apply { inSampleSize = sample })
                        ?: error("Cannot decode capture")
                    val rotated = Bitmap.createBitmap(original, 0, 0, original.width, original.height,
                        Matrix().apply { postRotate(image.imageInfo.rotationDegrees.toFloat()) }, false)
                    if (rotated !== original) original.recycle()
                    val result = RetroImage.finish(rotated, chosen.width, date, chosen.dateStamp, chosen.style)
                    rotated.recycle()
                    try { save(result, date) } finally { result.recycle() }
                    main.execute {
                        busy = false
                        if (!closed) message = "사진 저장 완료 · 갤러리의 SKAI 2003 폴더"
                    }
                } catch (exception: Exception) {
                    Log.e("SKAI", "Saving failed", exception)
                    main.execute {
                        busy = false
                        if (!closed) message = "저장하지 못했습니다. 권한과 저장 공간을 확인해 주세요."
                    }
                } finally { image.close() }
            }
            override fun onError(exception: ImageCaptureException) {
                Log.e("SKAI", "Capture failed", exception)
                main.execute {
                    busy = false
                    if (!closed) message = "촬영하지 못했습니다. 다시 시도해 주세요."
                }
            }
        })
    }

    private fun save(bitmap: Bitmap, date: Date) {
        val name = "SKAI_" + SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(date) + ".jpg"
        if (Build.VERSION.SDK_INT >= 29) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.DATE_TAKEN, date.time)
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SKAI 2003")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: error("Cannot create gallery entry")
            try {
                resolver.openOutputStream(uri)?.use {
                    check(bitmap.compress(Bitmap.CompressFormat.JPEG, 55, it))
                } ?: error("Cannot open photo")
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                check(resolver.update(uri, values, null, null) > 0)
            } catch (exception: Exception) {
                resolver.delete(uri, null, null)
                throw exception
            }
        } else {
            @Suppress("DEPRECATION")
            val directory = File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "SKAI 2003")
            check(directory.exists() || directory.mkdirs())
            val file = File(directory, name)
            try {
                file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.JPEG, 55, it)) }
                MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath),
                    arrayOf("image/jpeg"), null)
            } catch (exception: Exception) { file.delete(); throw exception }
        }
    }

    fun unbind() {
        generation++
        ready = false
        analysis?.clearAnalyzer()
        val useCases = listOfNotNull(capture, analysis)
        if (useCases.isNotEmpty()) provider?.unbind(*useCases.toTypedArray())
        capture = null
        analysis = null
        frame = null
    }

    fun close() {
        closed = true
        unbind()
        // Allow an in-flight photo to finish saving before the worker exits.
        worker.shutdown()
    }
}

/** Settings are frozen at shutter time and persisted between launches. */
data class PhotoSettings(val dateStamp: Boolean = true, val width: Int = 480, val style: Int = 0)
