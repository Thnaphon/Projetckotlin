package com.example.LockerApp.viewmodel


import android.app.Application
import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import com.example.LockerApp.model.BackupSettings
import com.example.LockerApp.model.LockerDatabase
import com.example.LockerApp.service.MqttService
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import com.example.LockerApp.model.BackupDao
import com.example.LockerApp.model.BackupLog
import com.example.LockerApp.model.BackupLogDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.eclipse.paho.client.mqttv3.MqttMessage
import java.io.FileOutputStream
import java.util.concurrent.CountDownLatch

class BackupViewModel(application: Application) : AndroidViewModel(application) {
    // ใช้ mutableStateOf สำหรับเก็บชื่อไฟล์สำรองและที่อยู่ไฟล์
    var backupFileName = mutableStateOf("")
    var backupFilePath = mutableStateOf("")
    private val BackupDao: BackupDao =
        LockerDatabase.getDatabase(application).backupDao()

    private val backupLogDao: BackupLogDao =
        LockerDatabase.getDatabase(application).BackupLogDao()

    private val _backupSettings = MutableStateFlow<BackupSettings?>(null)
    val backupSettings: StateFlow<BackupSettings?> = _backupSettings.asStateFlow()

    private val _allBackupLogs = MutableStateFlow<List<BackupLog>>(emptyList())
    val allBackupLogs: StateFlow<List<BackupLog>> get() = _allBackupLogs

    init {
        viewModelScope.launch {
            _backupSettings.value = BackupDao.getBackupSettings()
            backupLogDao.getAllBackupLogs()
                .collect { backupLogs ->
                    // อัพเดทข้อมูลใน StateFlow เมื่อได้ผลลัพธ์ใหม่
                    _allBackupLogs.value = backupLogs
                }
        }
    }


    // ฟังก์ชันสำหรับการแบ็คอัพ
    fun insertBackupLog(backupLog: BackupLog) {
        viewModelScope.launch {
            try {
                backupLogDao.insertBackupLog(backupLog) // สมมติว่า insertBackupLog เป็นฟังก์ชันที่บันทึกลงในฐานข้อมูล
                Log.d("Backup", "Backup log inserted successfully")
            } catch (e: Exception) {
                Log.e("Backup", "Error inserting backup log", e)
            }
        }
    }

    // ฟังก์ชันสำหรับการแบ็คอัพ
    fun performBackup(context: Context, accountname: String,description: String,) {
        viewModelScope.launch {

            // หลังจากสำรองข้อมูลเสร็จ บันทึก log
            val Time = System.currentTimeMillis().toString()
            insertBackupLog(
                BackupLog(
                    date_time = Time,
                    description = description,
                    status = "Success",
                    actoin_username = accountname,
                    operation = "Backup"
                )
            )

            val sourceDatabaseFile = context.getDatabasePath("locker_database")
            val sourceShmFile = context.getDatabasePath("locker_database-shm")
            val sourceWalFile = context.getDatabasePath("locker_database-wal")

            val backupFolder = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)

            if (backupFolder != null) {
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



                    Log.d("Backup", "Backup completed successfully")

                } catch (e: IOException) {
                    Log.e("Backup", "Backup failed", e)
                }
            }
        }
    }


    // ฟังก์ชันสำหรับการ Restore
    fun performRestore(context: Context, accountname: String,description: String,) {
        viewModelScope.launch {
            val sourceBackupDatabaseFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "backup_locker_database")
            val sourceBackupShmFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "backup_locker_database-shm")
            val sourceBackupWalFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "backup_locker_database-wal")

            val destinationDatabaseFile = context.getDatabasePath("locker_database")
            val destinationShmFile = context.getDatabasePath("locker_database-shm")
            val destinationWalFile = context.getDatabasePath("locker_database-wal")


            val Time = System.currentTimeMillis().toString()
            insertBackupLog(
                BackupLog(
                    date_time = Time,
                    description = description,
                    status = "Success",
                    actoin_username = accountname,
                    operation = "Restore"
                )
            )
            // ตรวจสอบว่าไฟล์สำรองมีอยู่หรือไม่
            if (sourceBackupDatabaseFile.exists() && sourceBackupShmFile.exists() && sourceBackupWalFile.exists()) {
                try {
                    // ลบไฟล์ปัจจุบันก่อน
                    destinationDatabaseFile.delete()
                    destinationShmFile.delete()
                    destinationWalFile.delete()

                    // คัดลอกไฟล์สำรองกลับมา
                    sourceBackupDatabaseFile.inputStream().use { input ->
                        destinationDatabaseFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    sourceBackupShmFile.inputStream().use { input ->
                        destinationShmFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    sourceBackupWalFile.inputStream().use { input ->
                        destinationWalFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    Log.d("Restore", "Restore completed successfully")

                } catch (e: IOException) {
                    Log.e("Restore", "Restore failed", e)
                }
            } else {
                Log.e("Restore", "Backup files not found")
            }
        }
    }




    fun performBackupToPi(mqttService: MqttService, context: Context) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
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
                } catch (e: IOException) {
                    Log.e("Backup", "Backup failed", e)
                }
            }
        }
    }

    fun sendBackupFileToPi(context: Context,mqttService: MqttService, encodedFile: String, topic: String,) {
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

                mqttService.sendMessage("request/restore", "")

                // รอรับข้อมูลจาก MQTT
                val databaseTopic = "locker/restore/database"
                val shmTopic = "locker/restore/shm"
                val walTopic = "locker/restore/wal"

                // ใช้ CountDownLatch เพื่อตรวจสอบเมื่อได้รับข้อมูลครบ
                val latch = CountDownLatch(3)

                // ตั้งค่า listener
                mqttService.setOnMessageReceivedListener { topic, message ->
                    when (topic) {
                        databaseTopic -> {
                            restoredDatabase.append(String(message.payload))
                            latch.countDown() // เมื่อได้รับข้อมูลจาก database
                        }
                        shmTopic -> {
                            restoredShm.append(String(message.payload))
                            latch.countDown() // เมื่อได้รับข้อมูลจาก shm
                        }
                        walTopic -> {
                            restoredWal.append(String(message.payload))
                            latch.countDown() // เมื่อได้รับข้อมูลจาก wal
                        }
                    }
                }

                // สมัครรับข้อมูลจากแต่ละ topic
                mqttService.subscribeToTopic(databaseTopic)
                mqttService.subscribeToTopic(shmTopic)
                mqttService.subscribeToTopic(walTopic)

                // รอให้ข้อมูลครบ
                latch.await()  // รอจนกว่าจะได้รับข้อมูลครบทั้ง 3 topic

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

    fun updateBackupSettings(frequency: String, backupTime: String,description:String) {
        viewModelScope.launch {
            val newSettings = BackupSettings(
                frequency = frequency,
                backupTime = backupTime,
                description =description
            )
            BackupDao.insertOrUpdateBackupSettings(newSettings)
            _backupSettings.value = newSettings
        }
    }


    fun deleteBackupLog(backupLog: BackupLog) {
        viewModelScope.launch {
            backupLogDao.deleteBackupLog(backupLog)
        }
    }

    fun clearBackupLogs() {
        viewModelScope.launch {
            backupLogDao.clearBackupLogs()
        }
    }
    fun loadBackupData(context: Context) {
        viewModelScope.launch {
            // ตรวจสอบว่ามีไฟล์สำรองที่ต้องการโหลดหรือไม่
            val backupFolder = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            val backupDatabaseFile = File(backupFolder, "backup_locker_database")
            val backupShmFile = File(backupFolder, "backup_locker_database-shm")
            val backupWalFile = File(backupFolder, "backup_locker_database-wal")

            // ตรวจสอบว่าไฟล์สำรองมีอยู่หรือไม่
            if (backupDatabaseFile.exists() && backupShmFile.exists() && backupWalFile.exists()) {
                try {
                    // คัดลอกไฟล์จาก backup กลับไปยังฐานข้อมูลปัจจุบัน
                    val destinationDatabaseFile = context.getDatabasePath("locker_database")
                    val destinationShmFile = context.getDatabasePath("locker_database-shm")
                    val destinationWalFile = context.getDatabasePath("locker_database-wal")

                    // ลบไฟล์เดิมที่มีอยู่
                    destinationDatabaseFile.delete()
                    destinationShmFile.delete()
                    destinationWalFile.delete()

                    // คัดลอกไฟล์สำรองกลับไปยังที่เดิม
                    backupDatabaseFile.inputStream().use { input ->
                        destinationDatabaseFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    backupShmFile.inputStream().use { input ->
                        destinationShmFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    backupWalFile.inputStream().use { input ->
                        destinationWalFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    Log.d("Restore", "Restore completed successfully")
                } catch (e: IOException) {
                    Log.e("Restore", "Restore failed", e)
                }
            } else {
                Log.e("Restore", "Backup files not found")
            }
        }
    }

}