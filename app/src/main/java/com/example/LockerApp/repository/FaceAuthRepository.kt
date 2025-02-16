package com.example.LockerApp.repository

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.LockerApp.model.Account
import com.example.LockerApp.model.AccountDao
import com.example.LockerApp.model.LockerDatabase
import com.example.LockerApp.view.FaceClassifier
import com.example.LockerApp.view.TFLiteFaceRecognition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FaceAuthRepository(context: Context) {
    private val accountDao: AccountDao = LockerDatabase.getDatabase(context).accountDao()
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
        withContext(Dispatchers.IO) {
            faceClassifier.register(name, role, phone, recognition)
        }
    }

    suspend fun recognizeFace(bitmap: Bitmap): RecognitionResult = withContext(Dispatchers.IO) {
        val recognition = faceClassifier.recognizeImage(bitmap, false)
        Log.d("tryResiN","Now coming into this first function");
        if (recognition?.title != null && recognition.title != "Unknown" && recognition.distance!! < 0.7f) {
            Log.d("tryResiN","Now coming into ที่สอง function");
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
                RecognitionResult.Failure("User not found in database")
            }
        } else {
            RecognitionResult.Failure("Face not recognized")
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