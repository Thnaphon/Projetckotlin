package com.example.LockerApp.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.LockerApp.repository.FaceRegisterRepository
import com.example.LockerApp.utils.LivenessDetector
import com.google.mlkit.vision.face.Face
import kotlinx.coroutines.launch

class FaceRegisterViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FaceRegisterRepository(application)
    private val livenessDetector = LivenessDetector()

    //state
    private val _registrationState = MutableLiveData<RegistrationState>()
    val registrationState: LiveData<RegistrationState> = _registrationState

    //name checking recognize with database
    private val _recognizedName = MutableLiveData<String>()
    val recognizedName: LiveData<String> = _recognizedName

    private val _capturedFace = MutableLiveData<Bitmap?>()
    val capturedFace: LiveData<Bitmap?> = _capturedFace

    //liveness Progress
    private val _livenessState = MutableLiveData<LivenessDetector.LivenessState>()
    val livenessState: LiveData<LivenessDetector.LivenessState> = _livenessState

    //register check with database
    private val _similarityCheck = MutableLiveData<SimilarityCheckResult>()
    val similarityCheck: LiveData<SimilarityCheckResult> = _similarityCheck

    init {
        // Set initial states
        // Set initial states without error messages
        _recognizedName.value = ""
        _similarityCheck.value = SimilarityCheckResult.Initial
        _registrationState.value = RegistrationState.Initial
    }

    // In FaceRegisterViewModel.kt, modify the registerFace method:

    fun registerFace(name: String, role: String, phone: String, faceBitmap: Bitmap): Int {
        var newAccountId = 0
        viewModelScope.launch {
            try {
                _registrationState.value = RegistrationState.Processing

                val resizedBitmap = Bitmap.createScaledBitmap(faceBitmap, 160, 160, false)
                val recognition = repository.getFaceRecognition(resizedBitmap)

                recognition?.let {
                    it.crop = resizedBitmap  // Set the bitmap to the recognition object
                    val similarity = repository.checkFaceSimilarity(it)

                    when {
                        similarity.isSimilar -> {
                            _registrationState.value = RegistrationState.Error(
                                "Face already registered under name: ${similarity.existingName}"
                            )
                            _similarityCheck.value = SimilarityCheckResult.Similar(similarity.existingName)
                        }
                        else -> {
                            // Get the newly created account ID after registration
                            newAccountId = repository.registerFace(name, role, phone, it)

                            _registrationState.value = RegistrationState.Success("Face registered for $name")
                            _similarityCheck.value = SimilarityCheckResult.Unique
                        }
                    }
                } ?: run {
                    _registrationState.value = RegistrationState.Error("No face detected. Please try again.")
                }
            } catch (e: Exception) {
                _registrationState.value = RegistrationState.Error("Registration failed: ${e.message}")
            }
        }

        return newAccountId
    }

    fun recognizeFace(faceBitmap: Bitmap) {
        viewModelScope.launch {
            try {
                val resizedBitmap = Bitmap.createScaledBitmap(faceBitmap, 160, 160, false)
                val recognition = repository.getFaceRecognition(resizedBitmap)

                recognition?.let {
                    it.crop = resizedBitmap  // Set the bitmap to the recognition object
                    val similarity = repository.checkFaceSimilarity(it)
                    if (similarity.isSimilar) {
                        _recognizedName.value = similarity.existingName
                        _similarityCheck.value = SimilarityCheckResult.Similar(similarity.existingName)
                    } else {
                        _recognizedName.value = ""  // Don't show "Unknown", just empty
                        _similarityCheck.value = SimilarityCheckResult.Unique
                    }
                } ?: run {
                    // Don't update states if no face is detected
                    _recognizedName.value = ""
                }
            } catch (e: Exception) {
                // Don't update error state for continuous recognition
                _recognizedName.value = ""
            }
        }
    }

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

    fun setCapturedFace(bitmap: Bitmap?) {
        _capturedFace.value = bitmap
        if (bitmap == null) {
            // Reset states when face capture is cleared
            _recognizedName.value = ""
            _similarityCheck.value = SimilarityCheckResult.Initial
            _registrationState.value = RegistrationState.Initial
        }
    }

    sealed class RegistrationState {
        object Initial : RegistrationState()
        object Processing : RegistrationState()
        data class Success(val message: String) : RegistrationState()
        data class Error(val message: String) : RegistrationState()
    }

    sealed class SimilarityCheckResult {
        object Initial : SimilarityCheckResult()
        data class Similar(val existingName: String) : SimilarityCheckResult()
        object Unique : SimilarityCheckResult()
        data class Error(val message: String) : SimilarityCheckResult()
    }
}
