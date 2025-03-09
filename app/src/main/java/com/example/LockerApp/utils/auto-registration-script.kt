package com.example.LockerApp.utils

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * This script can be used to automatically register faces during app initialization
 * or first run setup.
 */
object FaceRegistrationScript {
    private const val TAG = "FaceRegistrationScript"
    private const val PREF_NAME = "face_registration_prefs"
    private const val KEY_REGISTRATION_COMPLETED = "registration_completed"
    
    /**
     * Check if initial face registration has been completed
     */
    fun isRegistrationCompleted(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_REGISTRATION_COMPLETED, false)
    }
    
    /**
     * Mark registration as completed
     */
    private fun markRegistrationCompleted(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_REGISTRATION_COMPLETED, true).apply()
    }
    
    /**
     * Execute the registration process if it hasn't been done before
     * @param context Application context
     * @param lifecycleScope CoroutineScope to run the registration process
     * @param onComplete Callback when registration is complete
     */
    fun executeIfNeeded(
        context: Context,
        lifecycleScope: LifecycleCoroutineScope,
        onComplete: (success: Int, failures: Int) -> Unit
    ) {
        // Skip if already completed
        if (isRegistrationCompleted(context)) {
            Log.d(TAG, "Face registration already completed")
            return
        }
        
        Log.d(TAG, "Starting face registration process")
        
        // Run the registration in a coroutine
        lifecycleScope.launch {
            try {
                val utility = FaceRegistrationUtility(context.applicationContext)
                val (success, failures) = utility.registerFacesFromAssets("faces")
                
                // Mark as completed and notify
                markRegistrationCompleted(context)
                
                withContext(Dispatchers.Main) {
                    onComplete(success, failures)
                }
                
                Log.d(TAG, "Registration completed: $success success, $failures failures")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error during face registration", e)
                
                withContext(Dispatchers.Main) {
                    onComplete(0, 0)
                }
            }
        }
    }
    
    /**
     * Reset the registration status (for development/testing)
     */
    fun resetRegistrationStatus(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_REGISTRATION_COMPLETED, false).apply()
        Log.d(TAG, "Registration status reset")
    }
}
