package com.example.LockerApp.view

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.camera.view.PreviewView
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.navigation.NavController
import com.example.LockerApp.R
import com.example.LockerApp.utils.CameraManager
import com.example.LockerApp.viewmodel.FaceRegisterViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@Composable
fun FaceCapturePage(
    navController: NavController,
    viewModel: FaceRegisterViewModel,
    adminname: String,
    adminrole: String,
    participantName: String,
    participantRole: String,
    participantPhone: String,
    accountid: Int
) {
    //camera variable setup
    Log.d("value", "$adminname , $adminrole , $participantName , $participantRole , $participantPhone")
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val cameraManager = remember { CameraManager(context) }
    val previewView = remember { PreviewView(context) }

    //variable relate to making register
    val recognizedName by viewModel.recognizedName.observeAsState("")
    var insufficientLandmarks by remember { mutableStateOf(false) }
    var capturingFace by remember { mutableStateOf(false) }
    var capturedFace by remember { mutableStateOf<Bitmap?>(null) }
    var countdownSeconds by remember { mutableStateOf(5) }
    var isCountingDown by remember { mutableStateOf(false) }

    //brightness of user
    var originalBrightness by remember { mutableStateOf(-1f) }


    // Camera state
    var isCameraActive by remember { mutableStateOf(false) }
    var cameraInitialized by remember { mutableStateOf(false) }
    var isSurfaceReady by remember { mutableStateOf(false) }

    // state for face position inside rectangle
    var faceDetected by remember { mutableStateOf(false) }
    var isFaceWithinBoundary by remember { mutableStateOf(false) }
    var faceRect by remember { mutableStateOf<RectF?>(null) }
    var imageWidth by remember { mutableStateOf(0) }
    var imageHeight by remember { mutableStateOf(0) }

    // Clear any previous face capture and prepare camera with proper delay
    LaunchedEffect(Unit) {

        //set state is captured to mt
        viewModel.setCapturedFace(null)

        // Add a delay to ensure previous camera resources are released
        delay(750)
        isCameraActive = true
        Log.d("FaceCapturePage", "Setting camera active after initialization delay")
    }

    // Increase screen brightness to 100%
    LaunchedEffect(Unit) {
        delay(500)
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
    var previousRecognizedName by remember { mutableStateOf("") }

    // Reset state when recognizedName changes (face in and out of frame)
    LaunchedEffect(recognizedName) {

        // If got a name before and now no more or a face left the frame
        if (previousRecognizedName.isNotEmpty() && recognizedName.isEmpty()) {

            // Reset counting state for new face detection incase not save
            isCountingDown = false

            // Reset countdown for next face
            countdownSeconds = 5
        }

        // Hide countdown if user is in database
        if (recognizedName.isNotEmpty()) {
            isCountingDown = false
        }

        // Update previous name for next comparison
        previousRecognizedName = recognizedName
    }

    // main logic of faceboundary and face in frame
    LaunchedEffect(isFaceWithinBoundary, recognizedName, faceDetected, insufficientLandmarks) {

        /*

            Start countdown for capture the face in sequence by if:
         1. Face is within boundary rectangle
         2. Face is detected (That must be face of something Error of my project is
            you can try something weird like some cartoon character , anime and
            some kind of something block half a face
         3. Face is not in database (recognizedName is empty)
         4. Not already counting down ( the state of counting down isCountingDown)
         5. Not in capturing process
         6. face not enough information like ear , eye , mouth and nose
         7. No face already captured
         8. Camera Must active

         */

        if (isFaceWithinBoundary &&
            faceDetected &&
            !insufficientLandmarks &&
            recognizedName.isEmpty() &&
            !isCountingDown &&
            !capturingFace &&
            capturedFace == null &&
            isCameraActive
        ) {

            isCountingDown = true
            countdownSeconds = 5

            // Countdown loop
            while (countdownSeconds > 0 && isCountingDown && isFaceWithinBoundary && !insufficientLandmarks) {
                delay(1000)
                countdownSeconds--

                // If during countdown we detect a face in database or face leaves boundary, insufficientLandmarks , cancel countdown
                if (recognizedName.isNotEmpty() || !isFaceWithinBoundary || insufficientLandmarks) {
                    isCountingDown = false
                    break
                }
            }

            // Auto-capture after countdown if still counting down, no match, and face within boundary
            if (isCountingDown && recognizedName.isEmpty() && isFaceWithinBoundary && !insufficientLandmarks) {
                capturingFace = true
                isCountingDown = false
            }
        } else if (!isFaceWithinBoundary && isCountingDown) {
            // Cancel countdown if face moves out of boundary
            isCountingDown = false
            countdownSeconds = 5
        }
    }

    // Handle auto-capture
    LaunchedEffect(capturingFace) {
        if (capturingFace) {

            // Small delay to ensure clear capture
            delay(200)
            capturingFace = false
        }
    }

    // Effect for turning off camera when face is captured
    // And auto-register face and navigate to main menu
    LaunchedEffect(capturedFace) {
        if (capturedFace != null) {
            isCameraActive = false

            // Auto-register after a short delay (to show the captured face)

            capturedFace?.let { bitmap ->

                // Set captured face in ViewModel
                viewModel.setCapturedFace(bitmap)

                // Register the face
                viewModel.registerFace(
                    participantName,
                    participantRole,
                    participantPhone,
                    bitmap
                )

                // Navigate back to main menu after capture and preview

                delay(100) //delay to show the captured image

                navController.navigate("main_menu/$accountid/$adminname/$adminrole") {
                    popUpTo("face_capture") { inclusive = true }
                }
            }
        }
    }

    // Keep track of last time a face was detected
    var lastFaceDetectionTime by remember { mutableStateOf(0L) }

    // Check face
    LaunchedEffect(Unit) {
        while (true) {

            delay(750) // Check every 3/4 second
            val currentTime = System.currentTimeMillis()

            //reset counting when insufficent landmark
            if (insufficientLandmarks && isCountingDown) {
                isCountingDown = false
                countdownSeconds = 5
            }

            // If no face detected for more than one second and a face was previously detected
            if (currentTime - lastFaceDetectionTime > 1000 && faceDetected) {
                faceDetected = false
                isFaceWithinBoundary = false

                // Reset counting state when face leaves frame
                if (isCountingDown && capturedFace == null) {
                    isCountingDown = false
                    countdownSeconds = 5
                }
            }



        }
    }

    // Setup camera with proper initialization sequence and lifecycle handling
    LaunchedEffect(isCameraActive) {
        if (isCameraActive && !cameraInitialized) {
            try {
                Log.d("FaceCapturePage", "Initializing camera")

                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    delay(500)

                    // Ensure we're still active after the delay
                    if (!isCameraActive) {
                        Log.d(
                            "FaceCapturePage",
                            "Camera no longer active after delay, aborting setup"
                        )
                        return@LaunchedEffect
                    }

                    // Use a coroutine to manage the camera
                    scope.launch {
                        try {
                            cameraManager.startCamera(
                                lifecycleOwner = lifecycleOwner,
                                previewView = previewView,
                                cameraExecutor = cameraExecutor,
                                onFaceDetected = { bitmap, rect ->
                                    if (!isCameraActive) return@startCamera

                                    val faceBitmap = Bitmap.createBitmap(
                                        bitmap,
                                        rect.left.coerceAtLeast(0),
                                        rect.top.coerceAtLeast(0),
                                        rect.width().coerceAtMost(bitmap.width - rect.left),
                                        rect.height().coerceAtMost(bitmap.height - rect.top)
                                    )

                                    val faceWidth = rect.width()
                                    val faceHeight = rect.height()
                                    val screenWidth = bitmap.width
                                    val screenHeight = bitmap.height

                                    // Calculate face size as percentage of screen
                                    val faceWidthPercentage = (faceWidth.toFloat() / screenWidth) * 100
                                    val faceHeightPercentage = (faceHeight.toFloat() / screenHeight) * 100

                                    // Set thresholds
                                    val tooCloseThreshold = 70f  // Face takes up more than 70% of frame height
                                    val tooFarThreshold = 30f    // Face takes up less than 30% of frame height

                                    // Determine position state
                                    var faceTooClose = faceHeightPercentage > tooCloseThreshold
                                    var faceTooFar = faceHeightPercentage < tooFarThreshold

                                    Log.d("FaceCapturePage", "Face height percentage: $faceHeightPercentage% , Face width percentage: $faceWidthPercentage%")

                                    // Set the detected dimensions
                                    imageWidth = bitmap.width
                                    imageHeight = bitmap.height

                                    // Update face detection state and timestamp
                                    faceDetected = true
                                    lastFaceDetectionTime = System.currentTimeMillis()

                                    // Update face rectangle
                                    faceRect = RectF(
                                        rect.left.toFloat(),
                                        rect.top.toFloat(),
                                        rect.right.toFloat(),
                                        rect.bottom.toFloat()
                                    )

                                    // Check if face is within boundary rectangle
                                    val boundaryRect = RectF(
                                        bitmap.width * 0.25f,  // 25% from left
                                        bitmap.height * 0.1f,  // 10% from top
                                        bitmap.width * 0.75f,  // 75% from left (50% width)
                                        bitmap.height * 0.9f   // 90% from top (80% height)
                                    )

                                    // Update boundary state
                                    isFaceWithinBoundary = boundaryRect.contains(
                                        rect.left.toFloat(),
                                        rect.top.toFloat(),
                                        rect.right.toFloat(),
                                        rect.bottom.toFloat()
                                    )

                                    // perform recognition all time
                                    viewModel.recognizeFace(faceBitmap)
                                    insufficientLandmarks = false
                                    // If capture is requested, save the face
                                    if (capturingFace) {
                                        val scaledBitmap =
                                            Bitmap.createScaledBitmap(faceBitmap, 250, 250, false)
                                        capturedFace = scaledBitmap
                                        isCountingDown = false
                                    }
                                },
                                onInsufficientLandmarks = {
                                    // case when all facial landmarks are not detected
                                    insufficientLandmarks = true

                                    scope.launch {
                                        delay(3000)
                                        insufficientLandmarks = false
                                    }
                                }
                            )
                            cameraInitialized = true
                            Log.d("FaceCapturePage", "Camera initialized successfully")
                        } catch (e: Exception) {
                            Log.e("FaceCapturePage", "Error in camera coroutine: ${e.message}", e)
                        }
                    }
                } else {
                    Log.e("FaceCapturePage", "Camera permission not granted")
                }
            } catch (e: Exception) {
                Log.e("FaceCapturePage", "Error initializing camera: ${e.message}", e)

                // Try to recover after a delay
                delay(1000)
                isCameraActive = true
                cameraInitialized = false
            }
        }
    }

    // ป้องกันถูกเรียกซ้ำๆ
    DisposableEffect(key1 = Unit) {
        onDispose {
            Log.d("FaceCapturePage", "ensuring clean camera shutdown")
            isCameraActive = false

            // หน่วงเวลาให้กล้องไม่เกิดข้อผิดพลาด
            scope.launch {
                delay(300)
                cameraExecutor.shutdown()
            }
        }
    }

    // Design layout
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { navController.navigate("main_menu/$accountid") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF3F51B5)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(8.dp),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = "Face Recognition",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Black
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(16.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        Column {
                            // Camera preview card
                            Row(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f)
                                        .fillMaxHeight(0.75f),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(color = Color.DarkGray)
                                            .align(Alignment.CenterHorizontally),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // Camera preview code...
                                        if (isCameraActive) {
                                            AndroidView(
                                                factory = {
                                                    previewView.apply {
                                                        layoutParams = ViewGroup.LayoutParams(
                                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                                            ViewGroup.LayoutParams.MATCH_PARENT
                                                        )
                                                        implementationMode =
                                                            PreviewView.ImplementationMode.PERFORMANCE
                                                        scaleType =
                                                            PreviewView.ScaleType.FILL_CENTER
                                                    }
                                                    previewView
                                                },
                                                modifier = Modifier.fillMaxSize(),
                                                update = { view ->
                                                    if (isCameraActive && !isSurfaceReady) {
                                                        isSurfaceReady = true
                                                        Log.d(
                                                            "FaceCapturePage",
                                                            "PreviewView surface ready"
                                                        )
                                                    }
                                                }
                                            )
                                        }

                                        // apply face to boundary
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(400.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val overlayResId = when {
                                                recognizedName.isNotEmpty() && isFaceWithinBoundary -> R.drawable.rectangleerror
                                                insufficientLandmarks && isFaceWithinBoundary -> R.drawable.rectangleerror
                                                isFaceWithinBoundary -> R.drawable.rectangleok
                                                else -> R.drawable.rectangle
                                            }

                                            Image(
                                                painter = painterResource(id = overlayResId),
                                                contentDescription = "Face alignment boundary",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Fit
                                            )

                                            if (isCountingDown && recognizedName.isEmpty()) {
                                                Text(
                                                    text = countdownSeconds.toString(),
                                                    fontSize = 64.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    modifier = Modifier
                                                        .background(
                                                            Color.Black.copy(alpha = 0.6f),
                                                            RoundedCornerShape(16.dp)
                                                        )
                                                        .padding(
                                                            horizontal = 24.dp,
                                                            vertical = 16.dp
                                                        )
                                                )
                                            }

                                            capturedFace?.let { bitmap ->
                                                Image(
                                                    bitmap = bitmap.asImageBitmap(),
                                                    contentDescription = "Captured Face",
                                                    modifier = Modifier
                                                        .size(250.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .border(
                                                            4.dp,
                                                            Color.Green,
                                                            RoundedCornerShape(8.dp)
                                                        ),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Status messages row
                            Row {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (insufficientLandmarks || isCountingDown || !cameraInitialized ||
                                        (faceDetected && !isFaceWithinBoundary && recognizedName.isEmpty()) ||
                                        capturedFace != null) {
                                        Card(
                                            modifier = Modifier.padding(
                                                horizontal = 16.dp,
                                                vertical = 8.dp
                                            ),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(
                                                    horizontal = 16.dp,
                                                    vertical = 8.dp
                                                ),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                when {
                                                    // Prioritize captured face
                                                    capturedFace != null -> {
                                                        Text(
                                                            text = "จัดเก็บใบหน้าของ!",
                                                            fontSize = 22.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.Green
                                                        )
                                                        Text(
                                                            text = "กำลังบันทึกข้อมูลของ $participantName",
                                                            fontSize = 18.sp,
                                                            color = Color.Black,
                                                            textAlign = TextAlign.Center
                                                        )
                                                    }

                                                    // Then face not in boundary
                                                    (faceDetected && !isFaceWithinBoundary) -> {
                                                        Text(
                                                            text = "ใบหน้าไม่อยู่ในกรอบ",
                                                            fontSize = 22.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFFA15600)
                                                        )
                                                        Text(
                                                            text = "โปรดจัดตำแหน่งใบหน้าของท่านให้อยู่ในกรอบ",
                                                            fontSize = 18.sp,
                                                            color = Color.Black,
                                                            textAlign = TextAlign.Center
                                                        )
                                                    }

                                                    // Then check insufficient landmarks
                                                    (insufficientLandmarks && isFaceWithinBoundary) -> {
                                                        Text(
                                                            text = "มาร์คตำแหน่งใบหน้าไม่ครบถ้วน",
                                                            fontSize = 22.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.Red // Changed to red for warning
                                                        )
                                                        Text(
                                                            text = "กรุณาถอดสิ่งที่บดบังใบหน้า มองตรงที่กล้อง และห้ามยิ้ม",
                                                            fontSize = 18.sp,
                                                            color = Color.Black,
                                                            textAlign = TextAlign.Center
                                                        )
                                                    }

                                                    // Then countdown
                                                    isCountingDown -> {
                                                        Text(
                                                            text = "กำลังจะบันทึกข้อมูล",
                                                            fontSize = 22.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFFA15600)
                                                        )
                                                        Text(
                                                            text = "ท่านกำลังจะถูกเก็บใบหน้าในอีก $countdownSeconds วินาที...",
                                                            fontSize = 18.sp,
                                                            color = Color.Black,
                                                            textAlign = TextAlign.Center
                                                        )
                                                    }

                                                    // Then camera initializing
                                                    !cameraInitialized && isCameraActive -> {
                                                        Text(
                                                            text = "กำลังรอกล้องใช้งาน...",
                                                            fontSize = 22.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.Black
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (capturedFace == null) {
                                        if ((recognizedName.isNotEmpty() && isFaceWithinBoundary)) {
                                            Card(
                                                modifier = Modifier.padding(
                                                    horizontal = 16.dp,
                                                    vertical = 8.dp
                                                ),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(
                                                        horizontal = 16.dp,
                                                        vertical = 8.dp
                                                    ),
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    when {
                                                        faceDetected -> {
                                                            Text(
                                                                text = "ทำรายการไม่ได้",
                                                                fontSize = 22.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color.Red
                                                            )
                                                            Text(
                                                                text = "ชื่อผู้ใช้งานในระบบของท่านคือ : $recognizedName",
                                                                fontSize = 18.sp,
                                                                color = Color.Red,
                                                                textAlign = TextAlign.Center
                                                            )
                                                        }

                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Fixed bottom text Logo
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        text = "Face Authentication For Locker Borrowing",
                        fontSize = 36.sp,
                        color = Color.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}