package com.example.LockerApp.view

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.LockerApp.viewmodel.GoogleAuthViewModel
import com.example.LockerApp.viewmodel.SignInResult
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.LockerApp.viewmodel.GoogleAuthViewModelFactory
import com.google.android.gms.auth.api.signin.GoogleSignIn

@Composable
fun GoogleSignInScreen() {
    val context = LocalContext.current
    val viewModel: GoogleAuthViewModel = viewModel(factory = GoogleAuthViewModelFactory(context))
    val activity = context as? Activity ?: return  // ดึง Activity ออกจาก Context

    val signInState by viewModel.signInState.collectAsState()

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data).result
            Log.d("GoogleAuth", "Google Sign-In succeeded")
            viewModel.handleSignInResult(account)
        } else {
            // ใช้ getStringExtra เพื่อดึงข้อมูล error ที่อาจจะมาจาก result.data
            val errorMessage = result.data?.getStringExtra("error") ?: "Unknown error"
            Log.d("GoogleAuth", "Google Sign-In failed, Error: $errorMessage")
            viewModel.handleSignInResult(null)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        when (signInState) {
            is SignInResult.Success -> {
                Log.d("GoogleAuth", "Sign-in result: Success - Token: ${(signInState as SignInResult.Success).token}")
                Text("Sign-in Success! Token: ${(signInState as SignInResult.Success).token}")
            }
            is SignInResult.Failure -> {
                Log.d("GoogleAuth", "Sign-in result: Failure - Error: ${(signInState as SignInResult.Failure).error}")
                Text("Sign-in Failed: ${(signInState as SignInResult.Failure).error}")
            }
            null -> {
                Log.d("GoogleAuth", "No sign-in state")
                Button(onClick = {
                    val intent = viewModel.signIn()  // รับค่า Intent
                    signInLauncher.launch(intent)  // ใช้ Intent กับ launcher
                }) {
                    Text("Sign in with Google")
                }
            }
        }
    }
}