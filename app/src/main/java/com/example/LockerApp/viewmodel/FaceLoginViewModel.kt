package com.example.LockerApp.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.LockerApp.repository.FaceAuthRepository
import com.example.LockerApp.utils.LivenessDetector
import com.google.mlkit.vision.face.Face
import kotlinx.coroutines.launch

class FaceLoginViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FaceAuthRepository(application)
    private val livenessDetector = LivenessDetector()

    private val _loginState = MutableLiveData<LoginState>()
    val loginState: LiveData<LoginState> = _loginState
    private val _livenessState = MutableLiveData<LivenessDetector.LivenessState>()
    val livenessState: LiveData<LivenessDetector.LivenessState> = _livenessState

    fun processFrame(face: Face, faceBitmap: Bitmap) {
        val livenessResult = livenessDetector.processFrame(face)
        _livenessState.value = livenessResult

        if (livenessResult.isComplete) {
            recognizeFace(faceBitmap)
        }
    }

    fun resetLivenessCheck() {
        livenessDetector.reset()
        _livenessState.value = LivenessDetector.LivenessState()
    }

    fun recognizeFace(faceBitmap: Bitmap) {
        viewModelScope.launch {
            try {
                when (val result = repository.recognizeFace(faceBitmap)) {
                    is FaceAuthRepository.RecognitionResult.Success -> {
                        _loginState.value = LoginState.Success(
                            result.userDetails.accountid,
                            result.userDetails.name,
                            result.userDetails.role,
                            result.userDetails.phone
                        )
                    }
                    is FaceAuthRepository.RecognitionResult.Failure -> {
                        _loginState.value = LoginState.Error(result.error)
                    }
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Recognition failed")
            }
        }
    }

    sealed class LoginState {
        object Scanning : LoginState()
        data class Success(val accountid:Int,val name: String, val role: String, val phone: String) : LoginState()
        data class Error(val message: String) : LoginState()
    }
}
