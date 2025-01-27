package com.example.LockerApp.viewmodel

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task

class GoogleSignInViewModel : ViewModel() {

    private val RC_SIGN_IN = 9001

    // ให้ Activity ไปใช้เพื่อทำการเข้าสู่ระบบ
    fun signIn(activity: Activity): Intent {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        val googleSignInClient = GoogleSignIn.getClient(activity, gso)
        return googleSignInClient.signInIntent
    }

    // ตรวจสอบผลการเข้าสู่ระบบ
    fun handleSignInResult(
        task: Task<GoogleSignInAccount>,
        onSuccess: (GoogleSignInAccount) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        try {
            val account = task.getResult(ApiException::class.java)
            onSuccess(account)
        } catch (e: ApiException) {
            onFailure(e)
            Log.w("GoogleSignIn", "signInResult:failed code=" + e.statusCode)
        }
    }
}