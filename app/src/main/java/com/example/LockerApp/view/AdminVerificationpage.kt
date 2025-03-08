package com.example.LockerApp.view

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.LockerApp.model.KeystoreManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun AdminVerificationPage(
    navController: NavController,
    adminAccountId: Int,
    adminname : String,
    adminrole : String,
    name: String?,
    role: String?,
    phone: String?
) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("LockerAppPrefs", Context.MODE_PRIVATE)
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

    // Initialize encryption
    LaunchedEffect(Unit) {
        KeystoreManager.generateKey()
        encryptedData.value = KeystoreManager.encryptData(masterPassword)
    }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 96.dp)
                .padding(horizontal = 406.dp)
                .border(1.dp, Color.Red, shape = MaterialTheme.shapes.medium) // ขอบมุมโค้ง
                .background(Color.White)
                .clip(MaterialTheme.shapes.medium), // ขอบมุมโค้ง /
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Admin Verification",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Text(
                text = "Please enter the master password to verify your admin identity",
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            if (name != null && name.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Adding new user:",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        if (role != null && role.isNotEmpty()) {
                            Text(
                                text = "Role: $role",
                                fontSize = 16.sp
                            )
                        }
                        if (phone != null && phone.isNotEmpty()) {
                            Text(
                                text = "Phone: $phone",
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = enteredPassword,
                onValueChange = { setEnteredPassword(it) },
                label = { Text("Master Password") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { navController.popBackStack() },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Cancel")
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = {
                        try {
                            val encrypted = encryptedData.value
                            if (encrypted != null) {
                                val decryptedPassword = KeystoreManager.decryptData(
                                    encrypted.first, encrypted.second
                                )

                                if (enteredPassword == decryptedPassword) {
                                    // Password verified, proceed to registration or face registration
                                    if (name != null && role != null && phone != null &&
                                        name.isNotEmpty() && role.isNotEmpty() && phone.isNotEmpty()
                                    ) {
                                        // If we have user data, go to face capture page
                                        navController.navigate(
                                            "face_capture/$adminAccountId/$adminname/$adminname?name=$name&role=$role&phone=$phone/"
                                        ) {
                                            popUpTo("admin_verification/$adminAccountId") {
                                                inclusive = true
                                            }
                                        }
                                    }
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
                                "Error during verification: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Verify")
                }
            }
        }

}