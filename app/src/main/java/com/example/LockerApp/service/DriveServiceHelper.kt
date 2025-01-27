package com.example.LockerApp.service


import android.content.Context
import android.util.Log
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.InputStreamContent
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File // ใช้ model.File จาก Google Drive API
import com.google.auth.oauth2.GoogleCredentials
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream

class DriveServiceHelper(private val context: Context) {

    private val scopes = listOf(DriveScopes.DRIVE_FILE)

    // ฟังก์ชันนี้จะโหลดไฟล์จาก "credentials.json" และสร้าง Google Drive Service
    fun getDriveService(): Drive {
        try {
            // โหลดข้อมูลจากไฟล์ credentials.json
            val credentialsStream: InputStream = context.assets.open("credentials.json")
            val credentials = GoogleCredentials.fromStream(credentialsStream)
                .createScoped(scopes)

            // ใช้ credentials เพื่อเป็น HttpRequestInitializer
            val requestInitializer: HttpRequestInitializer = credentials as HttpRequestInitializer

            // สร้าง Google Drive Service
            return Drive.Builder(
                com.google.api.client.http.javanet.NetHttpTransport(),
                JacksonFactory.getDefaultInstance(), // ใช้ JacksonFactory แทน
                requestInitializer
            ).setApplicationName("LockerApp").build()

        } catch (e: IOException) {
            Log.e("DriveServiceHelper", "Error initializing Drive service", e)
            throw e
        }
    }

    // ฟังก์ชันการอัปโหลดไฟล์ไปยัง Google Drive
    fun uploadFile(fileName: String, mimeType: String, filePath: String): File { // ใช้ model.File แทน
        val fileMetadata = File()
        fileMetadata.name = fileName

        val fileContent = java.io.File(filePath)
        val fileStream = FileInputStream(fileContent)

        val mediaContent = InputStreamContent(mimeType, fileStream)

        return getDriveService().files().create(fileMetadata, mediaContent)
            .setFields("id")
            .execute()
    }
}