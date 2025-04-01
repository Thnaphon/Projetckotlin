package com.Locker.LockerApp.view

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.Locker.LockerApp.model.KeystoreManager
import com.Locker.LockerApp.viewmodel.AccountViewModel
import com.Locker.LockerApp.viewmodel.FaceLoginViewModel
import androidx.compose.ui.text.input.PasswordVisualTransformation
import coil.compose.rememberImagePainter
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import coil.compose.rememberAsyncImagePainter

@Composable
fun WelcomePage(
    accountViewModel: AccountViewModel,
    faceLoginViewModel: FaceLoginViewModel,
    navController: NavController
) {

    val coroutineScope = rememberCoroutineScope()

    val context = LocalContext.current
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var permissionDenialCount by remember { mutableStateOf(0) }
    var showFaceLoginOverlay by remember { mutableStateOf(false) }
    val (modechange, setModechange) = remember { mutableStateOf(false) }

    val sharedPreferences = context.getSharedPreferences("LockerAppPrefs", Context.MODE_PRIVATE)
    val (isPasswordVisible, setIsPasswordVisible) = remember { mutableStateOf(false) }
    val (enteredPassword, setEnteredPassword) = remember { mutableStateOf("") }
    var masterPassword by remember {
        mutableStateOf(
            try {
                sharedPreferences.getString("encrypted_master_password", null)?.let { encrypted ->
                    val encryptedData = Base64.decode(encrypted, Base64.DEFAULT)
                    val iv = sharedPreferences.getString("encryption_iv", null)?.let { ivBase64 ->
                        Base64.decode(ivBase64, Base64.DEFAULT)
                    }
                    if (iv != null) KeystoreManager.decryptData(encryptedData, iv) else null
                } ?: "Micro_2567" // ถ้าไม่มีค่า ให้ใช้ค่าเริ่มต้น
            } catch (e: Exception) {
                Log.e("LockerApp", "Error decrypting password: ${e.message}")
                "Micro_2567" // ใช้ค่าเริ่มต้นถ้ามีข้อผิดพลาด
            }
        )
    }
    val encryptedData = remember { mutableStateOf<Pair<ByteArray, ByteArray>?>(null) }
    val maskedChars = remember { mutableStateListOf<Boolean>() }

    val permissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.READ_MEDIA_IMAGES
    )

    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp


    LaunchedEffect(Unit) {
        // init face in database and reset scan state
        faceLoginViewModel.refreshFaceData()
        faceLoginViewModel.resetToScanning()
    }

    // คำนวณขนาดที่ต้องการตามเปอร์เซ็นต์

    // สร้างกุญแจและเข้ารหัสข้อมูลในตอนเริ่มต้น
    if (encryptedData.value == null) {
        LaunchedEffect(Unit) {
            KeystoreManager.generateKey() // สร้างกุญแจ
            encryptedData.value = KeystoreManager.encryptData(masterPassword)
        }
    }

    // asking for Permission
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allPermissionsGranted = permissions.values.all { it }
        if (allPermissionsGranted) {
            // Show face login overlay instead of navigating
//            showFaceLoginOverlay = true
        } else {
            // annoying user What is going on now you have to do it your own
            permissionDenialCount++
            if (permissionDenialCount >= 5) {
                showSettingsDialog = true
            } else {
                showPermissionDialog = true
            }
        }
    }

    // ฟังก์ชันเช็คสิทธิ์
    fun arePermissionsGranted(): Boolean {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_MEDIA_IMAGES
        )
        return permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    // เช็คสิทธิ์และขอสิทธิ์
    fun checkAndRequestPermissions() {
        if (arePermissionsGranted()) {
            showFaceLoginOverlay = true
        } else {
            permissionLauncher.launch(permissions)
        }
    }

    fun checkAndRequestFormaster() {
        if (arePermissionsGranted()) {
            //
        } else {
            permissionLauncher.launch(permissions)
        }
    }

    // Show face login overlay on call
    if (showFaceLoginOverlay) {
        FaceLoginOverlay(
            navController = navController,
            viewModel = faceLoginViewModel,
            onDismiss = {
                showFaceLoginOverlay = false
            },
            onLoginSuccess = { accountId, name, role, phone ->
                showFaceLoginOverlay = false
                // calling mainmenu if success login state after loginoverlay
                val route = when (role) {
                    "Owner" -> "main_menu/$accountId/$name/$role"  // if Owner then admin_dashboard
                    "service" -> "main_menu/$accountId/$name/$role"  // if service ให้ไปหน้า service_dashboard
                    else -> "main_menu_user/$accountId/$name/$role"  // if user ให้ไปหน้า user_dashboard
                }

                navController.navigate(route) {   // ใช้ route ที่กำหนด
                    popUpTo("face_login") { inclusive = true }
                    Log.d("FaceAcountid", "$accountId")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(70.dp)

    ) {
        Column {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)


            ) {
                val imagePainter = rememberImagePainter("file:///android_asset/Picture/Welcome.png")

                Image(
                    painter = imagePainter,
                    contentDescription = "Image from Assets",
                    modifier = Modifier
                        .height(170.dp)

                )
            }
            Row {
                //if condition
                Column(
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier
                        .weight(0.3f)
                        .height(370.dp)

                ) {
                    if (!modechange) {
                        Button(
                            onClick = { checkAndRequestPermissions() },
                            shape = RoundedCornerShape(0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF3961AA),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .width(250.dp)
                                .height(60.dp)
                                .shadow(8.dp, RoundedCornerShape(6.dp))
                        ) {
                            Text(text = "Start Now", fontSize = 26.sp)

                        }

                        ClickableText(
                            text = AnnotatedString.Builder().apply {
                                append("Use ")
                                withStyle(style = SpanStyle(color = Color(0xFF3961AA))) {
                                    append("Master Password")
                                }
                            }.toAnnotatedString(),
                            onClick = {
                                setModechange(true)
                            }, modifier = Modifier.padding(top = 16.dp)
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column() {
                                    OutlinedTextField(
                                        value = enteredPassword,
                                        onValueChange = { setEnteredPassword(it) },
                                        label = {
                                            Text(
                                                text = "Enter Master Password",
                                                style = TextStyle(fontSize = 12.sp) // ปรับขนาดข้อความที่แสดงใน label
                                            )
                                        },
                                        modifier = Modifier
                                            .width(250.dp)
                                            .height(70.dp),
                                        visualTransformation = PasswordVisualTransformation(),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                                    )
                                }
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center, // ระยะห่างระหว่างปุ่ม
                                    modifier = Modifier.padding(start = 5.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            try {
                                                if (!arePermissionsGranted()) {
                                                    permissionLauncher.launch(permissions)
                                                } else {
                                                    val encrypted = encryptedData.value
                                                    if (encrypted != null) {
                                                        val decryptedPassword =
                                                            KeystoreManager.decryptData(
                                                                encrypted.first, encrypted.second
                                                            )

                                                        if (enteredPassword == decryptedPassword) {
                                                            navController.navigate("main_menu/1/Service/Service")
                                                        } else {
                                                            Toast.makeText(
                                                                context,
                                                                "Incorrect password",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                Toast.makeText(
                                                    context,
                                                    "Error during decryption",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .padding(top = 7.dp)
                                            .width(61.dp) // ให้ปุ่มกว้างเท่ากับช่องกรอก Master Password
                                            .height(61.dp), // กำหนดความสูงของปุ่มให้เท่ากับช่องกรอก
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(
                                                0xFF3961AA
                                            )
                                        )
                                    ) {
                                        Icon(
                                            Icons.Filled.Check,
                                            contentDescription = "Add Locker",
                                            tint = Color(0xFFFFFFFF),
                                            modifier = Modifier.scale(2f)
                                        )
                                    }
                                }
                            }
                        }
                        ClickableText(
                            text = AnnotatedString.Builder().apply {
                                append("Use ")
                                withStyle(style = SpanStyle(color = Color(0xFF3961AA))) {
                                    append("Face recognition")
                                }
                            }.toAnnotatedString(),
                            onClick = {
                                setModechange(false)
                            }, modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier
                        .weight(0.7f)

                        .height(700.dp)


                ) {
                    val imagePainter2 =
                        rememberAsyncImagePainter("file:///android_asset/Picture/Locker.jpg")
                    Image(
                        painter = imagePainter2,
                        contentDescription = "Image from Assets",
                        modifier = Modifier
                    )
                }
            }
        }
    }

    // Permission denied dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Need permission") },
            text = {
                Text(
                    "Our application need to access Camera and Gallery to work properly.",
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(onClick = {
                    showPermissionDialog = false
                    checkAndRequestPermissions()
                }) {
                    Text("Try again")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Settings dialog (shown when permissions are permanently denied)
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("We need permission") },
            text = {
                Text(
                    "Our application need to access Camera and Gallery to work properly.",
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                // Force user do it their own setup camera perm
                Button(onClick = {
                    val intent =
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                    context.startActivity(intent)
                    showSettingsDialog = false
                }) {
                    Text("Go to setting")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
