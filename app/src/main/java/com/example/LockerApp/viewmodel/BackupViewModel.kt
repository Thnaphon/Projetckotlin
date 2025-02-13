package com.example.LockerApp.viewmodel


import android.content.Context
import android.os.Environment
import android.util.Log

import androidx.compose.runtime.mutableStateOf


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.example.LockerApp.model.BackupSettings

import com.example.LockerApp.model.LockerDatabase

import kotlinx.coroutines.launch
import java.io.File


import java.io.IOException

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class BackupViewModel : ViewModel() {
    // ใช้ mutableStateOf สำหรับเก็บชื่อไฟล์สำรองและที่อยู่ไฟล์
    var backupFileName = mutableStateOf("")
    var backupFilePath = mutableStateOf("")

    // ฟังก์ชันสำหรับการแบ็คอัพ
    fun performBackup(context: Context) {
        viewModelScope.launch {
            val sourceDatabaseFile = context.getDatabasePath("locker_database")
            val sourceShmFile = context.getDatabasePath("locker_database-shm")
            val sourceWalFile = context.getDatabasePath("locker_database-wal")

            val backupFolder = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)

            // ตรวจสอบว่า backupFolder ไม่เป็น null
            if (backupFolder != null) {
                // สร้างไฟล์สำรองสำหรับแต่ละไฟล์
                val backupDatabaseFile = File(backupFolder, "backup_locker_database")
                val backupShmFile = File(backupFolder, "backup_locker_database-shm")
                val backupWalFile = File(backupFolder, "backup_locker_database-wal")

                try {
                    // คัดลอกไฟล์ฐานข้อมูลหลัก
                    sourceDatabaseFile.inputStream().use { input ->
                        backupDatabaseFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    // คัดลอกไฟล์ SHM
                    sourceShmFile.inputStream().use { input ->
                        backupShmFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    // คัดลอกไฟล์ WAL
                    sourceWalFile.inputStream().use { input ->
                        backupWalFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    // บันทึกชื่อไฟล์สำรองและที่อยู่ไฟล์
                    backupFileName.value = backupDatabaseFile.name
                    backupFilePath.value = backupDatabaseFile.absolutePath

                    Log.d("Backup", "Backup completed successfully")

                    // อัพเดตวันที่สำรองล่าสุด
                    val currentDate =
                        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                    val backupDao = LockerDatabase.getDatabase(context).backupDao()
                    val backupSettings = backupDao.getBackupSettings()

                    if (backupSettings != null) {
                        backupDao.updateLastBackupDate(backupSettings.id, currentDate)
                    } else {
                        val newBackupSettings = BackupSettings(
                            frequency = "Daily",
                            backupTime = "02:00 AM",
                            lastBackupDate = currentDate
                        )
                        backupDao.insertOrUpdateBackupSettings(newBackupSettings)
                    }

                } catch (e: IOException) {
                    Log.e("Backup", "Backup failed", e)
                }
            }
        }
    }
    // ฟังก์ชันสำหรับการคืนค่าข้อมูล
    fun performRestore(context: Context) {
        viewModelScope.launch {
            val backupFolder = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)

            if (backupFolder != null) {
                // ตัวอย่างการคืนค่าไฟล์สำรองที่ชื่อว่า "backup_locker_database"
                val backupDatabaseFile = File(backupFolder, "backup_locker_database")
                val backupShmFile = File(backupFolder, "backup_locker_database-shm")
                val backupWalFile = File(backupFolder, "backup_locker_database-wal")

                val sourceDatabaseFile = context.getDatabasePath("locker_database")
                val sourceShmFile = context.getDatabasePath("locker_database-shm")
                val sourceWalFile = context.getDatabasePath("locker_database-wal")

                try {
                    // คัดลอกไฟล์สำรองคืนไปยังไฟล์ฐานข้อมูล
                    backupDatabaseFile.inputStream().use { input ->
                        sourceDatabaseFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    // คัดลอกไฟล์ SHM
                    backupShmFile.inputStream().use { input ->
                        sourceShmFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    // คัดลอกไฟล์ WAL
                    backupWalFile.inputStream().use { input ->
                        sourceWalFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    // อัปเดตสถานะของการคืนค่าข้อมูล
                    backupFileName.value = backupDatabaseFile.name
                    backupFilePath.value = backupDatabaseFile.absolutePath

                    Log.d("Restore", "Restore completed successfully")

                    // อัปเดตวันที่การคืนค่าล่าสุดในฐานข้อมูล
                    val currentDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                    val backupDao = LockerDatabase.getDatabase(context).backupDao()
                    val backupSettings = backupDao.getBackupSettings()

                    if (backupSettings != null) {
                        backupDao.updateLastBackupDate(backupSettings.id, currentDate)
                    } else {
                        val newBackupSettings = BackupSettings(
                            frequency = "Daily",
                            backupTime = "02:00 AM",
                            lastBackupDate = currentDate
                        )
                        backupDao.insertOrUpdateBackupSettings(newBackupSettings)
                    }
                } catch (e: IOException) {
                    Log.e("Restore", "Restore failed", e)
                }
            }
        }
    }
}