package com.Locker.LockerApp.utils

import java.text.SimpleDateFormat
import java.util.Locale

fun formatDate(dateString: String): String {
    val possibleFormats = listOf(
        "yyyy-MM-dd" // ถ้าไม่มีเวลา
    )

    val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH) // ใช้ Locale.ENGLISH

    for (format in possibleFormats) {
        try {
            val inputFormat = SimpleDateFormat(format, Locale.getDefault())
            val date = inputFormat.parse(dateString)
            if (date != null) return outputFormat.format(date)
        } catch (_: Exception) {
        }
    }

    return dateString // คืนค่าเดิมถ้าแปลงไม่ได้
}
