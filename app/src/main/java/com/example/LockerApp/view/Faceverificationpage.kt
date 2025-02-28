package com.example.LockerApp.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.Log
import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.NavBackStackEntry
import com.example.LockerApp.repository.FaceAuthRepository
import com.example.LockerApp.utils.CameraManager
import com.example.LockerApp.viewmodel.FaceLoginViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@Composable
fun FaceVerificationPage(
    navController: NavController,
    viewModel: FaceLoginViewModel,
    expectedAccountId: Int,
    onVerificationSuccess: () -> Unit,
    onVerificationFailed: () -> Unit
) {
    // Get registration data from navigation arguments if they exist
    val name = navController.currentBackStackEntry?.arguments?.getString("name") ?: ""
    val role = navController.currentBackStackEntry?.arguments?.getString("role") ?: ""
    val phone = navController.currentBackStackEntry?.arguments?.getString("phone") ?: ""
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val cameraManager = remember { CameraManager(context) }
    val previewView = remember { PreviewView(context) }
    val scope = rememberCoroutineScope()

    var isVerifying by remember { mutableStateOf(false) }
    var verificationMessage by remember { mutableStateOf("Please look at the camera for verification") }
    var verificationSuccess by remember { mutableStateOf(false) }
    var verificationFailed by remember { mutableStateOf(false) }

    val loginState by viewModel.loginState.observeAsState(FaceLoginViewModel.LoginState.Scanning)

    // Handle login state changes for verification
    LaunchedEffect(loginState) {
        when (loginState) {
            is FaceLoginViewModel.LoginState.Success -> {
                val state = loginState as FaceLoginViewModel.LoginState.Success
                if (state.accountid == expectedAccountId) {
                    // Verification successful - same person as original login
                    verificationMessage = "Verification successful!"
                    verificationSuccess = true
                    delay(1500) // Give user time to see success message

                    // If we have registration data, navigate to face capture page
                    if (name.isNotEmpty() && role.isNotEmpty() && phone.isNotEmpty()) {
                        navController.navigate(
                            "face_capture?name=${name}&role=${role}&phone=${phone}/$expectedAccountId"
                        ) {
                            popUpTo("face_verification/${expectedAccountId}") { inclusive = true }
                        }
                    } else {
                        // Otherwise use the default success handler
                        onVerificationSuccess()
                    }
                } else {
                    // Verification failed - different person
                    verificationMessage = "Verification failed! User does not match original login."
                    verificationFailed = true
                    delay(1500) // Give user time to see failure message
                    onVerificationFailed()
                }
            }
            is FaceLoginViewModel.LoginState.Error -> {
                val state = loginState as FaceLoginViewModel.LoginState.Error
                verificationMessage = "Verification error: ${state.message}"
                verificationFailed = true
            }
            else -> {
                // Reset states
                verificationSuccess = false
                verificationFailed = false
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

                    // Resize to expected format for model
                    val resizedBitmap = Bitmap.createScaledBitmap(faceBitmap, 160, 160, false)

                    // Only process one face at a time
                    if (!isVerifying) {
                        isVerifying = true
                        viewModel.recognizeFace(resizedBitmap)
                        // Allow next verification after a short delay
                        scope.launch {
                            delay(500)
                            isVerifying = false
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
            text = "Face Verification",
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

        // Verification status card
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
                Text(
                    text = "Security Verification",
                    fontSize = 20.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Please verify it's you before adding a new user",
                    fontSize = 16.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = verificationMessage,
                    fontSize = 18.sp,
                    color = when {
                        verificationSuccess -> Color(0xFF4CAF50) // Green
                        verificationFailed -> Color(0xFFF44336) // Red
                        else -> Color.Unspecified
                    }
                )

                if (name.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "You're about to add: $name",
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { onVerificationFailed() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Cancel Verification")
                }
            }
        }
    }
}