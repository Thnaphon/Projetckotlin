package com.example.LockerApp.view


import ParticipantScreen
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.example.LockerApp.viewmodel.AccountViewModel


class MainActivity : ComponentActivity() {
    private val accountViewModel: AccountViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // เรียกใช้ LockerApp ที่มีระบบนำทาง
        setContent {
            //DisplayImage()
            LockerApp()
            //QRCodeScreen()
            //ParticipantScreen(accountViewModel)
        }
    }
}
