package com.example.LockerApp.view

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.Button
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.LockerApp.viewmodel.GoogleSignInViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount



@Composable
fun GoogleSignInScreen(
    onSignInSuccess: (GoogleSignInAccount) -> Unit,
    onSignInFailure: (Exception) -> Unit
) {
    val googleSignInViewModel: GoogleSignInViewModel = viewModel()

    // ใช้ ActivityResultLauncher ในฟังก์ชัน @Composable
    val signInLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            googleSignInViewModel.handleSignInResult(
                task,
                onSuccess = { account ->
                    onSignInSuccess(account)  // ส่งต่อไปยัง callback เมื่อเข้าสู่ระบบสำเร็จ
                },
                onFailure = { exception ->
                    onSignInFailure(exception)  // ส่งต่อไปยัง callback เมื่อเข้าสู่ระบบล้มเหลว
                }
            )
        } else {
            Log.e("GoogleSignIn", "Login canceled or failed with code: ${result.resultCode}")
            onSignInFailure(Exception("Login canceled or failed"))
        }
    }

    // ใช้ LocalContext ภายใน @Composable
    val context = LocalContext.current

    Column(modifier = Modifier.padding(16.dp)) {
        BasicText(text = "Welcome to LockerApp")

        Button(onClick = {
            // ตรวจสอบว่า context เป็น Activity
            if (context is Activity) {
                val signInIntent = googleSignInViewModel.signIn(context)
                if (signInIntent != null) {
                    signInLauncher.launch(signInIntent)  // ใช้ ActivityResultLauncher เพื่อเริ่มการเข้าสู่ระบบ
                } else {
                    Log.e("GoogleSignIn", "Failed to create sign-in intent.")
                    onSignInFailure(Exception("Failed to create sign-in intent"))
                }
            }
        }) {
            BasicText(text = "Login with Google")
        }
    }
}