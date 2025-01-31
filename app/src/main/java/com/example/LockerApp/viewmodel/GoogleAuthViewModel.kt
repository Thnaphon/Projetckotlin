package com.example.LockerApp.viewmodel
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

sealed class SignInResult {
    data class Success(val token: String) : SignInResult()
    data class Failure(val error: String) : SignInResult()
}

class GoogleAuthViewModel(context: Context) : ViewModel() {

    private val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken("1011972356569-5cibu5gs8ei0cb3mul4ckc67tn5i8n0g.apps.googleusercontent.com") // ใส่ Client ID ที่ได้รับจาก Google Console
        .requestEmail()
        .build()

    private val googleSignInClient: GoogleSignInClient = GoogleSignIn.getClient(context, gso)

    private val _signInState = MutableStateFlow<SignInResult?>(null)
    val signInState: StateFlow<SignInResult?> = _signInState

    fun signIn(): Intent {
        return googleSignInClient.signInIntent
    }

    fun handleSignInResult(account: GoogleSignInAccount?) {
        if (account != null) {
            Log.d("GoogleAuth", "Sign-in successful with account: ${account.email}")
            _signInState.value = SignInResult.Success(account.idToken ?: "")
        } else {
            Log.d("GoogleAuth", "Sign-in failed: No account returned")
            _signInState.value = SignInResult.Failure("Sign-in failed")
        }
    }
}