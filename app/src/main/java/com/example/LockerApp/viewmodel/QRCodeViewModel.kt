package com.example.LockerApp.viewmodel

import android.graphics.Bitmap
import android.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData

import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers


class QRCodeViewModel : ViewModel() {

    fun generateQRCode(ssid: String, password: String) = liveData(Dispatchers.IO) {
        val qrCodeContent = "WIFI:S:$ssid;T:WPA;P:$password;;" // ข้อมูลการเชื่อมต่อ Wi-Fi Direct
        try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(qrCodeContent, BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            emit(bitmap) // ส่งผลลัพธ์กลับไปยัง UI
        } catch (e: Exception) {
            emit(null) // ส่งค่า null ถ้าเกิดข้อผิดพลาด
        }
    }
}
