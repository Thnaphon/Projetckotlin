package com.example.LockerApp.repository

import android.content.Context
import android.graphics.Bitmap
import com.example.LockerApp.view.FaceClassifier
import com.example.LockerApp.view.TFLiteFaceRecognition

class FaceRegisterRepository(private val context: Context) {
    private val faceClassifier: FaceClassifier = TFLiteFaceRecognition.create(
        context.assets,
        "facenet.tflite",
        160,
        false,
        context
    )

    data class SimilarityResult(
        val isSimilar: Boolean,
        val existingName: String = "",
        val distance: Float = Float.MAX_VALUE
    )

    suspend fun checkFaceSimilarity(recognition: FaceClassifier.Recognition): SimilarityResult {
        val threshold = 0.6f

        return when (val result = faceClassifier.recognizeImage(recognition.crop, true)) {
            null -> SimilarityResult(false)
            else -> {
                if (result.distance != null && result.distance < threshold && result.title != "Unknown") {
                    SimilarityResult(true, result.title.toString(), result.distance)
                } else {
                    SimilarityResult(false)
                }
            }
        }
    }

    suspend fun registerFace(
        name: String,
        role: String,
        phone: String,
        recognition: FaceClassifier.Recognition
    ) {
        faceClassifier.register(name, role, phone, recognition)
    }

    suspend fun getFaceRecognition(bitmap: Bitmap): FaceClassifier.Recognition? {
        return faceClassifier.recognizeImage(bitmap, true)
    }
}