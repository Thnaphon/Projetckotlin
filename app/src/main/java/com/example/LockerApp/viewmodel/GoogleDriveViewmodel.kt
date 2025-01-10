import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GoogleDriveViewModel(application: Application) : AndroidViewModel(application) {

    private val _driveFiles = MutableLiveData<List<File>>()
    val driveFiles: LiveData<List<File>> = _driveFiles

    // สถานะของการ sign in
    private val _isSignedIn = MutableLiveData<Boolean>()
    val isSignedIn: LiveData<Boolean> = _isSignedIn

    fun onSignIn(account: GoogleSignInAccount?) {
        account?.let {
            val context: Context = getApplication<Application>().applicationContext
            Log.d("GoogleDriveViewModel", "Sign-in successful for account: ${it.email}")

            viewModelScope.launch {
                val credential = GoogleAccountCredential.usingOAuth2(
                    context,
                    listOf(DriveScopes.DRIVE)
                ).apply {
                    selectedAccount = it.account
                }

                val driveService = Drive.Builder(
                    NetHttpTransport(),  // ใช้ NetHttpTransport แทน AndroidHttp
                    GsonFactory.getDefaultInstance(),
                    credential
                ).setApplicationName("Your App Name").build()

                withContext(Dispatchers.IO) {
                    try {
                        // ดึงไฟล์จาก Google Drive
                        val fileList = driveService.files().list().apply {
                            pageSize = 10
                            fields = "nextPageToken, files(id, name, mimeType)"
                        }.execute().files

                        _driveFiles.postValue(fileList)
                        _isSignedIn.postValue(true)  // ตั้งค่าสถานะว่า sign in สำเร็จ
                        Log.d("GoogleDriveViewModel", "Files fetched successfully")
                    } catch (e: Exception) {
                        _isSignedIn.postValue(false)  // ถ้ามีข้อผิดพลาดในการ sign in
                        Log.e("GoogleDriveViewModel", "Error fetching files: ${e.message}")
                    }
                }
            }
        } ?: run {
            _isSignedIn.postValue(false)  // ถ้า account เป็น null
            Log.e("GoogleDriveViewModel", "Account is null, sign-in failed")
        }
    }
}
