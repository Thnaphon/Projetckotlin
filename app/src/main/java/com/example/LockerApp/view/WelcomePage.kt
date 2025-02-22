package com.example.LockerApp.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.LockerApp.model.KeystoreManager
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.example.LockerApp.model.Account
import com.example.LockerApp.model.AccountDao
import com.example.LockerApp.model.LockerDatabase
import com.example.LockerApp.viewmodel.AccountViewModel


@Composable
fun WelcomePage(accountViewModel: AccountViewModel, navController: NavController) {

    val context = LocalContext.current
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var permissionDenialCount by remember { mutableStateOf(0) }

    val (isPasswordVisible, setIsPasswordVisible) = remember { mutableStateOf(false) }
    val (enteredPassword, setEnteredPassword) = remember { mutableStateOf("") }
    val masterPassword = "Micro_2567" // ตัวอย่างรหัสผ่าน
    val encryptedData = remember { mutableStateOf<Pair<ByteArray, ByteArray>?>(null) }

    // สร้างกุญแจและเข้ารหัสข้อมูลในตอนเริ่มต้น
    if (encryptedData.value == null) {
        LaunchedEffect(Unit) {
            KeystoreManager.generateKey() // สร้างกุญแจ
            encryptedData.value = KeystoreManager.encryptData(masterPassword)
        }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allPermissionsGranted = permissions.values.all { it }
        if (allPermissionsGranted) {
            navController.navigate("face_login")
        } else {
            permissionDenialCount++
            if (permissionDenialCount >= 2) {
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
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_MEDIA_IMAGES
        )

        if (arePermissionsGranted()) {
            navController.navigate("face_login")
        } else {
            permissionLauncher.launch(permissions)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome!",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { checkAndRequestPermissions() },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Start with FaceScan")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { setIsPasswordVisible(true) },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
            ) {
                Text(text = "Start with Master Pass")
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isPasswordVisible) {
                OutlinedTextField(
                    value = enteredPassword,
                    onValueChange = { setEnteredPassword(it) },
                    label = { Text("Enter Master Password") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = VisualTransformation.None,
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        try {
                            val encrypted = encryptedData.value
                            if (encrypted != null) {
                                val decryptedPassword = KeystoreManager.decryptData(
                                    encrypted.first, encrypted.second
                                )

                                if (enteredPassword == decryptedPassword) {
                                    navController.navigate("main_menu/1")
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Incorrect password",
                                        Toast.LENGTH_SHORT
                                    ).show()
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
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Green)
                ) {
                    Text(text = "Submit")
                }
            }
        }

        // Permission denied dialog
        if (showPermissionDialog) {
            AlertDialog(
                onDismissRequest = { showPermissionDialog = false },
                title = { Text("จำเป็นต้องอนุญาต การเข้าถึงกล้องและพื้นที่") },
                text = {
                    Text(
                        "แอพพลิเคชั่นของเราจำเป็นต้องเข้าถึงกล้อง และพื้นที่ในการจัดเก็บข้อมูล.",
                        textAlign = TextAlign.Center
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        showPermissionDialog = false
                        checkAndRequestPermissions()
                    }) {
                        Text("ลองอีกครั้ง")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPermissionDialog = false }) {
                        Text("ยกเลิก")
                    }
                }
            )
        }

        // Settings dialog (shown when permissions are permanently denied)
        if (showSettingsDialog) {
            AlertDialog(
                onDismissRequest = { showSettingsDialog = false },
                title = { Text("จำเป็นต้องอนุญาต การเข้าถึงกล้องและพื้นที่") },
                text = {
                    Text(
                        "แอพพลิเคชั่นของเราจำเป็นต้องเข้าถึงกล้อง และพื้นที่ในการจัดเก็บข้อมูล.",
                        textAlign = TextAlign.Center
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        val intent =
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                        context.startActivity(intent)
                        showSettingsDialog = false
                    }) {
                        Text("ไปที่การตั้งค่า")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSettingsDialog = false }) {
                        Text("ยกเลิก")
                    }
                }
            )
        }
    }
}