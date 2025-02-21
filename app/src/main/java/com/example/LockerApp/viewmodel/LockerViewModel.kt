package com.example.LockerApp.viewmodel


import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.LockerApp.model.Compartment
import com.example.LockerApp.model.CompartmentDao
import com.example.LockerApp.model.Locker
import com.example.LockerApp.model.LockerDao
import com.example.LockerApp.service.MqttService
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

import kotlinx.coroutines.launch

import kotlinx.coroutines.flow.MutableStateFlow

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.eclipse.paho.client.mqttv3.MqttClient

import kotlinx.coroutines.flow.stateIn

class LockerViewModel(private val lockerDao: LockerDao,private val compartmentDao: CompartmentDao) : ViewModel() {

    private val mqttService = MqttService()

    private val _topicMqttList = MutableStateFlow<List<String>>(emptyList())
    val topicMqttList: StateFlow<List<String>> get() = _topicMqttList

    // ตัวแปร mqttClient ที่จะใช้งาน
    val mqttClient: MqttClient? = mqttService.getClient()

    // สถานะการเชื่อมต่อ
    val connectionStatus: StateFlow<String> = mqttService.connectionStatus

    // ข้อความที่ได้รับ
    val receivedMessage: StateFlow<String> = mqttService.receivedMessage



    private val _lockers = MutableStateFlow<List<Locker>>(emptyList()) // กำหนดค่าเริ่มต้น
    val lockers: StateFlow<List<Locker>> = _lockers

    private val _lockerCount = MutableStateFlow(0) // กำหนดค่าเริ่มต้น
    val lockerCount: StateFlow<Int> = _lockerCount

    init {
        loadLockers()
    }

    private val _uploadResult = MutableLiveData<Result<String>>()
    val uploadResult: LiveData<Result<String>> = _uploadResult

    private fun loadLockers() {
        viewModelScope.launch {
            try {
                val lockersFromDb = lockerDao.getAllLockers() // ดึงข้อมูลจากฐานข้อมูล
                _lockers.value = lockersFromDb // อัพเดตค่าใน _lockers
                _lockerCount.value = lockerDao.getLockerCount() // อัพเดตจำนวนล็อคเกอร์
            } catch (e: Exception) {
                Log.e("LockerViewModel", "Error loading lockers", e)
            }
        }
    }


    private fun loadTopics() {
        viewModelScope.launch {
            val topics = lockerDao.getAllLockers().map { it.TokenTopic }
            _topicMqttList.value = topics.filterNotNull() // เก็บเฉพาะ topic ที่ไม่เป็น null
        }
    }


    // ฟังก์ชันเพิ่มล็อคเกอร์ใหม่
// ฟังก์ชันเพิ่มล็อคเกอร์ใหม่
    fun addLocker(Lockername: String,detail: String, receivedTopic: String, availableCompartment:String) {
        val lockerName = "Locker ${_lockerCount.value + 1}"  // เพิ่มหมายเลขหลังจากจำนวน locker
        val status = "Available"
        val topicMqtt = receivedTopic    // สร้าง topic_mqtt ใหม่
        val availableCompartment = availableCompartment

        val newLocker = Locker(Lockername=Lockername,detail = detail, status = status, TokenTopic = topicMqtt,availableCompartment=availableCompartment )
        viewModelScope.launch {
            lockerDao.insertLocker(newLocker)
            // อัปเดตจำนวนล็อคเกอร์หลังจากเพิ่ม
            _lockerCount.value = _lockerCount.value + 1
            // รีเฟรชข้อมูลล็อคเกอร์ใหม่
            loadLockers()
        }
    }

    fun getCompartmentsByLocker(lockerId: Int): Flow<List<Compartment>> = flow {
        val compartments = compartmentDao.getCompartmentsByLocker(lockerId)
        emit(compartments)
    }

    // ฟังก์ชันสำหรับเพิ่ม Compartment
    fun addCompartment(compartment: Compartment) {
        viewModelScope.launch {
            compartmentDao.insertCompartment(compartment)
        }
    }

    fun updateCompartmentStatus(compartmentID: Int, newStatus: String, lockerID: Int) {
        viewModelScope.launch {
            try {
                // ตรวจสอบว่า LockerID มีอยู่ในตาราง locker หรือไม่
                val lockerExists = compartmentDao.checkLockerExists(lockerID)
                if (lockerExists) {
                    // ถ้ามี LockerID ในตาราง Locker ให้ทำการอัปเดตสถานะ
                    compartmentDao.updateCompartmentStatus(compartmentID, newStatus, lockerID)
                } else {
                    Log.e("LockerViewModel", "LockerID does not exist")
                }
            } catch (e: Exception) {
                // จัดการข้อผิดพลาดที่เกิดขึ้น
                Log.e("LockerViewModel", "Error updating compartment status", e)
            }
        }
    }



    fun getMqttTopicFromDatabase(lockerId: Int): Flow<String?> = flow {
        val mqttTopic = lockerDao.getMqttTopicByLockerId(lockerId)
        emit(mqttTopic)
    }

    fun getavailableCompartmentByLockerId(lockerId: Int): Flow<String?> = flow {
        val availableCompartment = lockerDao.getavailableCompartmentByLockerId(lockerId)
        emit(availableCompartment)
    }
    fun parseAvailableCompartment(availableCompartment: String?): List<Int> {
        return try {
            // ถ้า availableCompartment เป็น null ให้คืนค่าเป็น List ว่าง
            availableCompartment?.let {
                val gson = Gson()
                gson.fromJson(it, Array<Int>::class.java).toList()
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList() // ถ้ามีข้อผิดพลาดก็จะคืนค่าเป็น List ว่าง
        }
    }
    fun getMqttTopicForCompartment(compartmentId: Int): Flow<String?> {
        return flow {
            // ดึง LockerID จาก Compartment
            val lockerId = compartmentDao.getLockerIdByCompartmentId(compartmentId)

            // ถ้า lockerId ไม่เป็น null, ให้ดึง MQTT topic
            if (lockerId != null) {
                val mqttTopic = lockerDao.getMqttTopicByLockerId(lockerId)
                emit(mqttTopic)
            } else {
                emit(null) // คืนค่า null ถ้าไม่พบ LockerID
            }
        }.flowOn(Dispatchers.IO)
    }

    fun updateLockerStatus(lockerID: Int, newStatus: String) {
        viewModelScope.launch {
            lockerDao.updateLockerStatus(lockerID, newStatus)
        }
    }

    // ใน ViewModel


    fun getAllLockers() {
        viewModelScope.launch {
            // ดึงข้อมูลจาก Dao หรือ Repository
            val lockersFromDb = lockerDao.getAllLockers() // สมมติว่า getAllLockers() คืนค่า List<Locker>
            _lockers.value = lockersFromDb
        }
    }

    fun CheckupdateCompartmentStatus(compartmentIds: List<String>, lockerId: Int) {
        viewModelScope.launch {
            val allCompartments = getCompartmentsByLocker(lockerId).first() // ดึงข้อมูลทั้งหมดก่อน
            allCompartments.forEach { compartment ->
                val newStatus = if (compartment.CompartmentID.toString() in compartmentIds) {
                    "available"
                } else {
                    "unavailable"
                }
                updateCompartmentStatus(compartment.CompartmentID, newStatus,lockerId)
            }
        }
    }




    // เพิ่มฟังก์ชันนี้ใน LockerViewModel



}
