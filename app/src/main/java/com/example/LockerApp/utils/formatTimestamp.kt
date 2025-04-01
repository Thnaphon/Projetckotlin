package com.example.LockerApp.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ฟังก์ชันแปลง timestamp เป็นรูปแบบ hh:mm dd/MM/yyyy
fun formatTimestamp(timestamp: String): String {
    return try {
        val date = Date(timestamp.toLong())
        val sdf = SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault())
        sdf.format(date)
    } catch (e: Exception) {
        "Invalid date"
    }
}