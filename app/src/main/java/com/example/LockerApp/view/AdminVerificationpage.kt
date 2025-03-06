package com.example.LockerApp.view

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
import androidx.compose.foundation.text.KeyboardOptions
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
    var enteredPassword by remember { mutableStateOf("") }
    val masterPassword = "Micro_2567" // Same as in WelcomePage
    val encryptedData = remember { mutableStateOf<Pair<ByteArray, ByteArray>?>(null) }

    // Initialize encryption
    LaunchedEffect(Unit) {
        KeystoreManager.generateKey()
        encryptedData.value = KeystoreManager.encryptData(masterPassword)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
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
            onValueChange = { enteredPassword = it },
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
                                    name.isNotEmpty() && role.isNotEmpty() && phone.isNotEmpty()) {
                                    // If we have user data, go to face capture page
                                    navController.navigate(
                                        "face_capture/$adminAccountId/$adminname/$adminname?name=$name&role=$role&phone=$phone/"
                                    ) {
                                        popUpTo("admin_verification/$adminAccountId") { inclusive = true }
                                    }
                                } else {
                                    // Otherwise go to registration form
                                    navController.navigate("registration_form/$adminAccountId") {
                                        popUpTo("admin_verification/$adminAccountId") { inclusive = true }
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