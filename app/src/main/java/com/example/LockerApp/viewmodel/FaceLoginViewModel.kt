package com.example.LockerApp.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.LockerApp.repository.FaceAuthRepository
import kotlinx.coroutines.launch

class FaceLoginViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FaceAuthRepository(application)

    private val _loginState = MutableLiveData<LoginState>()
    val loginState: LiveData<LoginState> = _loginState

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

    //Reset to scan state
    fun resetToScanning() {
        _loginState.value = LoginState.Scanning
    }

    //Reset face
    fun refreshFaceData() {
        viewModelScope.launch {
            try {
                repository.refreshFaceData()
                resetToScanning()
            } catch (e: Exception) {
                // If refresh fails, just log it and continue
                _loginState.value = LoginState.Error("Failed to refresh face data: ${e.message}")
            }
        }
    }
}
