package com.example.LockerApp.viewmodel


import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.LockerApp.model.Compartment
import com.example.LockerApp.model.CompartmentDao
import com.example.LockerApp.model.Locker
import com.example.LockerApp.model.LockerDao
import com.example.LockerApp.service.MqttService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

import kotlinx.coroutines.launch

import kotlinx.coroutines.flow.MutableStateFlow

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.eclipse.paho.client.mqttv3.MqttClient


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

    private val _lockers = MutableStateFlow<List<Locker>>(emptyList())
    val lockers: StateFlow<List<Locker>> get() = _lockers

    private val _lockerCount = MutableStateFlow(0)
    val lockerCount: StateFlow<Int> get() = _lockerCount

    init {
        loadLockers()
    }

    private fun loadLockers() {
        viewModelScope.launch {
            _lockers.value = lockerDao.getAllLockers()
            _lockerCount.value = lockerDao.getLockerCount() // เรียกใช้งานภายใน coroutine
        }
    }

    private fun loadTopics() {
        viewModelScope.launch {
            val topics = lockerDao.getAllLockers().map { it.topic_mqtt }
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

        val newLocker = Locker(Lockername=Lockername,detail = detail, status = status, topic_mqtt = topicMqtt,availableCompartment=availableCompartment )
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

    fun getMqttTopicFromDatabase(lockerId: Int): Flow<String?> = flow {
        val mqttTopic = lockerDao.getMqttTopicByLockerId(lockerId)
        emit(mqttTopic)
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
}
