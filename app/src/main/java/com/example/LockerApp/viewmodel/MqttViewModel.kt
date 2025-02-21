package com.example.LockerApp.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.LockerApp.service.MqttService
import com.example.LockerApp.view.Message
import com.google.gson.Gson
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.MqttClient

class MqttViewModel(application: Application) : AndroidViewModel(application) {

    private val mqttService = MqttService()

    // ตัวแปร mqttClient ที่จะใช้งาน
    val mqttClient: MqttClient? = mqttService.getClient()

    // สถานะการเชื่อมต่อ
    val connectionStatus: StateFlow<String> = mqttService.connectionStatus

    // ข้อความที่ได้รับ
    val receivedMessage: StateFlow<String> = mqttService.receivedMessage

    private val _receivedMessageTopic = MutableStateFlow<String>("")
    val receivedMessageTopic: StateFlow<String> get() = _receivedMessageTopic


    init {
        // เชื่อมต่อกับ MQTT broker เมื่อเริ่มต้น
        viewModelScope.launch {
            mqttService.connect()
            subscribeToTopic("respond/locker")
        }

    }

    // ฟังก์ชันเชื่อมต่อ
    fun connect() {
        viewModelScope.launch {
            mqttService.connect()
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

            Log.d("MqttViewModelcheckkut", "Message type: ${message::class.java.name} sent to topic: $topic with message: $jsonMessage")
        }
    }

    // ฟังก์ชันสำหรับการ subscribe
    // สร้าง StateFlow สำหรับเก็บสถานะที่ได้รับจาก MQTT
    private val _statusFlow = MutableStateFlow<String>("")
    val statusFlow: StateFlow<String> = _statusFlow

    fun subscribeToTopic(topic: String) {
        viewModelScope.launch {
            mqttService.subscribeToTopic(topic)
            mqttService.onMessageReceived { message ->
                // เมื่อได้รับข้อความจาก MQTT, ส่งข้อความไปยัง _statusFlow
                _receivedMessageTopic.value =  message
            }
        }
    }
    var job: Job? = null

    fun waitForMessages(topic: String, onMessageReceived: (String) -> Unit) {
        job = viewModelScope.launch {
            // Subscribe to the topic
            mqttService.subscribeToTopic(topic)

            receivedMessage.collect { message ->
                if (message.isNotEmpty()) {
                    onMessageReceived(message)
                    Log.d("MqttViewModelcheck", "Message received from topic: $topic with message: $message")

                    // เคลียร์ค่าหลังจากหน่วงเวลาเล็กน้อย
                    delay(500) // ป้องกันการเคลียร์เร็วเกินไป
                    clearReceivedMessage()
                }
            }
        }
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


    fun clearRetainedMessage(topic: String) {
        // ส่งข้อความว่างและตั้งค่า retain เป็น true เพื่อเคลียร์ retained message
        val message = "" // ข้อความว่าง
        val qos = 0 // Quality of Service
        val retain = true  // ตั้งค่า retain เป็น true

        mqttClient?.publish(topic, message.toByteArray(), qos, retain)
            ?: Log.e("MqttViewModel", "MqttClient is null, unable to clear retained message.")
    }

    // สร้าง LiveData สำหรับสถานะ
    private val _statusLiveData = MutableLiveData<String>()
    val statusLiveData: LiveData<String> = _statusLiveData

    // ฟังก์ชันในการสังเกต topic
    fun clearReceivedMessage() {
        _receivedMessageTopic.value = "" // เซ็ตค่าให้เป็นค่าว่าง
    }


}
