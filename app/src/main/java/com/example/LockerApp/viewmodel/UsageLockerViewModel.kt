package com.example.LockerApp.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.example.LockerApp.model.LockerDatabase
import com.example.LockerApp.model.UsageLocker
import com.example.LockerApp.model.UsageLockerDao
import androidx.lifecycle.viewModelScope
import com.example.LockerApp.model.ManageLocker
import com.example.LockerApp.model.ManageLockerDao
import com.example.LockerApp.service.MqttService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.MqttClient

class UsageLockerViewModel(application: Application) : AndroidViewModel(application) {
    private val usageLockerDao: UsageLockerDao = LockerDatabase.getDatabase(application).usageLockerDao()
    val allUsageLockers: LiveData<List<UsageLocker>> = usageLockerDao.getAllUsageLockers()

    private val manageLockerDao: ManageLockerDao = LockerDatabase.getDatabase(application).ManageLockerDao()
    val allManageLockers: LiveData<List<ManageLocker>> = manageLockerDao.getAllManageLockers()

    private val mqttService = MqttService()
    val mqttClient: MqttClient? = mqttService.getClient()

//    val mqttTopic: StateFlow<String> = mqttService.mqttTopic

    // ตัวแปร mqttClient ที่จะใช้งาน


    // สถานะการเชื่อมต่อ
    val connectionStatus: StateFlow<String> = mqttService.connectionStatus

    // ข้อความที่ได้รับ
    // ข้อความที่ได้รับจาก MQTT
//    private val _receivedMessageTopic = MutableStateFlow("")
//    val receivedMessage: StateFlow<String> = mqttService.receivedMessage



    init {
        // เชื่อมต่อกับ MQTT broker เมื่อเริ่มต้น
        viewModelScope.launch {
            mqttService.connect(getApplication<Application>().applicationContext)
            mqttService.subscribeToTopic("respond/locker")

        }


    }

    // ฟังก์ชันเชื่อมต่อ
    fun connect() {
        viewModelScope.launch {
            mqttService.connect(getApplication<Application>().applicationContext)
        }
    }

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
    fun insertUsageLocker(locker_name: String, compartmentId: Int, usageTime: String, usage: String, name_user: String,Status: String,name_equipment:String,accountID:Int) {
        val usageLocker = UsageLocker(
            locker_name = locker_name,
            name_user = name_user,
            number_compartment = compartmentId,
            UsageTime = usageTime,
            Usage = usage,
            Status = Status,
            name_equipment= name_equipment,
            accountID=accountID
        )
        viewModelScope.launch {
            usageLockerDao.insert(usageLocker)  // เรียกใช้งาน DAO แทน repository
            usageLockerDao.getAllUsageLockers()
        }
    }

//    fun observeMqttData() {
//        viewModelScope.launch {
//            mqttTopic.collect { message ->
//                Log.d("observeMqttData", "Received MQTT message: $message")
//            }
//        }
//    }


}