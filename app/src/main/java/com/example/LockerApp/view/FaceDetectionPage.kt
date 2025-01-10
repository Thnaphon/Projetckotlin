package com.example.LockerApp.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import android.content.ContentValues
import android.content.Context
import android.media.Image
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextField
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.BlendMode.Companion.Screen
import androidx.compose.ui.graphics.Color
import android.graphics.ImageFormat
import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.internal.ImageUtils


@SuppressLint("UnsafeOptInUsageError")
@Composable
fun FaceDetectionPage(navController: NavHostController,
                      modifier: Modifier = Modifier,
                      participantName: String,
                      participantPhone: String,
                      participantRole: String) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember { androidx.camera.view.PreviewView(context) }
    var showDialog by remember { mutableStateOf(false) }
    var pendingRecognition by remember { mutableStateOf<FaceClassifier.Recognition?>(null) }

    var recognizedName by remember { mutableStateOf("Unknown") }

    //model init
    val faceClassifier = remember {
        TFLiteFaceRecognition.create(
            context.assets,
            "facenet.tflite",
            160, // Input size of the model
            false, // isModelQuantized
            context
        )
    }


    // State variables
    var capturedFace by remember { mutableStateOf<Bitmap?>(null) }
    var latestFrameBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var latestCropRect by remember { mutableStateOf<Rect?>(null) }

    // Permission launchers
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (!granted) {
                Toast.makeText(context, "Camera permission is required", Toast.LENGTH_LONG).show()
            }
        }
    )
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (!granted) {
                Toast.makeText(context, "Storage permission is required to save images", Toast.LENGTH_LONG).show()
            }
        }
    )

    // Request camera permission
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Face Detection Page",
            fontSize = 24.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

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
                .fillMaxWidth()
                .height(500.dp)
                .padding(16.dp)
        )
        // manually setting up liveness detection Preparing process

        // Camera and face detection setup
        LaunchedEffect(cameraProviderFuture) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                val faceDetectorOptions = FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                    .build()
                val faceDetector = FaceDetection.getClient(faceDetectorOptions)

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build().also { analysis ->
                        analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                                val image = InputImage.fromMediaImage(mediaImage, rotationDegrees)
                                faceDetector.process(image)
                                    // Face found and processing Get more information about face to make liveness detection here
                                    .addOnSuccessListener { faces ->
                                        if (faces.isNotEmpty()) {
                                            val face = faces.first()
                                            // Convert mediaImage to Bitmap
                                            val bitmap = mediaImageToBitmap(mediaImage, rotationDegrees)
                                            latestFrameBitmap = bitmap
                                            latestCropRect = face.boundingBox

                                            // Perform face recognition
                                            val faceBitmap = Bitmap.createBitmap(
                                                bitmap,
                                                latestCropRect!!.left.coerceAtLeast(0),
                                                latestCropRect!!.top.coerceAtLeast(0),
                                                latestCropRect!!.width().coerceAtMost(bitmap.width - latestCropRect!!.left),
                                                latestCropRect!!.height().coerceAtMost(bitmap.height - latestCropRect!!.top)
                                            )

                                            // Here's is how we send our face to model
                                            val resizedFaceBitmap = Bitmap.createScaledBitmap(faceBitmap, 160, 160, false)
                                            val recognition = faceClassifier.recognizeImage(resizedFaceBitmap, false)

                                            // Handle recognition result we could process more which who is them in our system
                                            recognition?.let {
                                                recognizedName = it.title ?: "Unknown"
                                            } ?: run {
                                                recognizedName = "Unknown"
                                            }
                                        }
                                        imageProxy.close()
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("FaceDetection", "Face detection failed", e)
                                        imageProxy.close()
                                    }
                            } else {
                                imageProxy.close()
                            }
                        }
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
                    Log.e("FaceDetection", "Camera binding failed", e)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Display captured face
        capturedFace?.let { face ->
            Image(
                bitmap = face.asImageBitmap(),
                contentDescription = "Captured Face",
                modifier = Modifier
                    .size(250.dp)
                    .padding(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {


            Button(onClick = {
                latestFrameBitmap?.let { bitmap ->
                    latestCropRect?.let { cropRect ->
                        try {
                            // Crop the face region
                            val faceBitmap = Bitmap.createBitmap(
                                bitmap,
                                cropRect.left.coerceAtLeast(0),
                                cropRect.top.coerceAtLeast(0),
                                cropRect.width().coerceAtMost(bitmap.width - cropRect.left),
                                cropRect.height().coerceAtMost(bitmap.height - cropRect.top)
                            )
                            val resizedFaceBitmap = Bitmap.createScaledBitmap(faceBitmap, 160, 160, false)

                            // Get embedding
                            val recognition = faceClassifier.recognizeImage(resizedFaceBitmap, true)

                            // Register face using the participant's info from ParticipantScreen
                            recognition?.let {
                                faceClassifier.register(participantName, participantRole, participantPhone,  it)
                                Toast.makeText(context, "Face registered for $participantName", Toast.LENGTH_SHORT).show()

                            } ?: run {
                                Toast.makeText(context, "Failed to register face", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Log.e("FaceDetection", "Failed to register face", e)
                            Toast.makeText(context, "Failed to register face", Toast.LENGTH_SHORT).show()
                        }
                    } ?: Toast.makeText(context, "No face detected", Toast.LENGTH_SHORT).show()
                } ?: Toast.makeText(context, "No image available", Toast.LENGTH_SHORT).show()
            }) {
                Text("Register Face")
            }
            if (showDialog && pendingRecognition != null) {
                showRegisterDialog(
                    context = context,
                    recognition = pendingRecognition!!,
                    participantName = participantName,
                    participantRole = participantRole,
                    participantPhone = participantPhone,
                    faceClassifier = faceClassifier,
                    onDismiss = {
                        showDialog = false
                        pendingRecognition = null
                    }
                )
            }

            Button(onClick = {
                latestFrameBitmap?.let { bitmap ->
                    latestCropRect?.let { cropRect ->
                        try {
                            // Crop the face region
                            val faceBitmap = Bitmap.createBitmap(
                                bitmap,
                                cropRect.left.coerceAtLeast(0),
                                cropRect.top.coerceAtLeast(0),
                                cropRect.width().coerceAtMost(bitmap.width - cropRect.left),
                                cropRect.height().coerceAtMost(bitmap.height - cropRect.top)
                            )
                            capturedFace = Bitmap.createScaledBitmap(faceBitmap, 250, 250, false)

                            // Check and request storage permission if necessary
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                != PackageManager.PERMISSION_GRANTED
                            ) {
                                storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            } else {
                                // Save the image
                                saveImageToGallery(context, capturedFace!!, "CapturedFace")
                                Toast.makeText(context, "Image saved to gallery", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Log.e("FaceDetection", "Failed to process face capture", e)
                            Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
                        }
                    } ?: Toast.makeText(context, "No face detected", Toast.LENGTH_SHORT).show()
                } ?: Toast.makeText(context, "No image available", Toast.LENGTH_SHORT).show()
            }) {
                Text("Capture Face")
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ){
            Text(
                text = "Recognized: $recognizedName",
                color = Color.White,
                fontSize = 34.sp
            )
        }
    }
}

// Helper function to convert mediaImage to Bitmap
private fun mediaImageToBitmap(mediaImage: Image, rotationDegrees: Int): Bitmap {
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

// Convert YUV_420_888 to NV21
fun yuv420888ToNv21(image: Image): ByteArray {
    val width = image.width
    val height = image.height
    val ySize = width * height
    val uvSize = width * height / 2

    val nv21 = ByteArray(ySize + uvSize)

    val yPlane = image.planes[0].buffer
    val uPlane = image.planes[1].buffer
    val vPlane = image.planes[2].buffer

    yPlane.get(nv21, 0, ySize)

    val uBuffer = ByteArray(uPlane.remaining())
    val vBuffer = ByteArray(vPlane.remaining())

    uPlane.get(uBuffer)
    vPlane.get(vBuffer)

    for (i in 0 until uvSize step 2) {
        nv21[ySize + i] = vBuffer[i / 2]
        nv21[ySize + i + 1] = uBuffer[i / 2]
    }

    return nv21
}

private fun saveImageToGallery(context: Context, bitmap: Bitmap, title: String) {
    val filename = "$title-${System.currentTimeMillis()}.jpg"
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            put(MediaStore.MediaColumns.IS_PENDING, false)
        }
    }

    val resolver = context.contentResolver
    val imageUri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

    imageUri?.let { uri ->
        resolver.openOutputStream(uri).use { outputStream ->
            if (outputStream != null) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            }
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // Notify the gallery about the new image
            val scanIntent = android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            scanIntent.data = uri
            context.sendBroadcast(scanIntent)
        }
    }
}

@Composable
fun showRegisterDialog(
    context: Context,
    recognition: FaceClassifier.Recognition,
    participantName: String,  // <- pass the name from participant
    participantRole: String,
    participantPhone: String,
    faceClassifier: FaceClassifier,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Register Face") },
        text = {
            // Just show read-only text, no text field for name
            Text(text = "Register face for: $participantName")
        },
        confirmButton = {
            TextButton(onClick = {
                // Use the participantName from caller
                faceClassifier.register(participantName, participantRole, participantPhone, recognition)
                Toast.makeText(context, "Face registered successfully", Toast.LENGTH_SHORT).show()
                onDismiss()
            }) {
                Text("Register")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

