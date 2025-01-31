package com.example.LockerApp.viewmodel

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class GoogleAuthViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GoogleAuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GoogleAuthViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}