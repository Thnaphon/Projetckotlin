package com.Locker.LockerApp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.Locker.LockerApp.model.LockerDao
import com.Locker.LockerApp.model.CompartmentDao  // เพิ่มการนำเข้า CompartmentDao

class LockerViewModelFactory(
    private val lockerDao: LockerDao,
    private val compartmentDao: CompartmentDao  // เพิ่ม CompartmentDao เข้ามา
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LockerViewModel::class.java)) {
            return LockerViewModel(lockerDao, compartmentDao) as T  // ส่ง CompartmentDao เข้าไปด้วย
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}