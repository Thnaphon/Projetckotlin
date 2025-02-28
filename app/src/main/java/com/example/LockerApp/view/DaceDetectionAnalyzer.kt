package com.example.LockerApp.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.Image
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.compose.ui.geometry.Offset
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.abs

class FaceDetectionAnalyzer(
    private val context: Context,
    private val ovalCenter: Offset,
    private val ovalRadiusX: Float,
    private val ovalRadiusY: Float,
    private val onFaceDetected: (Boolean) -> Unit,
    private val onFaceBitmapCaptured: (Bitmap) -> Unit
) : ImageAnalysis.Analyzer {

    companion object {
        private const val THROTTLE_TIMEOUT_MS = 500L
        private const val MIN_FACE_SIZE = 50f
        private const val FACE_SIZE_MULTIPLIER = 3.5f
        private const val FACE_POSITION_ACCURACY = 0.3f
        private const val CAPTURE_INTERVAL_MS = 1000L // Capture face bitmap every second
    }

    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val faceDetector: FaceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()
    )
    
    private var lastCaptureTime = 0L

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        scope.launch {
            val mediaImage = imageProxy.image ?: run {
                imageProxy.close()
                return@launch
            }

            val inputImage = InputImage.fromMediaImage(
                mediaImage, imageProxy.imageInfo.rotationDegrees,
            )

            suspendCoroutine<Unit> { continuation ->
                faceDetector.process(inputImage)
                    .addOnSuccessListener { faces ->
                        var faceInOval = false
                        
                        faces.firstOrNull()?.let { face ->
                            val faceCenter = Offset(
                                face.boundingBox.centerX().toFloat(), 
                                face.boundingBox.centerY().toFloat()
                            )
                            
                            val faceWidth = face.boundingBox.width().toFloat()
                            val faceHeight = face.boundingBox.height().toFloat()
                            
                            faceInOval = isFaceInsideOval(
                                faceCenter,
                                faceWidth,
                                faceHeight
                            )
                            
                            // If face is inside oval, capture bitmap for recognition
                            if (faceInOval) {
                                val currentTime = System.currentTimeMillis()
                                if (currentTime - lastCaptureTime >= CAPTURE_INTERVAL_MS) {
                                    lastCaptureTime = currentTime
                                    
                                    val bitmap = mediaImageToBitmap(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                    val faceBitmap = Bitmap.createBitmap(
                                        bitmap,
                                        face.boundingBox.left.coerceAtLeast(0),
                                        face.boundingBox.top.coerceAtLeast(0),
                                        face.boundingBox.width().coerceAtMost(bitmap.width - face.boundingBox.left),
                                        face.boundingBox.height().coerceAtMost(bitmap.height - face.boundingBox.top)
                                    )
                                    val resizedFaceBitmap = Bitmap.createScaledBitmap(faceBitmap, 160, 160, false)
                                    onFaceBitmapCaptured(resizedFaceBitmap)
                                }
                            }
                        }
                        
                        onFaceDetected(faceInOval)
                    }
                    .addOnFailureListener { exception ->
                        Log.e("FaceDetection", "Face detection failed", exception)
                    }
                    .addOnCompleteListener {
                        continuation.resume(Unit)
                    }
            }

            delay(THROTTLE_TIMEOUT_MS)
        }.invokeOnCompletion { exception ->
            exception?.printStackTrace()
            imageProxy.close()
        }
    }

    private fun isFaceInsideOval(
        faceCenter: Offset,
        faceWidth: Float,
        faceHeight: Float
    ): Boolean {
        val verticalFaceCenter = Offset(
            faceCenter.x,
            faceCenter.y - ovalRadiusY
        )

        val xInsideOval = abs(verticalFaceCenter.x - ovalCenter.x) <= (ovalRadiusX * FACE_POSITION_ACCURACY)
        val yInsideOval = abs(verticalFaceCenter.y - ovalCenter.y) <= (ovalRadiusY * FACE_POSITION_ACCURACY)

        val isCenterInsideOval = xInsideOval && yInsideOval

        val faceFitsInOval = faceWidth in (MIN_FACE_SIZE)..(ovalRadiusX * FACE_SIZE_MULTIPLIER) &&
                faceHeight in MIN_FACE_SIZE..(ovalRadiusY * FACE_SIZE_MULTIPLIER)

        return isCenterInsideOval && faceFitsInOval
    }
    
    fun mediaImageToBitmap(mediaImage: Image, rotationDegrees: Int): Bitmap {
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
    }
}
