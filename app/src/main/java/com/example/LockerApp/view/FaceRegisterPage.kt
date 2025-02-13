package com.example.LockerApp.view

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.Log
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.example.LockerApp.utils.CameraManager
import com.example.LockerApp.viewmodel.FaceRegisterViewModel
import java.util.concurrent.Executors
import kotlinx.coroutines.launch
import androidx.camera.view.PreviewView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay

@Composable
fun FaceRegisterPage(
    navController: NavHostController,
    viewModel: FaceRegisterViewModel,
    participantName: String,
    participantPhone: String,
    participantRole: String
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val cameraManager = remember { CameraManager(context) }
    val previewView = remember { androidx.camera.view.PreviewView(context) }

    val recognizedName by viewModel.recognizedName.observeAsState("Unknown")
    val capturedFace by viewModel.capturedFace.observeAsState()
    val registrationState by viewModel.registrationState.observeAsState()
    val similarityCheck by viewModel.similarityCheck.observeAsState()
    var shouldCaptureFace by remember { mutableStateOf(false) }
    var isRegistrationSuccessful by remember { mutableStateOf(false) }

    LaunchedEffect(registrationState) {
        when (registrationState) {
            is FaceRegisterViewModel.RegistrationState.Success -> {
                if (!isRegistrationSuccessful) {
                    isRegistrationSuccessful = true
                    delay(500) // delay 0.5 sec
                    navController.navigate("main_menu") {
                        popUpTo("face_detection") { inclusive = true }
                    }
                }
            }
            else -> {
                // Reset the flag for other states
                isRegistrationSuccessful = false
            }
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            cameraManager.startCamera(
                lifecycleOwner = lifecycleOwner,
                previewView = previewView,
                cameraExecutor = cameraExecutor,
                onFaceDetected = { bitmap, rect ->
                    val faceBitmap = Bitmap.createBitmap(
                        bitmap,
                        rect.left.coerceAtLeast(0),
                        rect.top.coerceAtLeast(0),
                        rect.width().coerceAtMost(bitmap.width - rect.left),
                        rect.height().coerceAtMost(bitmap.height - rect.top)
                    )

                    // Always perform recognition
                    viewModel.recognizeFace(faceBitmap)

                    // If capture is requested, save the face
                    if (shouldCaptureFace) {
                        shouldCaptureFace = false // Reset the flag
                        val scaledBitmap = Bitmap.createScaledBitmap(faceBitmap, 250, 250, false)
                        viewModel.setCapturedFace(scaledBitmap)
                        scope.launch {
                            Log.d("Register","Captured Face $scaledBitmap")
                        }
                    }
                }
            )
        }

    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview in background
        AndroidView(
            factory = {
                previewView.apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Content overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Face Registration Page",
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Status Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (similarityCheck) {
                        is FaceRegisterViewModel.SimilarityCheckResult.Similar -> {
                            Text(
                                text = "There's Exist User : ${(similarityCheck as FaceRegisterViewModel.SimilarityCheckResult.Similar).existingName}",
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        is FaceRegisterViewModel.SimilarityCheckResult.Unique -> {
                            Text(
                                text = "Face is ready for registration",
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        else -> {}
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Captured Face and Recognition Status
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 1.0f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    capturedFace?.let { face ->
                        Image(
                            bitmap = face.asImageBitmap(),
                            contentDescription = "Captured Face",
                            modifier = Modifier
                                .size(350.dp)
                                .padding(vertical = 8.dp)
                        )

                        Text(
                            text = "Debug Recognized: $recognizedName",
                            fontSize = 20.sp,
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Capture Again button
                        Button(
                            onClick = { shouldCaptureFace = true },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("Capture Again")
                        }
                    }

                    when (registrationState) {
                        is FaceRegisterViewModel.RegistrationState.Processing -> {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        is FaceRegisterViewModel.RegistrationState.Success -> {
                            Text(
                                text = (registrationState as FaceRegisterViewModel.RegistrationState.Success).message,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        is FaceRegisterViewModel.RegistrationState.Error -> {
                            Text(
                                text = (registrationState as FaceRegisterViewModel.RegistrationState.Error).message,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        else -> {}
                    }
                }
            }

            // Bottom Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (capturedFace == null) {
                    Button(
                        onClick = { shouldCaptureFace = true },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Capture Face")
                    }
                } else {
                    Button(
                        onClick = {
                            scope.launch {
                                capturedFace?.let { bitmap ->
                                    viewModel.registerFace(
                                        participantName,
                                        participantRole,
                                        participantPhone,
                                        bitmap
                                    )
                                }
                            }
                        },
                        enabled = similarityCheck !is FaceRegisterViewModel.SimilarityCheckResult.Similar &&
                                registrationState !is FaceRegisterViewModel.RegistrationState.Processing
                    ) {
                        if (registrationState is FaceRegisterViewModel.RegistrationState.Processing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Register Face")
                        }
                    }
                }
            }
        }
    }
}