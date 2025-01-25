package com.example.LockerApp.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.media.Image
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.example.LockerApp.viewmodel.FaceLoginViewModel
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import kotlinx.coroutines.delay

@SuppressLint("UnsafeOptInUsageError")
@Composable
fun FaceLoginPage(
    navController: NavController,
    viewModel: FaceLoginViewModel,
    onLoginSuccess: (String, String, String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember { androidx.camera.view.PreviewView(context) }

    var isLoginSuccessful by remember { mutableStateOf(false) }
    val loginState by viewModel.loginState.observeAsState(FaceLoginViewModel.LoginState.Scanning)

    // Handle login state changes
    LaunchedEffect(loginState) {
        when (loginState) {
            is FaceLoginViewModel.LoginState.Success -> {
                val state = loginState as FaceLoginViewModel.LoginState.Success
                if (!isLoginSuccessful) {
                    isLoginSuccessful = true
                    delay(3000)
                    onLoginSuccess(state.name, state.role, state.phone)
                }
            }
            else -> {} // Handle other states if needed
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Face Login",
            fontSize = 24.sp,
            modifier = Modifier.padding(16.dp)
        )

        AndroidView(
            factory = {
                previewView.apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp)
        )

        // Recognition status card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (loginState) {
                    is FaceLoginViewModel.LoginState.Scanning -> {
                        Text(
                            text = "Scanning...",
                            fontSize = 20.sp
                        )
                    }
                    is FaceLoginViewModel.LoginState.Success -> {
                        val state = loginState as FaceLoginViewModel.LoginState.Success
                        Text(
                            text = "Recognized User: ${state.name}",
                            fontSize = 20.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Role: ${state.role}",
                            fontSize = 16.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = "Phone: ${state.phone}",
                            fontSize = 16.sp
                        )
                    }
                    is FaceLoginViewModel.LoginState.Error -> {
                        val state = loginState as FaceLoginViewModel.LoginState.Error
                        Text(
                            text = state.message,
                            fontSize = 20.sp,
                            color = Color.Red
                        )
                    }
                }
            }
        }

        // Camera Setup
        LaunchedEffect(cameraProviderFuture) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) {
                setupCamera(
                    context = context,
                    lifecycleOwner = lifecycleOwner,
                    cameraProviderFuture = cameraProviderFuture,
                    previewView = previewView,
                    cameraExecutor = cameraExecutor,
                    onFaceBitmapCaptured = { bitmap ->
                        viewModel.recognizeFace(bitmap)
                    }
                )
            }
        }
    }
}

private suspend fun setupCamera(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    cameraProviderFuture: ListenableFuture<ProcessCameraProvider>,
    previewView: PreviewView,
    cameraExecutor: Executor,
    onFaceBitmapCaptured: (Bitmap) -> Unit
) {
    try {
        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder()
            .build()
            .also { it.setSurfaceProvider(previewView.surfaceProvider) }

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .apply {
                setAnalyzer(cameraExecutor) { imageProxy ->
                    analyzeImage(
                        imageProxy = imageProxy,
                        onFaceBitmapCaptured = onFaceBitmapCaptured
                    )
                }
            }

        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageAnalysis
        )
    } catch (e: Exception) {
        Log.e("FaceLogin", "Camera setup failed", e)
    }
}

@OptIn(ExperimentalGetImage::class)
private fun analyzeImage(
    imageProxy: ImageProxy,
    onFaceBitmapCaptured: (Bitmap) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val image = InputImage.fromMediaImage(mediaImage, rotationDegrees)

        val faceDetector = FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build()
        )

        faceDetector.process(image)
            .addOnSuccessListener { faces ->
                if (faces.isNotEmpty()) {
                    val face = faces.first()
                    val bitmap = mediaImageToBitmap(mediaImage, rotationDegrees)
                    val faceBitmap = Bitmap.createBitmap(
                        bitmap,
                        face.boundingBox.left.coerceAtLeast(0),
                        face.boundingBox.top.coerceAtLeast(0),
                        face.boundingBox.width().coerceAtMost(bitmap.width - face.boundingBox.left),
                        face.boundingBox.height().coerceAtMost(bitmap.height - face.boundingBox.top)
                    )
                    val resizedFaceBitmap = Bitmap.createScaledBitmap(faceBitmap, 160, 160, false)
                    onFaceBitmapCaptured(resizedFaceBitmap)
                }
                imageProxy.close()
            }
            .addOnFailureListener { e ->
                Log.e("FaceLogin", "Face detection failed", e)
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}