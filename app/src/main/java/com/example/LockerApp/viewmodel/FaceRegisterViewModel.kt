package com.example.LockerApp.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.LockerApp.repository.FaceRegisterRepository
import kotlinx.coroutines.launch

class FaceRegisterViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FaceRegisterRepository(application)

    private val _registrationState = MutableLiveData<RegistrationState>()
    val registrationState: LiveData<RegistrationState> = _registrationState

    private val _recognizedName = MutableLiveData<String>()
    val recognizedName: LiveData<String> = _recognizedName

    private val _capturedFace = MutableLiveData<Bitmap?>()
    val capturedFace: LiveData<Bitmap?> = _capturedFace

    fun registerFace(name: String, role: String, phone: String, faceBitmap: Bitmap) {
        viewModelScope.launch {
            try {
                val resizedBitmap = Bitmap.createScaledBitmap(faceBitmap, 160, 160, false)
                val recognition = repository.getFaceRecognition(resizedBitmap)

                recognition?.let {
                    repository.registerFace(name, role, phone, it)
                    _registrationState.value = RegistrationState.Success("Face registered for $name")
                } ?: run {
                    _registrationState.value = RegistrationState.Error("Failed to get face recognition")
                }
            } catch (e: Exception) {
                _registrationState.value = RegistrationState.Error(e.message ?: "Registration failed")
            }
        }
    }

    fun recognizeFace(faceBitmap: Bitmap) {
        viewModelScope.launch {
            try {
                val resizedBitmap = Bitmap.createScaledBitmap(faceBitmap, 160, 160, false)
                val recognition = repository.getFaceRecognition(resizedBitmap)
                _recognizedName.value = recognition?.title ?: "Unknown"
            } catch (e: Exception) {
                _recognizedName.value = "Unknown"
            }
        }
    }

    fun setCapturedFace(bitmap: Bitmap?) {
        _capturedFace.value = bitmap
    }

    sealed class RegistrationState {
        data class Success(val message: String) : RegistrationState()
        data class Error(val message: String) : RegistrationState()
    }
}
