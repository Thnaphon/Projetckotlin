package com.example.LockerApp.view

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.LockerApp.R
import com.example.LockerApp.utils.CameraManager
import com.example.LockerApp.viewmodel.FaceLoginViewModel
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

@Composable
fun FaceLoginOverlay(
    navController: NavController,
    viewModel: FaceLoginViewModel,
    onDismiss: () -> Unit,
    onLoginSuccess: (Int, String, String, String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val cameraManager = remember { CameraManager(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember { androidx.camera.view.PreviewView(context) }

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
                    cameraManager.shutdown() // Use our manager's shutdown method
                    // Deactivate recognition to prevent stale data
                    viewModel.resetToScanning()
                    onLoginSuccess(state.accountid, state.name, state.role, state.phone)
                }
            }
            is FaceLoginViewModel.LoginState.Error -> {
                isScanningVisible = false
                isSuccessVisible = false
                isFailedVisible = true
                isLoginSuccessful = false
                delay(1500) // Show error animation briefly
                // Reset to scanning state after showing error
                viewModel.resetToScanning()
            }
        }
    }

    // Initialize camera
    LaunchedEffect(Unit) {
        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) {
                cameraManager.startCameraForOverlay(
                    lifecycleOwner = lifecycleOwner,
                    previewView = previewView,
                    cameraExecutor = cameraExecutor,
                    onFaceBitmapCaptured = { faceBitmap ->
                        viewModel.recognizeFace(faceBitmap)
                    }
                )
            }
        } catch (e: Exception) {
            // Handle camera errors
        }
    }

    // Handle dismiss with proper camera shutdown
    val handleDismiss = {
        cameraManager.shutdown()
        // Deactivate recognition when dismissing to prevent stale data
        viewModel.resetToScanning()
        onDismiss()
    }

    // Cleanup when the composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            cameraManager.shutdown()
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
            //camera preview
            AndroidView(
                factory = { previewView.apply {
                    layoutParams = android.view.ViewGroup.LayoutParams(1, 1) // Tiny size to hide it
                    implementationMode = androidx.camera.view.PreviewView.ImplementationMode.COMPATIBLE
                } },
                modifier = Modifier.size(1.dp) // Minimal size
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .wrapContentHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Animation container
                Row {
                    Box(
                        modifier = Modifier
                            .width(width = 220.dp)
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
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

                // Status card
                Row {
                    Card(
                        shape = RoundedCornerShape(0.dp),
                        modifier = Modifier
                            .width(220.dp)
                            .wrapContentHeight(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        )
                    )
                    {
                        Column(
                            modifier = Modifier.padding(top = 2.dp, bottom = 25.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Top
                        ) {
                            when (loginState) {
                                is FaceLoginViewModel.LoginState.Scanning -> {
                                    Text(
                                        modifier = Modifier.fillMaxWidth(),
                                        text = "Scanning...",
                                        fontSize = 16.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                is FaceLoginViewModel.LoginState.Success -> {
                                    val state = loginState as FaceLoginViewModel.LoginState.Success
                                    Text(
                                        text = "Verifing...",
                                        fontSize = 16.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(bottom = 8.dp).fillMaxWidth(),
                                    )
                                }
                                is FaceLoginViewModel.LoginState.Error -> {
                                    val state = loginState as FaceLoginViewModel.LoginState.Error
                                    Text(
                                        modifier = Modifier.fillMaxWidth(),
                                        text = state.message,
                                        fontSize = 16.sp,
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
                        .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                        .background(Color(0xFF3961AA)),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TextButton(
                        onClick = handleDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Text("Cancel", color = Color.White)
                    }
                }
            }
        }
    }
}