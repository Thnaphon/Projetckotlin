import GoogleDriveViewModel
import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import android.util.Log


@Composable
fun GoogleSignInButton(viewModel: GoogleDriveViewModel) {
    val context = LocalContext.current
    val googleSignInClient = remember { createGoogleSignInClient(context) }

    // ใช้ rememberLauncherForActivityResult ภายใน Composable ฟังก์ชัน
    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                if (task.isSuccessful) {
                    val account = task.result
                    Log.d("GoogleSignIn", "Sign-in successful for account: ${account?.email}")
                    viewModel.onSignIn(account)
                } else {
                    Log.e("GoogleSignIn", "Sign-in failed: ${task.exception?.message}")
                }
            } else {
                Log.e("GoogleSignIn", "Result code not OK: ${result.resultCode}")
            }
        }
    )

    Button(onClick = {
        Log.d("GoogleSignIn", "Button clicked")
        val signInIntent = googleSignInClient.signInIntent
        signInLauncher.launch(signInIntent) // ใช้ activity result launcher
    }) {
        Text(text = "Sign In with Google")
    }
}

private fun createGoogleSignInClient(context: Context): GoogleSignInClient {
    val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail() // ขอสิทธิ์ในการเข้าถึงอีเมล์ของผู้ใช้
        .requestScopes(com.google.android.gms.common.api.Scope(DriveScopes.DRIVE)) // ขอสิทธิ์ในการเข้าถึง Google Drive
        .build()
    return GoogleSignIn.getClient(context, signInOptions)
}
@Composable
fun GoogleDriveFilesUI(viewModel: GoogleDriveViewModel) {
    val driveFiles by viewModel.driveFiles.observeAsState(emptyList())

    Column {
        LazyColumn {
            items(driveFiles) { file ->
                Text(text = "File: ${file.name}")
                if (file.mimeType.startsWith("image/")) { // แสดงเฉพาะไฟล์รูปภาพ
                    DisplayImage() // ใช้ฟังก์ชันนี้ในการแสดงภาพ
                }
            }
        }
    }
}

@Composable
fun DisplayImage() {
    val imageUrl = "https://drive.google.com/uc?export=view&id=1RJDByDqPVZ-3x7lgBJQvH5nCMuV1rT6O"
    Image(
        painter = rememberImagePainter(imageUrl),
        contentDescription = "Compartment Image",
        modifier = Modifier.size(150.dp)
    )
}




@Composable
fun GoogleDriveSignInStatus(viewModel: GoogleDriveViewModel) {
    val isSignedIn by viewModel.isSignedIn.observeAsState(false)

    if (isSignedIn) {
        Text(text = "Sign-in successful")
        GoogleDriveFilesUI(viewModel = viewModel)  // แสดงไฟล์จาก Google Drive
    } else {
        Text(text = "Sign-in failed. Please try again.")
    }
}
