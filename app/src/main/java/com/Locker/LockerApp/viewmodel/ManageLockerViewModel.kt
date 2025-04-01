package com.Locker.LockerApp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.Locker.LockerApp.model.LockerDatabase
import com.Locker.LockerApp.model.ManageLocker
import kotlinx.coroutines.launch

class ManageLockerViewModel(application: Application) : AndroidViewModel(application) {

    private val manageLockerDao = LockerDatabase.getDatabase(application).ManageLockerDao()


    // MutableLiveData สำหรับแสดงผลหรือการแสดงสถานะการเพิ่มข้อมูล
    private val _insertResult = MutableLiveData<Boolean>()
    val insertResult: LiveData<Boolean> = _insertResult



    // ฟังก์ชันในการเพิ่มข้อมูล
    fun insertManageLocker(locker_name: String, usageTime: String, usage: String, name_user: String,Status: String) {
        val manageLocker =ManageLocker(
            locker_name = locker_name,
            name_user = name_user,
            UsageTime = usageTime,
            Usage = usage,
            Status = Status
        )
        viewModelScope.launch {
            manageLockerDao.insert(manageLocker)  // เรียกใช้งาน DAO แทน repository
        }
    }
}



