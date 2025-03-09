package com.example.LockerApp.view

import android.content.Context
import android.util.Base64
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

@Composable
fun EditPasswordScreen(navController: NavController) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("LockerAppPrefs", Context.MODE_PRIVATE)

    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    Column(modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth()
            .padding(horizontal = 238.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.Center
    ) {
        Row {
            Card(
                elevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                ) {
                    Row (
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ){
                        Text(text = "Change Master Password",Modifier.fillMaxWidth(), fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }

                    Row {
                        // ฟิลด์กรอกรหัสผ่านเก่า
                        OutlinedTextField(
                            value = oldPassword,
                            onValueChange = { oldPassword = it },
                            label = { Text("Current Password") },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = PasswordVisualTransformation()
                        )
                    }

                    Row {
                        // ฟิลด์กรอกรหัสผ่านใหม่
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = { Text("New Password") },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = PasswordVisualTransformation()
                        )
                    }

                    Row (
                        Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ){
                        // ฟิลด์ยืนยันรหัสผ่านใหม่
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = { Text("Confirm New Password") },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = PasswordVisualTransformation()
                        )
                    }

                    Row {
                        // ปุ่มบันทึกรหัสผ่านใหม่
                        Button(
                            onClick = {
                                // ดึงค่าจาก SharedPreferences
                                val encryptedStoredPassword =
                                    sharedPreferences.getString("encrypted_master_password", null)
                                val storedIV = sharedPreferences.getString("encryption_iv", null)

                                // ถ้าไม่มีรหัสผ่านและ IV ใน SharedPreferences
                                if (encryptedStoredPassword == null || storedIV == null) {
                                    // ใช้รหัสผ่านเริ่มต้นและบันทึก
                                    val defaultPassword = "Micro_2567"
                                    val (encryptedPassword, iv) = KeystoreManager.encryptData(
                                        defaultPassword
                                    )

                                    // บันทึกใน SharedPreferences
                                    sharedPreferences.edit()
                                        .putString(
                                            "encrypted_master_password",
                                            Base64.encodeToString(encryptedPassword, Base64.DEFAULT)
                                        )
                                        .putString(
                                            "encryption_iv",
                                            Base64.encodeToString(iv, Base64.DEFAULT)
                                        )
                                        .apply()

                                    Toast.makeText(
                                        context,
                                        "Stored default password",
                                        Toast.LENGTH_SHORT
                                    )
                                        .show()
                                    return@Button
                                }

                                // ถอดรหัสรหัสผ่านเก่าที่เก็บไว้
                                val decryptedPassword = KeystoreManager.decryptData(
                                    Base64.decode(encryptedStoredPassword, Base64.DEFAULT),
                                    Base64.decode(storedIV, Base64.DEFAULT)
                                )

                                // ตรวจสอบว่ารหัสผ่านเก่าตรงกันไหม
                                if (oldPassword == decryptedPassword) {
                                    // ตรวจสอบว่า newPassword กับ confirmPassword ตรงกันไหม
                                    if (newPassword == confirmPassword) {
                                        // เข้ารหัสรหัสผ่านใหม่
                                        val (encryptedNewPassword, newIv) = KeystoreManager.encryptData(
                                            newPassword
                                        )

                                        // บันทึกรหัสผ่านใหม่ที่เข้ารหัสลงใน SharedPreferences
                                        sharedPreferences.edit()
                                            .putString(
                                                "encrypted_master_password",
                                                Base64.encodeToString(
                                                    encryptedNewPassword,
                                                    Base64.DEFAULT
                                                )
                                            )
                                            .putString(
                                                "encryption_iv",
                                                Base64.encodeToString(newIv, Base64.DEFAULT)
                                            )
                                            .apply()

                                        // แสดงข้อความยืนยัน
                                        Toast.makeText(
                                            context,
                                            "Password updated successfully",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        navController.popBackStack() // กลับไปยังหน้าก่อนหน้า
                                    } else {
                                        // ถ้ารหัสผ่านใหม่กับยืนยันไม่ตรงกัน
                                        Toast.makeText(
                                            context,
                                            "New passwords do not match",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } else {
                                    // ถ้ารหัสผ่านเก่าไม่ถูกต้อง
                                    Toast.makeText(
                                        context,
                                        "Incorrect current password",
                                        Toast.LENGTH_SHORT
                                    )
                                        .show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF3961AA)),
                        ) {
                            Text("Save New Password", color = Color(0xFFFFFFFF),)
                        }
                    }
                }
            }
        }
    }
}
