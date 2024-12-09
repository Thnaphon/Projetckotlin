package com.example.LockerApp.view

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue

import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview

import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.LockerApp.viewmodel.QRCodeViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp


@Composable
fun QRCodeScreen(qrCodeViewModel: QRCodeViewModel = viewModel()) {
    var ssid by remember { mutableStateOf("MySSID") }
    var password by remember { mutableStateOf("MyPassword") }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        TextField(
            value = ssid,
            onValueChange = { ssid = it },
            label = { Text("SSID") }
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") }
        )
        Spacer(modifier = Modifier.height(16.dp))

        // ใช้ observeAsState() เพื่อรับข้อมูลจาก liveData
        val qrCodeBitmap by qrCodeViewModel.generateQRCode(ssid, password).observeAsState(initial = null)

        if (qrCodeBitmap != null) {
            Image(bitmap = qrCodeBitmap!!.asImageBitmap(), contentDescription = "QR Code", modifier = Modifier.wrapContentSize())
        } else {
            Text("Error generating QR code", modifier = Modifier.fillMaxSize())
        }
    }
}


@Preview(showBackground = true)
@Composable
fun QRCodeScreenPreview() {
    QRCodeScreen()
}