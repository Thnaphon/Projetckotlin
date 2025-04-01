package com.Locker.LockerApp.view

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.Locker.LockerApp.R
import com.Locker.LockerApp.utils.CameraManager
import com.Locker.LockerApp.viewmodel.FaceLoginViewModel
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

@Composable
fun FaceVerificationOverlay(
    navController: NavController,
    viewModel: FaceLoginViewModel,
    expectedAccountId: Int,
    adminname : String = "",
    adminrole : String = "",
    name: String = "",
    role: String = "",
    phone: String = "",
    onDismiss: () -> Unit,
    onVerificationSuccess: () -> Unit,
    onVerificationFailed: () -> Unit
) {
    Log.d("value", "$adminname , $adminrole , $name , $role , $phone")
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraManager = remember { CameraManager(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember { androidx.camera.view.PreviewView(context) }

    // Face recognition state
    var isVerificationSuccessful by remember { mutableStateOf(false) }
    var isVerificationFailed by remember { mutableStateOf(false) }
    val loginState by viewModel.loginState.observeAsState(FaceLoginViewModel.LoginState.Scanning)

    // Animation compositions
    val idleComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.idle))
    val successComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.success))
    val failedComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.failed))

    // Animation state
    var isScanningVisible by remember { mutableStateOf(true) }
    var isSuccessVisible by remember { mutableStateOf(false) }
    var isFailedVisible by remember { mutableStateOf(false) }
    var verificationMessage by remember { mutableStateOf("Scanning") }

    // Handle login state changes for verification
    LaunchedEffect(loginState) {
        when (loginState) {
            is FaceLoginViewModel.LoginState.Scanning -> {
                isScanningVisible = true
                isSuccessVisible = false
                isFailedVisible = false
                verificationMessage = "Scanning"
            }
            is FaceLoginViewModel.LoginState.Success -> {
                val state = loginState as FaceLoginViewModel.LoginState.Success
                if (state.accountid == expectedAccountId) {

                    // Verification successful
                    isScanningVisible = false
                    isSuccessVisible = true
                    isFailedVisible = false
                    isVerificationSuccessful = true
                    verificationMessage = "Success"

                    delay(1500) // Show success animation briefly

                    // Clean up before navigation
                    cameraManager.shutdown()
                    viewModel.resetToScanning()

                    // Navigate based on the verification purpose
                    if (name.isNotEmpty() && role.isNotEmpty() && phone.isNotEmpty()) {
                        // Registration flow
                        navController.navigate(
                            "face_capture/$expectedAccountId/$adminname/$adminrole?name=${name}&role=${role}&phone=${phone}"
                        ) {
                            popUpTo("face_verification/${expectedAccountId}") { inclusive = true }
                        }
                    } else {
                        // Standard verification flow
                        onVerificationSuccess()
                    }
                } else {
                    // Verification failed
                    isScanningVisible = false
                    isSuccessVisible = false
                    isFailedVisible = true
                    isVerificationFailed = true
                    verificationMessage = "Denied Different Face"

                    delay(3000) // Show error animation briefly

                    // Reset to scanning state after showing error
                    viewModel.resetToScanning()
                    onVerificationFailed()
                }
            }
            is FaceLoginViewModel.LoginState.Error -> {
                val state = loginState as FaceLoginViewModel.LoginState.Error
                isScanningVisible = false
                isSuccessVisible = false
                isFailedVisible = true
                verificationMessage = "Failed: ${state.message}"

                delay(3000) // Show error briefly
                viewModel.resetToScanning() // Reset to scanning state
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

    // Dialog UI
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
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            //Camera Preview
            AndroidView(
                factory = { previewView.apply {
                    layoutParams = android.view.ViewGroup.LayoutParams(1, 1) // Tiny size to hide it
                    implementationMode = androidx.camera.view.PreviewView.ImplementationMode.COMPATIBLE
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
                Row {
                    // Animation container
                    Box(
                        modifier = Modifier
                            .width(width = 220.dp)
                            .clip(RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp))
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

                Row {
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
                            Text(
                                text = "Verification",
                                fontSize = 18.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Text(
                                text = verificationMessage,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center,
                                color = when {
                                    isSuccessVisible -> Color(0xFF4CAF50)
                                    isFailedVisible -> Color.Red
                                    else -> Color.Unspecified
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .width(width = 220.dp)
                        .clip(RoundedCornerShape(bottomStart = 25.dp, bottomEnd = 25.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TextButton(
                        onClick = handleDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Text("Back to mainmenu")
                    }
                }
            }
        }
    }
}