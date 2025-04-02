package com.Locker.LockerApp.view

import android.content.Context
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.Locker.LockerApp.utils.KeystoreManager

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

    // encryption
    LaunchedEffect(Unit) {
        KeystoreManager.generateKey()
        encryptedData.value = KeystoreManager.encryptData(masterPassword)
    }

        Card(
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp), // เพิ่มเงา
            modifier = Modifier.padding(16.dp).padding(vertical = 96.dp).padding(horizontal = 406.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .clip(MaterialTheme.shapes.medium), // ขอบมุมโค้ง /
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row() {
                        Text(
                            text = "Admin Verification",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF151227),
                            modifier = Modifier.padding(bottom = 24.dp)
                        )
                    }
                    Row() {
                        Text(
                            text = "Please enter the master password to verify your admin identity",
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 32.dp)
                        )
                    }
                    Row(
                        modifier = Modifier.height(1.5.dp).fillMaxWidth()
                            .background(Color(0xFFE8E8E8)),
                    ) {}

                    if (name != null && name.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().background(Color.White),
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp).background(Color.White),
                            ) {
                                Row() {
                                    Column(
                                        modifier = Modifier
                                            .padding(vertical = 8.dp)
                                            .width(150.dp)
                                    ) {
                                        Text(
                                            text = "Adding new user:",
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Column(
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = name,
                                            fontSize = 18.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Row() {
                                    if (role != null && role.isNotEmpty()) {
                                        Column(
                                            modifier = Modifier
                                                .padding(vertical = 8.dp)
                                                .width(150.dp)
                                        ) {
                                            Text(
                                                text = "Role:",
                                                fontSize = 16.sp
                                            )
                                        }
                                        Column(
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        ) {
                                            Text(
                                                text = role,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                Row() {
                                    if (phone != null && phone.isNotEmpty()) {
                                        Column(
                                            modifier = Modifier
                                                .padding(vertical = 8.dp)
                                                .width(150.dp)
                                        ) {
                                            Text(
                                                text = "Phone:",
                                                fontSize = 16.sp
                                            )
                                        }
                                        Column(
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        ) {
                                            Text(
                                                text = phone,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.height(1.5.dp).fillMaxWidth()
                                .background(Color(0xFFE8E8E8)),
                        ) {}
                        Row(
                            modifier = Modifier.height(24.dp)
                        ) {}
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
                                containerColor = Color.White, // กำหนดสีพื้นหลังปุ่ม
                                contentColor = Color.Black // กำหนดสีข้อความในปุ่ม
                            ),
                            border = BorderStroke(1.dp, Color.Black),
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
                                            if (name != null && role != null && phone != null &&
                                                name.isNotEmpty() && role.isNotEmpty() && phone.isNotEmpty()
                                            ) {
                                                navController.navigate(
                                                    "face_capture/$adminAccountId/$adminname/$adminrole?name=$name&role=$role&phone=$phone"
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
                                containerColor = Color(0xFF3961AA), // กำหนดสีพื้นหลังปุ่ม
                                contentColor = Color.White // กำหนดสีข้อความในปุ่ม
                            ),
                        ) {
                            Text("Verify")
                        }
                    }
                }
            }
        }

}