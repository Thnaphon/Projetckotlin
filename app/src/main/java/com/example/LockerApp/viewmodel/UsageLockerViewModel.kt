package com.example.LockerApp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.example.LockerApp.model.LockerDatabase
import com.example.LockerApp.model.UsageLocker
import com.example.LockerApp.model.UsageLockerDao
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class UsageLockerViewModel(application: Application) : AndroidViewModel(application) {
    private val usageLockerDao: UsageLockerDao = LockerDatabase.getDatabase(application).usageLockerDao()
    val allUsageLockers: LiveData<List<UsageLocker>> = usageLockerDao.getAllUsageLockers()

    // ฟังก์ชันลบข้อมูล
    fun deleteUsageLocker(usageLocker: UsageLocker) {
        viewModelScope.launch {
            usageLockerDao.delete(usageLocker)
        }
    }

    // ฟังก์ชันสำหรับเพิ่มหรืออัพเดตข้อมูล (ถ้าต้องการ)
    fun addOrUpdateUsageLocker(usageLocker: UsageLocker) {
        viewModelScope.launch {
            // เพิ่มหรืออัพเดตข้อมูลตามต้องการ
            if (usageLocker.UsageLockerID == 0) {
                // ถ้า UsageLockerID = 0 หมายถึงเป็นข้อมูลใหม่
                usageLockerDao.insert(usageLocker)
            } else {
                // ถ้ามีการแก้ไขข้อมูล
                usageLockerDao.update(usageLocker)
            }
        }
    }
    fun insertUsageLocker(lockerId: Int, compartmentId: Int, usageTime: String, usage: String, AccountID: Int,Status: String) {
        val usageLocker = UsageLocker(
            LockerID = lockerId,
            AccountID = AccountID,
            CompartmentID = compartmentId,
            UsageTime = usageTime,
            Usage = usage,
            Status = Status
        )
        viewModelScope.launch {
            usageLockerDao.insert(usageLocker)  // เรียกใช้งาน DAO แทน repository
        }
    }

}