package com.example.LockerApp.viewmodel

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.google.api.client.extensions.android.http.AndroidHttp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class GoogleAuthViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext  // เก็บ Context ไว้ใช้กับ Toast
    private val _signInResult = MutableLiveData<GoogleSignInAccount?>()
    val signInResult: LiveData<GoogleSignInAccount?> = _signInResult

    private val googleSignInClient: GoogleSignInClient by lazy {
        GoogleSignIn.getClient(
            application.applicationContext,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(Scope(DriveScopes.DRIVE_FILE))
                .requestIdToken("1011972356569-ls2e65034naeppaphkrc3cl5668s2cu0.apps.googleusercontent.com")
                .build()
        )
    }

    private fun getDriveService(account: GoogleSignInAccount): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(
            getApplication<Application>(), listOf(DriveScopes.DRIVE_FILE)
        )
        credential.selectedAccount = account.account
        return Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            JacksonFactory.getDefaultInstance(), credential
        ).build()
    }

    fun getSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    fun handleSignInResult(data: Intent?) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(ApiException::class.java)
            _signInResult.value = account
            Log.d("GoogleAuthViewModel", "✅ Login Successful: ${account.email}")
            Toast.makeText(context, "Login Successful: ${account.email}", Toast.LENGTH_SHORT).show()
        } catch (e: ApiException) {
            _signInResult.value = null
            Log.e("GoogleAuthViewModel", "❌ Login Failed: ${e.statusCode}", e)
            Toast.makeText(context, "Login Failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun signOut() {
        googleSignInClient.signOut().addOnCompleteListener {
            _signInResult.value = null
            Log.d("GoogleAuthViewModel", "🔄 User Signed Out")
            Toast.makeText(context, "User Signed Out", Toast.LENGTH_SHORT).show()
        }
    }

    fun createQrCodeFromDriveFolder(folderId: String): Bitmap? {
        try {
            val account = _signInResult.value
            if (account != null) {
                val driveService = getDriveService(account)  // รับบัญชีที่ล็อกอินอยู่
                val folder = driveService.files().get(folderId).execute()  // ดึงข้อมูลโฟลเดอร์
                val folderUrl = "https://drive.google.com/drive/folders/${folder.id}"

                val qrCodeWriter = QRCodeWriter()
                val bitMatrix = qrCodeWriter.encode(folderUrl, BarcodeFormat.QR_CODE, 512, 512)

                // แปลง bitMatrix เป็น Bitmap
                val width = bitMatrix.width
                val height = bitMatrix.height
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

                for (x in 0 until width) {
                    for (y in 0 until height) {
                        bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                    }
                }
                return bitmap
            } else {
                Log.e("GoogleAuthViewModel", "❌ User not signed in")
            }
        } catch (e: Exception) {
            Log.e("GoogleAuthViewModel", "Error creating QR Code", e)
        }
        return null
    }

    // ใน GoogleAuthViewModel เพิ่มฟังก์ชันนี้
    fun chooseFolderFromDrive() {
        // ตรวจสอบว่าผู้ใช้ล็อกอินแล้วหรือไม่
        val account = _signInResult.value
        if (account == null) {
            Log.e("GoogleAuthViewModel", "❌ User not signed in")
            Toast.makeText(context, "Please sign in first", Toast.LENGTH_SHORT).show()
            return
        }

        // ใช้ Coroutine เพื่อทำงานใน background thread
        viewModelScope.launch {
            try {
                val driveService = getDriveService(account)

                // เรียก Google Drive API ใน background thread
                val request = withContext(Dispatchers.IO) {
                    driveService.files().list().apply {
                        q = "mimeType='application/vnd.google-apps.folder'" // ค้นหาเฉพาะโฟลเดอร์
                        fields = "nextPageToken, files(id, name)"
                    }.execute()
                }

                val folders = request.files
                val folderId = folders.firstOrNull()?.id

                folderId?.let {
                    // สร้าง QR Code จากโฟลเดอร์ที่เลือก
                    createQrCodeFromDriveFolder(it)
                } ?: run {
                    Log.e("GoogleAuthViewModel", "❌ No folders found")
                }
            } catch (e: Exception) {
                Log.e("GoogleAuthViewModel", "Error choosing folder", e)
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }




}
