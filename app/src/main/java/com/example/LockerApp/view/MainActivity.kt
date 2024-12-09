package com.example.LockerApp.view

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent



class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // เรียกใช้ LockerApp ที่มีระบบนำทาง
        setContent {
            LockerApp()
            //QRCodeScreen()
        }
    }
}
