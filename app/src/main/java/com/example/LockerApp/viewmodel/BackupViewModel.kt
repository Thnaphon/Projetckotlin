package com.example.LockerApp.viewmodel


import android.content.Context
import android.os.Environment
import android.util.Log

import androidx.compose.runtime.mutableStateOf


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.example.LockerApp.model.BackupSettings

import com.example.LockerApp.model.LockerDatabase
import com.example.LockerApp.service.MqttService

import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream


import java.io.IOException

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.util.Base64
import kotlinx.coroutines.delay
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttMessage
import java.io.FileOutputStream

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

    fun performBackupToPi(mqttService: MqttService, context: Context) {
        viewModelScope.launch {
            val sourceDatabaseFile = context.getDatabasePath("locker_database")
            val sourceShmFile = context.getDatabasePath("locker_database-shm")
            val sourceWalFile = context.getDatabasePath("locker_database-wal")

            try {
                // อ่านไฟล์ database, SHM และ WAL
                val databaseBytes = sourceDatabaseFile.readBytes()
                val shmBytes = sourceShmFile.readBytes()
                val walBytes = sourceWalFile.readBytes()

                // แปลงไฟล์เป็น Base64 หรือส่งข้อมูลผ่าน MQTT โดยตรง
                val encodedDatabase = Base64.encodeToString(databaseBytes, Base64.DEFAULT)
                val encodedShm = Base64.encodeToString(shmBytes, Base64.DEFAULT)
                val encodedWal = Base64.encodeToString(walBytes, Base64.DEFAULT)

                // ส่งไฟล์ผ่าน MQTT โดยแบ่งเป็นชิ้นๆ
                sendBackupFileToPi(context, mqttService, encodedDatabase, "locker/backup/database")
                sendBackupFileToPi(context, mqttService, encodedShm, "locker/backup/shm")
                sendBackupFileToPi(context, mqttService, encodedWal, "locker/backup/wal")

                // บันทึกชื่อไฟล์และที่อยู่ของไฟล์สำรองที่ส่งไป
                Log.d("Backup", "Backup data sent successfully to Pi")

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
    fun sendBackupFileToPi(context: Context,mqttService: MqttService, encodedFile: String, topic: String) {
        // ตรวจสอบสถานะการเชื่อมต่อก่อนส่งข้อความ

        mqttService.connect(context) // พยายามเชื่อมต่อใหม่ (ตรวจสอบว่า connect method มีอยู่ในคลาสหรือไม่)


        val chunkSize = 4000 // ขนาดชิ้นส่วนที่ส่งได้
        val totalChunks = (encodedFile.length + chunkSize - 1) / chunkSize

        for (i in 0 until totalChunks) {
            val start = i * chunkSize
            val end = minOf(start + chunkSize, encodedFile.length)
            val chunk = encodedFile.substring(start, end)

            val message = MqttMessage(chunk.toByteArray())
            message.qos = 2

            try {
                mqttService.sendMessage(topic, chunk) // ส่งผ่าน MQTT
                Log.d("MQTT", "ส่งชิ้นที่ $i/$totalChunks ไปที่ topic: $topic")
            } catch (e: Exception) {
                Log.e("MQTT", "เกิดข้อผิดพลาดในการส่งชิ้นที่ $i: ${e.message}")
            }
        }
    }

    fun restoreBackupFromPi(mqttService: MqttService, context: Context) {
        viewModelScope.launch {
            try {
                val restoredDatabase = StringBuilder()
                val restoredShm = StringBuilder()
                val restoredWal = StringBuilder()
                mqttService.sendMessage("request/restore","")
                // รอรับข้อมูลจาก MQTT
                val databaseTopic = "locker/restore/database"
                val shmTopic = "locker/restore/shm"
                val walTopic = "locker/restore/wal"

                // ตั้งค่า listener
                mqttService.setOnMessageReceivedListener { topic, message ->
                    when (topic) {
                        databaseTopic -> restoredDatabase.append(String(message.payload))
                        shmTopic -> restoredShm.append(String(message.payload))
                        walTopic -> restoredWal.append(String(message.payload))
                    }
                }

                // สมัครรับข้อมูลจากแต่ละ topic

                mqttService.subscribeToTopic(databaseTopic)
                mqttService.subscribeToTopic(shmTopic)
                mqttService.subscribeToTopic(walTopic)

                // รอให้ข้อมูลครบ
                delay(5000)  // เพิ่มเวลาที่เหมาะสมในการรับข้อมูลทั้งหมด

                // รวมข้อมูลที่ได้รับ
                val databaseBytes = Base64.decode(restoredDatabase.toString(), Base64.DEFAULT)
                val shmBytes = Base64.decode(restoredShm.toString(), Base64.DEFAULT)
                val walBytes = Base64.decode(restoredWal.toString(), Base64.DEFAULT)

                // เขียนไฟล์ที่ได้รับลงในฐานข้อมูลหรือไฟล์ต่างๆ
                val databaseFile = context.getDatabasePath("locker_database")
                val shmFile = context.getDatabasePath("locker_database-shm")
                val walFile = context.getDatabasePath("locker_database-wal")

                Log.d("Database Path", "Database file path: ${databaseFile.absolutePath}")
                // เขียนข้อมูลลงในไฟล์
                if (databaseBytes.isNotEmpty() && shmBytes.isNotEmpty() && walBytes.isNotEmpty()) {
                    // เขียนข้อมูลลงในไฟล์
                    FileOutputStream(databaseFile).use { it.write(databaseBytes) }
                    FileOutputStream(shmFile).use { it.write(shmBytes) }
                    FileOutputStream(walFile).use { it.write(walBytes) }
                } else {
                    Log.e("Restore", "ข้อมูลที่ได้รับไม่ครบถ้วน")
                }

                // แจ้งผลการ restore
                Log.d("Restore", "ข้อมูลถูกรีสโตร์สำเร็จ")
            } catch (e: Exception) {
                Log.e("Restore", "การ restore ข้อมูลล้มเหลว", e)
            }
        }
    }
}