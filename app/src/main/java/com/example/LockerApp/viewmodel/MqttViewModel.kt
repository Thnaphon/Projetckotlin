package com.example.LockerApp.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.LockerApp.model.Locker
import com.example.LockerApp.model.LockerDao
import com.example.LockerApp.model.LockerDatabase
import com.example.LockerApp.service.MqttService
import com.example.LockerApp.view.Message
import com.google.gson.Gson
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.MqttClient

class MqttViewModel( application: Application) : AndroidViewModel(application) {

    private val mqttService = MqttService()

    private val lockerDatabase = LockerDatabase.getDatabase(application)
    private val lockerDao = lockerDatabase.lockerDao()
    // ตัวแปร mqttClient ที่จะใช้งาน
    val mqttClient: MqttClient? = mqttService.getClient()

    // สถานะการเชื่อมต่อ
    val connectionStatus: StateFlow<String> = mqttService.connectionStatus

    // ข้อความที่ได้รับ
    // ข้อความที่ได้รับจาก MQTT
    private val _mqttData = MutableStateFlow(Pair("", ""))
    val mqttData: StateFlow<Pair<String, String>> = _mqttData

    private val _lockerList = MutableStateFlow<List<Locker>>(emptyList())
    val lockerList: StateFlow<List<Locker>> = _lockerList




    init {
        // เชื่อมต่อกับ MQTT broker เมื่อเริ่มต้น

        viewModelScope.launch {
            mqttService.connect(getApplication<Application>().applicationContext)
            mqttService.subscribeToTopic("respond/locker")
            _lockerList.value = lockerDao.getAllLockers()
            lockerList.collect { lockers ->
                lockers.forEach { locker ->
                    locker.availableCompartment.forEach{ compartment ->
                        if (compartment != ',') {
                            mqttService.subscribeToTopic("${locker.TokenTopic}/borrow/$compartment/status")
                            mqttService.subscribeToTopic("${locker.TokenTopic}/return/$compartment/status")
                        }
                    }
                }
            }
        }
        observeMqttData()

    }

    // ฟังก์ชันเชื่อมต่อ
    fun connect() {
        viewModelScope.launch {
            mqttService.connect(getApplication<Application>().applicationContext)
        }
    }
    fun isConnected(): Boolean {
        return mqttClient?.isConnected == true
    }

    // ฟังก์ชันตัดการเชื่อมต่อ
    fun disconnect() {
        viewModelScope.launch {
            mqttService.disconnect()
        }

    }

    // ฟังก์ชันส่งข้อความ
    fun sendMessage(topic: String, message: String) {
        viewModelScope.launch {
            mqttService.sendMessage(topic, message)
            Log.d("MqttViewModel", "Message sent to topic: $topic with message: $message")


        }
    }
    fun sendMessageJson(topic: String, message: Message) {
        viewModelScope.launch {
            val gson = Gson()

            // แปลง Message เป็น JSON
            val jsonMessage = gson.toJson(message)

            // ส่ง JSON ผ่าน MQTT
            mqttService.sendMessage(topic, jsonMessage)

            Log.d("MqttViewModel", "Message type: ${message::class.java.name} sent to topic: $topic with message: $jsonMessage")
        }
    }

    // ฟังก์ชันสำหรับการ subscribe
    // สร้าง StateFlow สำหรับเก็บสถานะที่ได้รับจาก MQTT
    private val _statusFlow = MutableStateFlow<String>("")
    val statusFlow: StateFlow<String> = _statusFlow

    fun subscribeToTopic(topic: String) {
        viewModelScope.launch {

            mqttService.subscribeToTopic(topic)

        }
    }

//    fun waitForMessages(topic: String, onMessageReceived: (String) -> Unit) {
//        job?.cancel()  // Canc
//        job = viewModelScope.launch {
//            // Subscribe to the topic
//            receivedMessage.collect { message ->
//                if (message.isNotEmpty()) {
//                    onMessageReceived(message) //0t]v'
//                    Log.d("MqttViewModelwiatfor", "Message received from topic: $topic with message: $message")
//                }
//                clearMessage()  // Clear ข้อความหลังจากรับแล้ว
//            }
//        }
//    }



    fun clearMessage() {
        mqttService.clearMessage()
    }




    fun cancelWaitingForMessages() {
        job?.cancel()  // หยุดการรับข้อความ
    }


    fun unsubscribeFromTopic(topic: String) {
        viewModelScope.launch {
            mqttService.unsubscribeFromTopic(topic)
            Log.d("MqttViewModel", "Unsubscribed from topic: $topic")
        }
    }


    // สร้าง LiveData สำหรับสถานะ
    private val _statusLiveData = MutableLiveData<String>()
    val statusLiveData: LiveData<String> = _statusLiveData

    // ฟังก์ชันในการสังเกต topic


    //    fun clearReceivedMessage() {
//        viewModelScope.launch {
//            _receivedMessageTopic.emit("")
//
//        }
//    }
    var job: Job? = null

    fun observeMqttData() {
        if (job?.isActive != true) { // ตรวจสอบว่า job กำลังทำงานหรือไม่
            job = viewModelScope.launch {
                mqttService.mqttData.collect { (topic, message) ->
                    _mqttData.value = Pair(topic, message)
                    Log.d("observeMqttData", "MQTT Topic: $topic, Message: $message")
                }
            }
        }
    }



}
