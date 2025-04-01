package com.example.LockerApp.repository

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.LockerApp.model.AccountDao
import com.example.LockerApp.model.LockerDatabase
import com.example.LockerApp.view.FaceClassifier
import com.example.LockerApp.view.TFLiteFaceRecognition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class FaceAuthRepository(private val context: Context) {
    private val accountDao: AccountDao = LockerDatabase.getDatabase(context).accountDao()
    private var faceClassifier: FaceClassifier? = null

    // Initialize classifier safely
    init {
        initializeClassifier()
    }

    // Method to initialize or re-initialize the classifier
    private fun initializeClassifier() {
        try {
            faceClassifier = TFLiteFaceRecognition.create(
                context.assets,
                "facenet.tflite",
                160,
                false,
                context
            )
            Log.d("FaceAuthRepository", "Face classifier initialized successfully")
        } catch (e: Exception) {
            Log.e("FaceAuthRepository", "Error initializing face classifier", e)
        }
    }

    //method to refresh the face database
    suspend fun refreshFaceData() = withContext(Dispatchers.IO) {
        try {
            Log.d("FaceAuthRepository", "Refreshing face recognition data")
            // Reinitialize the classifier to load latest face data
            initializeClassifier()
        } catch (e: Exception) {
            Log.e("FaceAuthRepository", "Error refreshing face data", e)
        }
    }

    suspend fun recognizeFace(bitmap: Bitmap): RecognitionResult = withContext(Dispatchers.IO) {
        try {
            // Ensure the classifier is initialized
            if (faceClassifier == null) {
                // Try to reinitialize if it's null
                initializeClassifier()
                if (faceClassifier == null) {
                    return@withContext RecognitionResult.Failure("Face recognition system not available")
                }
            }

            // Preprocess the bitmap to ensure consistent format
            val processedBitmap = preprocessBitmap(bitmap)

            // Add timeout to prevent hanging if TFLite crashes
            return@withContext withTimeout(5000) { // 5-second timeout
                val recognition = try {
                    faceClassifier?.recognizeImage(processedBitmap, false)
                } catch (e: Exception) {
                    Log.e("FaceAuthRepository", "Error during face recognition", e)
                    return@withTimeout RecognitionResult.Failure("Recognition failed: ${e.message}")
                }

                //Adjust distance < xxx if you want to make more confident to that person lower mean more confident more mean less strict (can be other person)
                if (recognition?.title != null && recognition.title != "Unknown" && recognition.distance!! < 0.75f) {
                    try {
                        val user = accountDao.getUserByName(recognition.title)
                        if (user != null) {
                            RecognitionResult.Success(
                                UserDetails(
                                    accountid = user.AccountID,
                                    name = user.Name,
                                    role = user.Role,
                                    phone = user.Phone
                                )
                            )
                        } else {
                            RecognitionResult.Failure("Timeout")
                        }
                    } catch (e: Exception) {
                        Log.e("FaceAuthRepository", "Database error", e)
                        RecognitionResult.Failure("Database error: ${e.message}")
                    }
                } else {
                    RecognitionResult.Failure("Access Deny")
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.e("FaceAuthRepository", "Face recognition timed out", e)
            RecognitionResult.Failure("Recognition timed out. Please try again.")
        } catch (e: OutOfMemoryError) {
            Log.e("FaceAuthRepository", "Out of memory during face recognition", e)
            RecognitionResult.Failure("หน่วยความจำไม่เพียงพอ")
        } catch (e: Exception) {
            Log.e("FaceAuthRepository", "Unexpected error in face recognition", e)
            RecognitionResult.Failure("Recognition error: ${e.message}")
        }
    }

    //method to preprocess bitmap for more consistent results
    private fun preprocessBitmap(original: Bitmap): Bitmap {
        try {
            // Ensure consistent size and format to avoid memory issues
            val targetSize = 160 // Standard size for facenet model

            // If already correct size, just return a copy to ensure consistent format
            if (original.width == targetSize && original.height == targetSize) {
                // Create a copy to ensure we're working with a clean bitmap
                val copy = original.copy(Bitmap.Config.ARGB_8888, true)
                return copy
            }

            // Resize bitmap to appropriate dimensions
            val resized = Bitmap.createScaledBitmap(original, targetSize, targetSize, true)

            // Convert to consistent format (ARGB_8888) if needed
            if (resized.config != Bitmap.Config.ARGB_8888) {
                val converted = resized.copy(Bitmap.Config.ARGB_8888, true)
                resized.recycle() // Clean up the intermediate bitmap
                return converted
            }

            return resized
        } catch (e: Exception) {
            Log.e("FaceAuthRepository", "Error preprocessing bitmap", e)
            // If preprocessing fails, return the original to attempt recognition anyway
            return original
        }
    }

    data class UserDetails(
        val accountid: Int,
        val name: String,
        val role: String,
        val phone: String
    )

    sealed class RecognitionResult {
        data class Success(val userDetails: UserDetails) : RecognitionResult()
        data class Failure(val error: String) : RecognitionResult()
    }
}