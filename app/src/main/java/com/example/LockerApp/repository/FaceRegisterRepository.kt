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