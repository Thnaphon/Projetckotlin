package com.example.LockerApp.service


import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

class GoogleSignInContract : ActivityResultContract<Void?, GoogleSignInAccount?>() {
    override fun createIntent(context: Context, input: Void?): Intent {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("1011972356569-5cibu5gs8ei0cb3mul4ckc67tn5i8n0g.apps.googleusercontent.com")  // ใส่ Client ID ของคุณที่นี่
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(context, gso)
        return client.signInIntent
    }

    override fun parseResult(resultCode: Int, intent: Intent?): GoogleSignInAccount? {
        if (resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(intent)
            try {
                return task.getResult(ApiException::class.java)
            } catch (e: ApiException) {
                // Handle error
            }
        }
        return null
    }
}