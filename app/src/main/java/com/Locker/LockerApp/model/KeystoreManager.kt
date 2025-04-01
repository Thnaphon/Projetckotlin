package com.Locker.LockerApp.model

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import java.security.KeyStore

object KeystoreManager {
    private const val KEY_ALIAS = "KEY_ALIAS"

    fun generateKey() {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)

        // ตรวจสอบว่าคีย์มีอยู่แล้วหรือไม่
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            keyGenerator.init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
            )
            keyGenerator.generateKey()
        }
    }


    fun encryptData(data: String): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getKey())
        val iv = cipher.iv
        val encryptedData = cipher.doFinal(data.toByteArray())

        // Log ดูว่าค่าที่ได้ออกมาคืออะไร
        Log.d("KeystoreManager", "Encrypt IV: ${iv.joinToString()}")
        Log.d("KeystoreManager", "Encrypted Data: ${encryptedData.joinToString()}")

        return Pair(encryptedData, iv) // **แก้ให้ encryptedData มาก่อน iv**
    }

    fun decryptData(encryptedData: ByteArray, iv: ByteArray): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getKey(), GCMParameterSpec(128, iv))

        // Log ดูค่าก่อนถอดรหัส
        Log.d("KeystoreManager", "Decrypt IV: ${iv.joinToString()}")
        Log.d("KeystoreManager", "Encrypted Data: ${encryptedData.joinToString()}")

        return String(cipher.doFinal(encryptedData))
    }


    private fun getKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }
}