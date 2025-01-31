package com.example.LockerApp.view

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
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
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

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
    var shouldCaptureFace by remember { mutableStateOf(false) }

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
                            Toast.makeText(context, "Face captured!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Face Detection Page",
            fontSize = 24.sp,
            modifier = Modifier.padding(16.dp)
        )

        AndroidView(
            factory = { previewView },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp)
        )

        capturedFace?.let { face ->
            Image(
                bitmap = face.asImageBitmap(),
                contentDescription = "Captured Face",
                modifier = Modifier
                    .size(250.dp)
                    .padding(16.dp)
            )
        }

        Text(
            text = "Debug Recognized: $recognizedName",
            fontSize = 20.sp,
            modifier = Modifier.padding(16.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = {
                scope.launch {
                    capturedFace?.let { bitmap ->
                        viewModel.registerFace(
                            participantName,
                            participantRole,
                            participantPhone,
                            bitmap
                        )
                    } ?: run {
                        // Show error if no face is captured
                        Toast.makeText(context, "Please capture a face first", Toast.LENGTH_SHORT).show()
                    }
                }
            }) {
                Text("Register Face")
            }

            Button(onClick = {
                // Mark that we want to capture the next detected face
                shouldCaptureFace = true
            }) {
                Text("Capture Face")
            }
        }

        // Show registration status
        registrationState?.let { state ->
            when (state) {
                is FaceRegisterViewModel.RegistrationState.Success -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                is FaceRegisterViewModel.RegistrationState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}