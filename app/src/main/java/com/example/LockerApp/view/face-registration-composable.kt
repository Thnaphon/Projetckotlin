package com.example.LockerApp.view

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.LockerApp.repository.FaceRegisterRepository
import com.example.LockerApp.viewmodel.FaceRegisterViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@Composable
fun BatchFaceRegistrationScreen(
    onComplete: (successCount: Int, failureCount: Int) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val faceRegisterViewModel: FaceRegisterViewModel = viewModel()
    
    var isRegistering by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("Press the button to start registration") }
    var successCount by remember { mutableStateOf(0) }
    var failureCount by remember { mutableStateOf(0) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Batch Face Registration",
            style = MaterialTheme.typography.h5,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        Text(
            text = "This utility will register faces from your dataset directory. Each person folder should contain at least one face image.",
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        if (isRegistering) {
            CircularProgressIndicator(
                modifier = Modifier.padding(16.dp)
            )
        }
        
        Text(
            text = statusMessage,
            modifier = Modifier.padding(16.dp)
        )
        
        Button(
            onClick = {
                if (!isRegistering) {
                    isRegistering = true
                    statusMessage = "Registration in progress..."
                    
                    coroutineScope.launch {
                        val result = registerFacesFromAssets(context)
                        successCount = result.first
                        failureCount = result.second
                        
                        statusMessage = "Registration complete!\nSuccess: $successCount\nFailures: $failureCount"
                        isRegistering = false
                        
                        // Notify parent composable
                        onComplete(successCount, failureCount)
                    }
                }
            },
            enabled = !isRegistering,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Start Registration")
        }
    }
}

/**
 * Register faces from assets directory
 */
private suspend fun registerFacesFromAssets(
    context: android.content.Context
): Pair<Int, Int> = withContext(Dispatchers.IO) {
    var successCount = 0
    var failureCount = 0
    val TAG = "FaceRegistration"
    
    try {
        // Initialize face detector
        val faceDetector = FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build()
        )
        
        // Create repository
        val repository = FaceRegisterRepository(context)
        
        // List person directories
        val assetDir = "faces"
        val personDirs = context.assets.list(assetDir) ?: emptyArray()
        
        Log.d(TAG, "Found ${personDirs.size} person directories")
        
        for (personDir in personDirs) {
            try {
                // Get the first image for each person
                val imageFiles = context.assets.list("$assetDir/$personDir") ?: continue
                if (imageFiles.isEmpty()) continue
                
                val firstImagePath = "$assetDir/$personDir/${imageFiles[0]}"
                Log.d(TAG, "Processing image: $firstImagePath")
                
                // Extract person name (replace underscores with spaces)
                val personName = personDir.replace("_", " ")
                
                // Load bitmap from assets
                val inputStream = context.assets.open(firstImagePath)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                
                if (bitmap == null) {
                    Log.e(TAG, "Failed to decode bitmap from $firstImagePath")
                    failureCount++
                    continue
                }
                
                // Detect face using ML Kit
                val image = InputImage.fromBitmap(bitmap, 0)
                val faces = faceDetector.process(image).await()
                
                if (faces.isEmpty()) {
                    Log.e(TAG, "No face detected in $firstImagePath")
                    failureCount++
                    continue
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
                    Log.e(TAG, "Failed to get face recognition for $personName")
                    failureCount++
                    continue
                }
                
                // Set bitmap to recognition
                recognition.crop = resizedBitmap
                
                // Check if face is already registered
                val similarity = repository.checkFaceSimilarity(recognition)
                
                if (similarity.isSimilar) {
                    Log.w(TAG, "Face for $personName is similar to existing face: ${similarity.existingName}")
                    failureCount++
                    continue
                }
                
                // Register the face
                val role = "User"            // Mock role
                val phone = "1234567890"     // Mock phone
                
                repository.registerFace(personName, role, phone, recognition)
                Log.d(TAG, "Successfully registered face for $personName")
                successCount++
                
            } catch (e: Exception) {
                Log.e(TAG, "Error processing person directory $personDir", e)
                failureCount++
            }
        }
        
    } catch (e: Exception) {
        Log.e(TAG, "Error registering faces from assets", e)
    }
    
    Log.d(TAG, "Registration complete. Success: $successCount, Failures: $failureCount")
    return@withContext Pair(successCount, failureCount)
}
