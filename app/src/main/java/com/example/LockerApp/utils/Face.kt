package com.example.LockerApp.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.example.LockerApp.repository.FaceRegisterRepository
import com.example.LockerApp.view.FaceClassifier
import com.example.LockerApp.view.TFLiteFaceRecognition
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Utility class to batch register faces from a directory of images
 */
class FaceRegistrationUtility(private val context: Context) {

    private val TAG = "FaceRegistrationUtil"
    private val repository = FaceRegisterRepository(context)

    // Create high-accuracy face detector
    private val faceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()
    )

    /**
     * Process a directory of face images and register them in the database
     * @param assetDir Directory in assets containing subfolders with person names
     * @return Pair of (success count, failure count)
     */
    suspend fun registerFacesFromAssets(assetDir: String): Pair<Int, Int> = withContext(Dispatchers.IO) {
        var successCount = 0
        var failureCount = 0

        try {
            // List all person directories
            val personDirs = context.assets.list(assetDir) ?: return@withContext Pair(0, 0)

            Log.d(TAG, "Found ${personDirs.size} person directories in assets/$assetDir")

            for (personDir in personDirs) {
                try {
                    // Get the first image for each person
                    val imageFiles = context.assets.list("$assetDir/$personDir") ?: continue
                    if (imageFiles.isEmpty()) continue

                    val firstImagePath = "$assetDir/$personDir/${imageFiles[0]}"
                    Log.d(TAG, "Processing image: $firstImagePath")

                    // Extract person name (remove underscores)
                    val personName = personDir.replace("_", " ")

                    // Process and register the face
                    val result = processAndRegisterFace(
                        firstImagePath,
                        personName,
                        "User",  // Default role
                        "1234567890"  // Mock phone number
                    )

                    if (result) {
                        successCount++
                        Log.d(TAG, "Successfully registered face for $personName")
                    } else {
                        failureCount++
                        Log.e(TAG, "Failed to register face for $personName")
                    }

                } catch (e: Exception) {
                    failureCount++
                    Log.e(TAG, "Error processing person directory $personDir", e)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error registering faces from assets", e)
        }

        Log.d(TAG, "Registration complete. Success: $successCount, Failures: $failureCount")
        return@withContext Pair(successCount, failureCount)
    }

    /**
     * Process a single image and register the detected face
     */
    private suspend fun processAndRegisterFace(
        imagePath: String,
        name: String,
        role: String,
        phone: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // Load image from assets
            val inputStream = context.assets.open(imagePath)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (bitmap == null) {
                Log.e(TAG, "Failed to decode bitmap from $imagePath")
                return@withContext false
            }

            // Detect face using ML Kit
            val image = InputImage.fromBitmap(bitmap, 0)
            val faces = faceDetector.process(image).await()

            if (faces.isEmpty()) {
                Log.e(TAG, "No face detected in $imagePath")
                return@withContext false
            }

            // Get the first face and crop it
            val face = faces.first()
            val boundingBox = face.boundingBox

            // Ensure bounding box is within image bounds
            val left = boundingBox.left.coerceAtLeast(0)
            val top = boundingBox.top.coerceAtLeast(0)
            val width = boundingBox.width().coerceAtMost(bitmap.width - left)
            val height = boundingBox.height().coerceAtMost(bitmap.height - top)

            // Crop and resize face
            val faceBitmap = Bitmap.createBitmap(bitmap, left, top, width, height)
            val resizedBitmap = Bitmap.createScaledBitmap(faceBitmap, 160, 160, false)

            // Get face recognition
            val recognition = repository.getFaceRecognition(resizedBitmap)

            if (recognition == null) {
                Log.e(TAG, "Failed to get face recognition for $name")
                return@withContext false
            }

            // Check if face is already registered
            recognition.crop = resizedBitmap
            val similarity = repository.checkFaceSimilarity(recognition)

            if (similarity.isSimilar) {
                Log.w(TAG, "Face for $name is similar to existing face: ${similarity.existingName}")
                return@withContext false
            }

            // Register the face
            repository.registerFace(name, role, phone, recognition)
            Log.d(TAG, "Registered face for $name")

            return@withContext true

        } catch (e: Exception) {
            Log.e(TAG, "Error processing face for $name", e)
            return@withContext false
        }
    }
}