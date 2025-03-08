package com.example.LockerApp.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.Image
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.LockerApp.R
import com.example.LockerApp.viewmodel.FaceLoginViewModel
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executor
import java.util.concurrent.Executors

// close all cam func
private fun shutdownCamera(provider: ProcessCameraProvider?, executor: Executor) {
    try {
        // whatever camera is active > deactivate
        provider?.unbindAll()
        if (executor is java.util.concurrent.ExecutorService) {
            executor.shutdown()
        }
    } catch (e: Exception) {
        Log.e("FaceLoginOverlay", "Error shutting down camera", e)
    }
}

@Composable
fun FaceLoginOverlay(
    navController: NavController,
    viewModel: FaceLoginViewModel,
    onDismiss: () -> Unit,
    onLoginSuccess: (Int, String, String, String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    // Camera setup
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember { PreviewView(context) }

    // Face recognition state
    var isLoginSuccessful by remember { mutableStateOf(false) }
    val loginState by viewModel.loginState.observeAsState(FaceLoginViewModel.LoginState.Scanning)

    // Animation compositions
    val idleComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.idle))
    val successComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.success))
    val failedComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.failed))

    // Animation state
    var isScanningVisible by remember { mutableStateOf(true) }
    var isSuccessVisible by remember { mutableStateOf(false) }
    var isFailedVisible by remember { mutableStateOf(false) }
    val errorcolor = Color(0xFFff9796)
    // Handle login state changes
    LaunchedEffect(loginState) {
        when (loginState) {
            is FaceLoginViewModel.LoginState.Scanning -> {
                isScanningVisible = true
                isSuccessVisible = false
                isFailedVisible = false
            }
            is FaceLoginViewModel.LoginState.Success -> {
                if (!isLoginSuccessful) {
                    isScanningVisible = false
                    isSuccessVisible = true
                    isFailedVisible = false
                    isLoginSuccessful = true
                    delay(1500) // Show success animation briefly
                    val state = loginState as FaceLoginViewModel.LoginState.Success
                    shutdownCamera(cameraProvider, cameraExecutor)
                    // Deactivate recognition to prevent stale data when coming back
                    viewModel.resetToScanning()
                    onLoginSuccess(state.accountid, state.name, state.role, state.phone)
                }
            }
            is FaceLoginViewModel.LoginState.Error -> {
                isScanningVisible = false
                isSuccessVisible = false
                isFailedVisible = true
                isLoginSuccessful = false
                delay(3000) // Show error animation briefly
                // Reset to scanning state after showing error
                viewModel.resetToScanning()
            }
        }
    }

    // Get camera provider on initialization
    LaunchedEffect(Unit) {
        try {
            cameraProvider = cameraProviderFuture.get()
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
        } catch (e: Exception) {
            Log.e("FaceLoginOverlay", "Failed to get camera provider", e)
        }
    }

    // Handle dismiss with proper camera shutdown
    val handleDismiss = {
        shutdownCamera(cameraProvider, cameraExecutor)
        // Deactivate recognition when dismissing to prevent stale data
        viewModel.resetToScanning()
        onDismiss()
    }

    // Cleanup when the composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            shutdownCamera(cameraProvider, cameraExecutor)
        }
    }

    // Dialog UI - now completely transparent
    Dialog(
        onDismissRequest = handleDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            // Hidden camera preview (still active for face detection)
            AndroidView(
                factory = { previewView.apply {
                    layoutParams = ViewGroup.LayoutParams(1, 1) // Tiny size to hide it
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                } },
                modifier = Modifier.size(1.dp) // Minimal size
            )

            // Centered content column
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .wrapContentHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(

                ){
                    // Animation container
                    Box(
                        modifier = Modifier
                            .width(width = 220.dp)
                            .clip(RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp) )
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        // Idle/Scanning animation
                        if (isScanningVisible) {
                            LottieAnimation(
                                composition = idleComposition,
                                iterations = LottieConstants.IterateForever,
                                modifier = Modifier.size(220.dp)
                            )
                        }

                        // Success animation
                        if (isSuccessVisible) {
                            LottieAnimation(
                                composition = successComposition,
                                iterations = 1,
                                modifier = Modifier.size(220.dp)
                            )
                        }

                        // Failed animation
                        if (isFailedVisible) {
                            LottieAnimation(
                                composition = failedComposition,
                                iterations = 1,
                                modifier = Modifier.size(220.dp)
                            )
                        }
                    }
                }
                Row(){
                    Card(
                        shape = RoundedCornerShape(0.dp),
                        modifier = Modifier
                            .width(width = 220.dp)
                            .wrapContentHeight(),

                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            when (loginState) {
                                is FaceLoginViewModel.LoginState.Scanning -> {
                                    Text(
                                        text = "กำลังค้นหา...",
                                        fontSize = 20.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                is FaceLoginViewModel.LoginState.Success -> {
                                    val state = loginState as FaceLoginViewModel.LoginState.Success
                                    Text(
                                        text = "กำลังยืนยันตัวตน",
                                        fontSize = 20.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }
                                is FaceLoginViewModel.LoginState.Error -> {
                                    val state = loginState as FaceLoginViewModel.LoginState.Error
                                    Text(
                                        text = state.message,
                                        fontSize = 20.sp,
                                        color = Color.Red,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .width(width = 220.dp)
                        .clip(RoundedCornerShape(bottomStart = 25.dp, bottomEnd = 25.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        ,
                    horizontalArrangement = Arrangement.Center,
                ){
                    TextButton(
                        onClick = handleDismiss,
                        modifier = Modifier.weight(1f)
                            ,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color.White)

                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@SuppressLint("UnsafeOptInUsageError")
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

        // Try to use the front camera
        val cameraSelector = try {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } catch (e: Exception) {
            Log.w("FaceLoginOverlay", "Front camera not available but let's try again", e)
            CameraSelector.DEFAULT_FRONT_CAMERA
        }

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            )
        } catch (e: Exception) {
            Log.w("FaceLoginOverlay", "Failed to bind specific camera, trying generic selector", e)
            val genericSelector = CameraSelector.Builder().build()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                genericSelector,
                preview,
                imageAnalysis
            )
        }
    } catch (e: Exception) {
        Log.e("FaceLoginOverlay", "Camera setup failed", e)
    }
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
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
                Log.e("FaceLoginOverlay", "Face detection failed", e)
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}

fun mediaImageToBitmap(mediaImage: Image, rotationDegrees: Int): Bitmap {
    val width = mediaImage.width
    val height = mediaImage.height
    // Get the YUV planes
    val yPlane = mediaImage.planes[0]
    val uPlane = mediaImage.planes[1]
    val vPlane = mediaImage.planes[2]
    // Get plane buffers
    val yBuffer = yPlane.buffer
    val uBuffer = uPlane.buffer
    val vBuffer = vPlane.buffer
    // Get plane pixels strides
    val yPixelStride = yPlane.pixelStride
    val yRowStride = yPlane.rowStride
    val uPixelStride = uPlane.pixelStride
    val uRowStride = uPlane.rowStride
    val vPixelStride = vPlane.pixelStride
    val vRowStride = vPlane.rowStride
    // Create output buffer
    val outputArray = IntArray(width * height)
    var outputIndex = 0
    for (y in 0 until height) {
        val yRowIndex = y * yRowStride
        val uvRowIndex = (y shr 1) * uRowStride
        for (x in 0 until width) {
            val uvx = x shr 1
            // Extract YUV values
            val yValue = yBuffer.get(yRowIndex + x * yPixelStride).toInt() and 0xFF
            val uValue = uBuffer.get(uvRowIndex + uvx * uPixelStride).toInt() and 0xFF
            val vValue = vBuffer.get(uvRowIndex + uvx * vPixelStride).toInt() and 0xFF
            // YUV to RGB conversion
            var r = yValue + (1.370705f * (vValue - 128)).toInt()
            var g = yValue - (0.698001f * (vValue - 128)).toInt() - (0.337633f * (uValue - 128)).toInt()
            var b = yValue + (1.732446f * (uValue - 128)).toInt()
            // Clamp RGB values
            r = r.coerceIn(0, 255)
            g = g.coerceIn(0, 255)
            b = b.coerceIn(0, 255)
            // Pack RGB into output pixel
            outputArray[outputIndex++] = 0xff000000.toInt() or (r shl 16) or (g shl 8) or b
        }
    }
    // Create bitmap from the RGB array
    var bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    bitmap.setPixels(outputArray, 0, width, 0, 0, width, height)
    // Apply rotation if needed
    if (rotationDegrees != 0) {
        val matrix = Matrix().apply {
            postRotate(rotationDegrees.toFloat())
        }
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
    return bitmap
}