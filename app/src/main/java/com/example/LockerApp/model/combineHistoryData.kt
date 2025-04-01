package com.example.LockerApp.model

data class combine(
    val Username: String,      // รหัสบัญชีผู้ใช้
    val Lockername: String? = null,
    val Compartmentnumber : Int? = null,
    val UsageTime: String,   // เวลาใช้งาน (ในรูปแบบ String หรือ Timestamp)
    val Status: String, // สถานะ (อาจจะมีหรือไม่มีก็ได้)
    val Usage: String,
    val Equipment : String? = null,
    val User_Edited:String?= null,

    )