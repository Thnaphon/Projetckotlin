package com.example.LockerApp.view

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.camera.view.PreviewView
import androidx.navigation.NavController
import com.example.LockerApp.utils.CameraManager
import com.example.LockerApp.viewmodel.FaceRegisterViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@Composable
fun FaceCapturePage(
    navController: NavController,
    viewModel: FaceRegisterViewModel,
    participantName: String,
    participantRole: String,
    participantPhone: String,
    accountid: Int
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val cameraManager = remember { CameraManager(context) }
    val previewView = remember { PreviewView(context) }

    val recognizedName by viewModel.recognizedName.observeAsState("")
    var capturingFace by remember { mutableStateOf(false) }
    var capturedFace by remember { mutableStateOf<Bitmap?>(null) }
    var countdownSeconds by remember { mutableStateOf(5) }
    var isCountingDown by remember { mutableStateOf(false) }
    var originalBrightness by remember { mutableStateOf(-1f) }
    val similarityCheck by viewModel.similarityCheck.observeAsState()

    // Increase screen brightness to 100%
    LaunchedEffect(Unit) {
        val activity = context as? ComponentActivity
        activity?.let {
            val window = it.window
            // Store original brightness
            val layoutParams = window.attributes
            originalBrightness = layoutParams.screenBrightness

            // Set to maximum brightness
            layoutParams.screenBrightness = 1.0f
            window.attributes = layoutParams
        }
    }

    // Restore original brightness when leaving
    DisposableEffect(Unit) {
        onDispose {
            val activity = context as? ComponentActivity
            activity?.let {
                val window = it.window
                // Restore original brightness
                val layoutParams = window.attributes
                if (originalBrightness >= 0) {
                    layoutParams.screenBrightness = originalBrightness
                    window.attributes = layoutParams
                }
            }
        }
    }

    // Track face detection state to handle face changes
    var faceDetected by remember { mutableStateOf(false) }
    var previousRecognizedName by remember { mutableStateOf("") }

    // Reset state when recognizedName changes (face in/out of frame)
    LaunchedEffect(recognizedName) {
        // If we had a name before and now we don't, a face left the frame
        if (previousRecognizedName.isNotEmpty() && recognizedName.isEmpty()) {
            // Reset counting state for new face detection
            isCountingDown = false
            // Reset countdown for next face
            countdownSeconds = 5
        }
        // Update previous name for next comparison
        previousRecognizedName = recognizedName
    }

    // Detection logic that runs continuously
    LaunchedEffect(recognizedName, faceDetected) {
        // Start countdown if:
        // 1. Face is detected (faceDetected is true)
        // 2. Face is not in database (recognizedName is empty)
        // 3. Not already counting down
        // 4. Not in capturing process
        // 5. No face already captured
        if (faceDetected &&
            recognizedName.isEmpty() &&
            !isCountingDown &&
            !capturingFace &&
            capturedFace == null) {

            isCountingDown = true
            countdownSeconds = 5

            // Countdown loop
            while (countdownSeconds > 0 && isCountingDown) {
                delay(1000)
                countdownSeconds--

                // If during countdown we detect a face in database, cancel countdown
                if (recognizedName.isNotEmpty()) {
                    isCountingDown = false
                    break
                }
            }

            // Auto-capture after countdown if still counting down and no match
            if (isCountingDown && recognizedName.isEmpty()) {
                capturingFace = true
                isCountingDown = false
            }
        }
    }

    // Handle auto-capture
    LaunchedEffect(capturingFace) {
        if (capturingFace) {
            delay(200) // Small delay to ensure clear capture
            capturingFace = false
        }
    }

    // Keep track of last time a face was detected
    var lastFaceDetectionTime by remember { mutableStateOf(0L) }

    // Check for face absence (if no face detected for a period)
    LaunchedEffect(Unit) {
        while (true) {
            delay(500) // Check every half second
            val currentTime = System.currentTimeMillis()

            // If no face detected for more than 1 second and a face was previously detected
            if (currentTime - lastFaceDetectionTime > 1000 && faceDetected) {
                faceDetected = false
                // Reset counting state when face leaves frame
                if (isCountingDown && capturedFace == null) {
                    isCountingDown = false
                    countdownSeconds = 5
                }
            }
        }
    }

    // Setup camera
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

                    // Update face detection state and timestamp
                    faceDetected = true
                    lastFaceDetectionTime = System.currentTimeMillis()

                    // Always perform recognition
                    viewModel.recognizeFace(faceBitmap)

                    // If capture is requested, save the face
                    if (capturingFace) {
                        // Create a clean scaled copy for registration
                        val scaledBitmap = Bitmap.createScaledBitmap(faceBitmap, 250, 250, false)
                        capturedFace = scaledBitmap
                        isCountingDown = false
                    }
                }
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
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

        // Overlay UI
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title
            Text(
                text = "Face Capture",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .background(
                        Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Face guide outline - using standard border instead of custom one
            Box(
                modifier = Modifier
                    .size(250.dp)
                    .clip(CircleShape)
                    .border(
                        width = 4.dp,
                        color = when {
                            capturedFace != null -> Color.Green
                            recognizedName.isNotEmpty() -> Color.Red
                            isCountingDown -> Color(0xFFFFA000) // Amber
                            else -> Color.White
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (capturedFace != null) {
                    Image(
                        bitmap = capturedFace!!.asImageBitmap(),
                        contentDescription = "Captured Face",
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (isCountingDown) {
                    Text(
                        text = countdownSeconds.toString(),
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Status message
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.7f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when {
                        capturedFace != null -> {
                            Text(
                                text = "Face Captured!",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Green
                            )
                            Text(
                                text = "Ready to register $participantName",
                                fontSize = 16.sp,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        }
                        recognizedName.isNotEmpty() -> {
                            Text(
                                text = "Face Already Registered",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Red
                            )
                            Text(
                                text = "This face is already registered as: $recognizedName",
                                fontSize = 16.sp,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        }
                        isCountingDown -> {
                            Text(
                                text = "New Face Detected",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFA000) // Amber
                            )
                            Text(
                                text = "Auto-capturing in $countdownSeconds seconds...",
                                fontSize = 16.sp,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        }
                        else -> {
                            Text(
                                text = "Looking for Face...",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Please center your face in the circle",
                                fontSize = 16.sp,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bottom buttons row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { navController.popBackStack() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red.copy(alpha = 0.8f)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }

                Spacer(modifier = Modifier.width(16.dp))

                if (capturedFace == null) {
                    Button(
                        onClick = { capturingFace = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Blue.copy(alpha = 0.8f)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Capture Now")
                    }
                } else {
                    Button(
                        onClick = {
                            capturedFace?.let { bitmap ->
                                // Set captured face in ViewModel
                                viewModel.setCapturedFace(bitmap)

                                // Register the face
                                scope.launch {
                                    viewModel.registerFace(
                                        participantName,
                                        participantRole,
                                        participantPhone,
                                        bitmap
                                    )
                                }

                                // Navigate back to main menu
                                navController.navigate("main_menu/$accountid") {
                                    popUpTo("face_register") { inclusive = true }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Green.copy(alpha = 0.8f)
                        ),
                        modifier = Modifier.weight(1f),
                        enabled = capturedFace != null &&
                                similarityCheck !is FaceRegisterViewModel.SimilarityCheckResult.Similar
                    ) {
                        Text("Register Face")
                    }
                }
            }
        }
    }
}