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
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceLandmark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CameraManager(private val context: Context) {
    private var lastProcessingTimeStamp = 0L
    private val MINIMUM_TIME_BETWEEN_FRAMES = 200L // 200ms between frames
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var preview: Preview? = null

    // Track when insufficient landmarks were detected to prevent rapid toast displays
    private var lastInsufficientLandmarksTime = 0L
    private val MINIMUM_TIME_BETWEEN_INSUFFICIENT_LANDMARKS = 3000L // 3 seconds
    private var insufficientLandmarksNotified = false

    // Allow configuration of face detector options to support different use cases
    fun createFaceDetector(
        performanceMode: Int = FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE,
        landmarkMode: Int = FaceDetectorOptions.LANDMARK_MODE_ALL,
        classificationMode: Int = FaceDetectorOptions.CLASSIFICATION_MODE_ALL
    ) = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(performanceMode)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .setLandmarkMode(landmarkMode)
            .setClassificationMode(classificationMode)
            .build()
    )

    // Default face detector for backward compatibility
    private val faceDetector = createFaceDetector()

    suspend fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        cameraExecutor: Executor,
        onFaceDetected: (Bitmap, Rect) -> Unit,
        onInsufficientLandmarks: () -> Unit
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
                        processImage(imageProxy, onFaceDetected, onInsufficientLandmarks)
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

    // New overlay-specific method for face login and verification
    suspend fun startCameraForOverlay(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        cameraExecutor: Executor,
        onFaceBitmapCaptured: (Bitmap) -> Unit, // Simplified callback for overlays
        onInsufficientLandmarks: () -> Unit,
        hidePreview: Boolean = true // Optional parameter to hide preview
    ) {
        try {
            Log.d("CameraManager", "Starting camera setup for overlay")

            // Get camera provider
            cameraProvider = getCameraProvider()

            // Unbind all use cases before binding new ones
            cameraProvider?.unbindAll()

            // Create the preview with potentially minimal size for hidden mode
            preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                    Log.d("CameraManager", "Preview surface provider set for overlay")
                }

            imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .apply {
                    setAnalyzer(cameraExecutor) { imageProxy ->
                        processImageForOverlay(imageProxy, onFaceBitmapCaptured, onInsufficientLandmarks)
                    }
                    Log.d("CameraManager", "Image analyzer set for overlay")
                }

            // Try to use the front camera
            val cameraSelector = try {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } catch (e: Exception) {
                Log.w("CameraManager", "Front camera not available, using default camera", e)
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
                Log.d("CameraManager", "Camera successfully bound to lifecycle for overlay")
            } catch (e: Exception) {
                Log.e("CameraManager", "Failed to bind camera for overlay: ${e.message}", e)
                throw e
            }
        } catch (e: Exception) {
            Log.e("CameraManager", "Camera setup failed for overlay: ${e.message}", e)
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

    // Helper method to check if all required landmarks are present with confidence values
    private fun validateRequiredLandmarks(face: com.google.mlkit.vision.face.Face): Boolean {
        val lefteye = face.getLandmark(FaceLandmark.LEFT_EYE)
        val righteye = face.getLandmark(FaceLandmark.RIGHT_EYE)
        val leftear = face.getLandmark(FaceLandmark.LEFT_EAR)
        val rightear = face.getLandmark(FaceLandmark.RIGHT_EAR)
        val cheekleft = face.getLandmark(FaceLandmark.LEFT_CHEEK)
        val cheekright = face.getLandmark(FaceLandmark.RIGHT_CHEEK)
        val nose = face.getLandmark(FaceLandmark.NOSE_BASE)
        val mouth = face.getLandmark(FaceLandmark.MOUTH_BOTTOM)

        // Get face contour points for additional validation
        val faceOval = face.getContour(FaceContour.FACE)?.points

        // Get eye contours to check if eyes are open
        val leftEyeContour = face.getContour(FaceContour.LEFT_EYE)?.points
        val rightEyeContour = face.getContour(FaceContour.RIGHT_EYE)?.points

        // Check if eyes are closed based on contours
        val leftEyeOpen = if (leftEyeContour != null && leftEyeContour.size >= 4) {
            // Calculate the vertical distance between top and bottom eye points
            val topPoint = leftEyeContour.maxByOrNull { it.y } ?: leftEyeContour[0]
            val bottomPoint = leftEyeContour.minByOrNull { it.y } ?: leftEyeContour[2]
            val eyeHeight = Math.abs(topPoint.y - bottomPoint.y)

            // Calculate width for aspect ratio
            val leftPoint = leftEyeContour.minByOrNull { it.x } ?: leftEyeContour[0]
            val rightPoint = leftEyeContour.maxByOrNull { it.x } ?: leftEyeContour[3]
            val eyeWidth = Math.abs(rightPoint.x - leftPoint.x)

            // Eye is considered open if the height to width ratio is above a threshold
            val aspectRatio = if (eyeWidth > 0) eyeHeight / eyeWidth else 0f
            aspectRatio > 0.2f  // Threshold for eye openness
        } else {
            false
        }

        val rightEyeOpen = if (rightEyeContour != null && rightEyeContour.size >= 4) {
            // Calculate the vertical distance between top and bottom eye points
            val topPoint = rightEyeContour.maxByOrNull { it.y } ?: rightEyeContour[0]
            val bottomPoint = rightEyeContour.minByOrNull { it.y } ?: rightEyeContour[2]
            val eyeHeight = Math.abs(topPoint.y - bottomPoint.y)

            // Calculate width for aspect ratio
            val leftPoint = rightEyeContour.minByOrNull { it.x } ?: rightEyeContour[0]
            val rightPoint = rightEyeContour.maxByOrNull { it.x } ?: rightEyeContour[3]
            val eyeWidth = Math.abs(rightPoint.x - leftPoint.x)

            // Eye is considered open if the height to width ratio is above a threshold
            val aspectRatio = if (eyeWidth > 0) eyeHeight / eyeWidth else 0f
            aspectRatio > 0.2f  // Threshold for eye openness
        } else {
            false
        }

        // Check if both eyes are open
        val areEyesOpen = leftEyeOpen && rightEyeOpen

        // Get smile probability
        val smileProbability = face.smilingProbability ?: 0f
        val isSmiling = smileProbability > 0.7f  // Threshold for smile detection

        // Check face rotation - reject faces that are rotated too much
        val rotationX = face.headEulerAngleX // Pitch
        val rotationY = face.headEulerAngleY // Yaw
        val rotationZ = face.headEulerAngleZ // Roll

        val isValidRotation = Math.abs(rotationY) < 25 && Math.abs(rotationX) < 25 && Math.abs(rotationZ) < 25

        // Log detailed detection information
        Log.d("CameraManager", "Landmarks detected: " +
                "Left Eye: ${lefteye != null}, " +
                "Right Eye: ${righteye != null}, " +
                "Left Ear: ${leftear != null}, " +
                "Right Ear: ${rightear != null}, " +
                "Left Cheek: ${cheekleft != null}, " +
                "Right Cheek: ${cheekright != null}, " +
                "Nose: ${nose != null}, " +
                "Mouth: ${mouth != null}, " +
                "Face Contour Points: ${faceOval?.size ?: 0}"
        )

        Log.d("CameraManager", "Face expressions: " +
                "Smiling: $isSmiling (${smileProbability * 100}%), " +
                "Left Eye Open: $leftEyeOpen, " +
                "Right Eye Open: $rightEyeOpen"
        )

        Log.d("CameraManager", "Face rotation: X(Pitch): $rotationX, Y(Yaw): $rotationY, Z(Roll): $rotationZ")

        // Check if any required landmark is missing
        val hasAllRequiredLandmarks = lefteye != null && righteye != null &&
                leftear != null && rightear != null &&
                cheekleft != null && cheekright != null &&
                nose != null && mouth != null

        // Check if face contour has enough points (for detecting partially covered faces)
        val hasEnoughContourPoints = faceOval != null && faceOval.size >= 15

        // Final validation combines all checks
        val isValid = hasAllRequiredLandmarks &&
                hasEnoughContourPoints &&
                isValidRotation &&
                !isSmiling &&  // Must NOT be smiling
                areEyesOpen    // Eyes must be open

        if (!isValid) {
            if (!hasAllRequiredLandmarks) {
                Log.w("CameraManager", "Missing required facial landmarks")
            }
            if (!hasEnoughContourPoints) {
                Log.w("CameraManager", "Insufficient face contour points: ${faceOval?.size ?: 0}/15")
            }
            if (!isValidRotation) {
                Log.w("CameraManager", "Face rotated too much: X=$rotationX, Y=$rotationY, Z=$rotationZ")
            }
            if (isSmiling) {
                Log.w("CameraManager", "User is smiling (${smileProbability * 100}%). Neutral expression required.")
            }
            if (!areEyesOpen) {
                Log.w("CameraManager", "Eyes are not fully open. Both eyes must be open.")
            }
        }

        return isValid
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImage(
        imageProxy: ImageProxy,
        onFaceDetected: (Bitmap, Rect) -> Unit,
        onInsufficientLandmarks: () -> Unit
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

                                // Validate all required landmarks are present
                                val allLandmarksDetected = validateRequiredLandmarks(face)

                                if (allLandmarksDetected) {
                                    Log.d("CameraManager", "All validation checks passed, processing face")
                                    // Reset the notification flag when face is valid
                                    insufficientLandmarksNotified = false
                                    onFaceDetected(bitmap, face.boundingBox)
                                } else {
                                    // Only trigger the callback if we haven't recently notified
                                    if (!insufficientLandmarksNotified ||
                                        (currentTimeStamp - lastInsufficientLandmarksTime) > MINIMUM_TIME_BETWEEN_INSUFFICIENT_LANDMARKS) {

                                        Log.w("CameraManager", "Face validation failed, stopping processing")
                                        lastInsufficientLandmarksTime = currentTimeStamp
                                        insufficientLandmarksNotified = true
                                        onInsufficientLandmarks()
                                    }
                                }
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

    // New method for overlay processing that returns just the face bitmap
    @OptIn(ExperimentalGetImage::class)
    private fun processImageForOverlay(
        imageProxy: ImageProxy,
        onFaceBitmapCaptured: (Bitmap) -> Unit,
        onInsufficientLandmarks: () -> Unit
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

                                // Validate all required landmarks are present
                                val allLandmarksDetected = validateRequiredLandmarks(face)

                                if (allLandmarksDetected) {
                                    Log.d("CameraManager", "All validation checks passed, processing face for overlay")
                                    // Reset the notification flag when face is valid
                                    insufficientLandmarksNotified = false

                                    // Extract and resize the face bitmap
                                    val faceBitmap = Bitmap.createBitmap(
                                        bitmap,
                                        face.boundingBox.left.coerceAtLeast(0),
                                        face.boundingBox.top.coerceAtLeast(0),
                                        face.boundingBox.width().coerceAtMost(bitmap.width - face.boundingBox.left),
                                        face.boundingBox.height().coerceAtMost(bitmap.height - face.boundingBox.top)
                                    )
                                    val resizedFaceBitmap = Bitmap.createScaledBitmap(faceBitmap, 160, 160, false)

                                    // Pass only the face bitmap to the callback
                                    onFaceBitmapCaptured(resizedFaceBitmap)
                                } else {
                                    // Only trigger the callback if we haven't recently notified
                                    if (!insufficientLandmarksNotified ||
                                        (currentTimeStamp - lastInsufficientLandmarksTime) > MINIMUM_TIME_BETWEEN_INSUFFICIENT_LANDMARKS) {

                                        Log.w("CameraManager", "Face validation failed for overlay")
                                        lastInsufficientLandmarksTime = currentTimeStamp
                                        insufficientLandmarksNotified = true
                                        onInsufficientLandmarks()
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("CameraManager", "Error processing face for overlay: ${e.message}", e)
                            }
                        }
                        imageProxy.close()
                    }
                    .addOnFailureListener { e ->
                        Log.e("CameraManager", "Face detection failed for overlay: ${e.message}", e)
                        imageProxy.close()
                    }
            } catch (e: Exception) {
                Log.e("CameraManager", "Error in image processing for overlay: ${e.message}", e)
                imageProxy.close()
            }
        } else {
            imageProxy.close()
        }
    }

    // Central media image conversion method - now available to all components
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

    // Consolidated shutdown method
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