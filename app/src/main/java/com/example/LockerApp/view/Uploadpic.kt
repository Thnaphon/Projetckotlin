package com.example.LockerApp.view

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter


@Composable
fun uploadPicScreen() {
    val qrCodeBitmap = generateQRCode("https://drive.google.com/drive/folders/1D9ako6sSs4peHLsoEzXRsPKa24B22_6F?usp=drive_link")
    var showQRCode by remember { mutableStateOf(false) }

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Button(onClick = {
            showQRCode = !showQRCode  // เมื่อกดปุ่มจะแสดง/ซ่อน QR Code
        }) {
            Text("Scan QR Code to Select Image")
        }

        // แสดง QR Code เมื่อ showQRCode เป็น true
        if (showQRCode) {
            Image(bitmap = qrCodeBitmap.asImageBitmap(), contentDescription = "QR Code", modifier = Modifier.size(150.dp))
        }
    }
}

fun generateQRCode(content: String): Bitmap {
    val qrCodeWriter = QRCodeWriter()
    val bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, 200, 200)

    val width = bitMatrix.width
    val height = bitMatrix.height
    val pixels = IntArray(width * height)

    for (y in 0 until height) {
        for (x in 0 until width) {
            pixels[y * width + x] = if (bitMatrix.get(x, y)) {
                Color.BLACK
            } else {
                Color.WHITE
            }
        }
    }

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
    bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    return bitmap
}