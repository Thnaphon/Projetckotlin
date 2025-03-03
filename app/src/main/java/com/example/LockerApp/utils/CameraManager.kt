package com.example.LockerApp.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import android.media.Image
import java.util.concurrent.Executor
import android.graphics.Matrix
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


class CameraManager(private val context: Context) {
    private var lastProcessingTimeStamp = 0L
    private val MINIMUM_TIME_BETWEEN_FRAMES = 200L // 200ms between frames
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var preview: Preview? = null

    private val faceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()
    )

    suspend fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        cameraExecutor: Executor,
        onFaceDetected: (Bitmap, Rect) -> Unit
    ) {
        try {
            Log.d("CameraManager", "Starting camera setup")

            // Get camera provider
            cameraProvider = getCameraProvider()

            // Unbind all use cases before binding new ones
            cameraProvider?.unbindAll()

            // Create the preview and analyzer
            preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                    Log.d("CameraManager", "Preview surface provider set")
                }

            imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .apply {
                    setAnalyzer(cameraExecutor) { imageProxy ->
                        processImage(imageProxy, onFaceDetected)
                    }
                    Log.d("CameraManager", "Image analyzer set")
                }

            // Try to use the front camera
            val cameraSelector = try {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } catch (e: Exception) {
                Log.w("CameraManager", "Front camera not available, falling back", e)
                CameraSelector.DEFAULT_FRONT_CAMERA
            }

            try {
                // Get a camera instance
                camera = cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
                Log.d("CameraManager", "Camera successfully bound to lifecycle")
            } catch (e: Exception) {
                Log.e("CameraManager", "Failed to bind camera: ${e.message}", e)
                throw e
            }
        } catch (e: Exception) {
            Log.e("CameraManager", "Camera setup failed: ${e.message}", e)
            throw e
        }
    }

    private suspend fun getCameraProvider(): ProcessCameraProvider = suspendCancellableCoroutine { continuation ->
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                val provider = cameraProviderFuture.get()
                Log.d("CameraManager", "Camera provider obtained successfully")
                continuation.resume(provider)
            } catch (e: Exception) {
                Log.e("CameraManager", "Failed to get camera provider: ${e.message}", e)
                continuation.resumeWithException(e)
            }
        }, ContextCompat.getMainExecutor(context))

        continuation.invokeOnCancellation {
            Log.d("CameraManager", "Camera provider coroutine was cancelled")
        }
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImage(
        imageProxy: ImageProxy,
        onFaceDetected: (Bitmap, Rect) -> Unit
    ) {
        val currentTimeStamp = System.currentTimeMillis()
        if (currentTimeStamp - lastProcessingTimeStamp < MINIMUM_TIME_BETWEEN_FRAMES) {
            imageProxy.close()
            return
        }
        lastProcessingTimeStamp = currentTimeStamp

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            try {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

                faceDetector.process(image)
                    .addOnSuccessListener { faces ->
                        if (faces.isNotEmpty()) {
                            try {
                                val face = faces.first()
                                val bitmap = mediaImageToBitmap(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                onFaceDetected(bitmap, face.boundingBox)
                            } catch (e: Exception) {
                                Log.e("CameraManager", "Error processing face: ${e.message}", e)
                            }
                        }
                        imageProxy.close()
                    }
                    .addOnFailureListener { e ->
                        Log.e("CameraManager", "Face detection failed: ${e.message}", e)
                        imageProxy.close()
                    }
            } catch (e: Exception) {
                Log.e("CameraManager", "Error in image processing: ${e.message}", e)
                imageProxy.close()
            }
        } else {
            imageProxy.close()
        }
    }

    fun mediaImageToBitmap(mediaImage: Image, rotationDegrees: Int): Bitmap {
        try {
            val width = mediaImage.width
            val height = mediaImage.height
            // Get the YUV planes
            val yPlane = mediaImage.planes[0]
            val uPlane = mediaImage.planes[1]
            val vPlane = mediaImage.planes[2]
            // Get plane buffers
            val yBuffer = yPlane.buffer
            val uBuffer = uPlane.buffer
            val vBuffer = vPlane.buffer
            // Get plane pixels strides
            val yPixelStride = yPlane.pixelStride
            val yRowStride = yPlane.rowStride
            val uPixelStride = uPlane.pixelStride
            val uRowStride = uPlane.rowStride
            val vPixelStride = vPlane.pixelStride
            val vRowStride = vPlane.rowStride
            // Create output buffer
            val outputArray = IntArray(width * height)
            var outputIndex = 0
            for (y in 0 until height) {
                val yRowIndex = y * yRowStride
                val uvRowIndex = (y shr 1) * uRowStride
                for (x in 0 until width) {
                    val uvx = x shr 1
                    // Extract YUV values
                    val yValue = yBuffer.get(yRowIndex + x * yPixelStride).toInt() and 0xFF
                    val uValue = uBuffer.get(uvRowIndex + uvx * uPixelStride).toInt() and 0xFF
                    val vValue = vBuffer.get(uvRowIndex + uvx * vPixelStride).toInt() and 0xFF
                    // YUV to RGB conversion
                    var r = yValue + (1.370705f * (vValue - 128)).toInt()
                    var g = yValue - (0.698001f * (vValue - 128)).toInt() - (0.337633f * (uValue - 128)).toInt()
                    var b = yValue + (1.732446f * (uValue - 128)).toInt()
                    // Clamp RGB values
                    r = r.coerceIn(0, 255)
                    g = g.coerceIn(0, 255)
                    b = b.coerceIn(0, 255)
                    // Pack RGB into output pixel
                    outputArray[outputIndex++] = 0xff000000.toInt() or (r shl 16) or (g shl 8) or b
                }
            }
            // Create bitmap from the RGB array
            var bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(outputArray, 0, width, 0, 0, width, height)
            // Apply rotation if needed
            if (rotationDegrees != 0) {
                val matrix = Matrix().apply {
                    postRotate(rotationDegrees.toFloat())
                }
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            }
            return bitmap
        } catch (e: Exception) {
            Log.e("CameraManager", "Error in mediaImageToBitmap: ${e.message}", e)
            throw e
        }
    }

    fun shutdown() {
        try {
            Log.d("CameraManager", "Shutting down camera")
            cameraProvider?.unbindAll()
            camera = null
            imageAnalysis = null
            preview = null
        } catch (e: Exception) {
            Log.e("CameraManager", "Error shutting down camera: ${e.message}", e)
        }
    }
}