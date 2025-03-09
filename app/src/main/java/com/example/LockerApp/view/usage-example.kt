package com.example.LockerApp.view

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.LockerApp.R
import com.example.LockerApp.utils.FaceRegistrationScript

class SplashActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        
        // For development/testing: uncomment to reset registration status
        // FaceRegistrationScript.resetRegistrationStatus(applicationContext)
        
        // Check and execute face registration if needed
        FaceRegistrationScript.executeIfNeeded(
            applicationContext,
            lifecycleScope
        ) { success, failures ->
            // Display notification only if faces were registered
            if (success > 0) {
                Toast.makeText(
                    this,
                    "Registered $success faces for recognition",
                    Toast.LENGTH_SHORT
                ).show()
            }
            
            // Continue with app initialization or navigation
            // For example:
            // navigateToNextScreen()
        }
    }
    
    // Example navigation method
    private fun navigateToNextScreen() {
        // Add your navigation logic here
        // For example:
        // startActivity(Intent(this, MainActivity::class.java))
        // finish()
    }
}
