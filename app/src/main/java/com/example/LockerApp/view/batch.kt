package com.example.LockerApp.view

import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.LockerApp.R
import com.example.LockerApp.utils.FaceRegistrationUtility
import kotlinx.coroutines.launch

class BatchRegistrationActivity : AppCompatActivity() {

    private lateinit var btnRegister: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvStatus: TextView
    private lateinit var registrationUtility: FaceRegistrationUtility

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_batch_registration)

        // Initialize views
        btnRegister = findViewById(R.id.btnRegister)
        progressBar = findViewById(R.id.progressBar)
        tvStatus = findViewById(R.id.tvStatus)

        // Initialize utility
        registrationUtility = FaceRegistrationUtility(applicationContext)

        // Set up button click listener
        btnRegister.setOnClickListener {
            startRegistration()
        }
    }

    private fun startRegistration() {
        // Show loading state
        btnRegister.isEnabled = false
        progressBar.isVisible = true
        tvStatus.text = "Registration in progress..."

        // Launch coroutine for registration
        lifecycleScope.launch {
            try {
                // The directory in assets containing person folders
                val assetDir = "faces"

                // Start registration process
                val (successCount, failureCount) = registrationUtility.registerFacesFromAssets(assetDir)

                // Update UI
                tvStatus.text = "Registration complete!\nSuccess: $successCount\nFailures: $failureCount"
                Toast.makeText(
                    this@BatchRegistrationActivity,
                    "Registration complete: $successCount faces registered",
                    Toast.LENGTH_LONG
                ).show()

            } catch (e: Exception) {
                tvStatus.text = "Error: ${e.message}"
                Toast.makeText(
                    this@BatchRegistrationActivity,
                    "Registration failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                // Reset UI state
                progressBar.isVisible = false
                btnRegister.isEnabled = true
            }
        }
    }
}