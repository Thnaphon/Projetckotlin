package com.example.LockerApp.view

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.VisualTransformation
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.LockerApp.model.KeystoreManager


@Composable
fun WelcomePage(navController: NavController) {

    val context = LocalContext.current
    val (isPasswordVisible, setIsPasswordVisible) = remember { mutableStateOf(false) }
    val (enteredPassword, setEnteredPassword) = remember { mutableStateOf("") }
    val masterPassword = "Micro_2567" // ตัวอย่างรหัสผ่าน
    val encryptedData = remember { mutableStateOf<Pair<ByteArray, ByteArray>?>(null) }

    // เข้ารหัสรหัสผ่านเมื่อโหลดหน้า
    LaunchedEffect(Unit) {
        KeystoreManager.generateKey() // สร้างกุญแจ
        encryptedData.value = KeystoreManager.encryptData(masterPassword)
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
                onClick = { navController.navigate("face_login") },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Start with FaceScan")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    setIsPasswordVisible(true)
                },
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
                                Log.d("WelcomePage", "Stored IV: ${encrypted.second.joinToString()}")
                                Log.d("WelcomePage", "Stored Encrypted Data: ${encrypted.first.joinToString()}")

                                val decryptedPassword = KeystoreManager.decryptData(
                                    encrypted.first, // **แก้ให้ใช้ encryptedData ก่อน iv**
                                    encrypted.second
                                )

                                Log.d("WelcomePage", "Decrypted Password: $decryptedPassword")

                                if (enteredPassword == decryptedPassword) {
                                    navController.navigate("main_menu/{accountid}")
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Incorrect password",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("WelcomePage", "Decryption error", e)
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
    }
}