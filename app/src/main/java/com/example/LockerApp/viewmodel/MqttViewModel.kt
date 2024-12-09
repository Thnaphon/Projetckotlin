package com.example.LockerApp.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.LockerApp.service.MqttService
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

    // ฟังก์ชันสำหรับการ subscribe
    fun subscribeToTopic(topic: String) {
        viewModelScope.launch {
            mqttService.subscribeToTopic(topic)

        }
    }
    fun waitForMessages(topic: String, onMessageReceived: (String) -> Unit) {
        viewModelScope.launch {
            mqttService.subscribeToTopic(topic) // Subscribe to the topic
            receivedMessage.collect { message ->
                if (message.isNotEmpty()) {
                    onMessageReceived(message) // เรียก Callback เมื่อได้รับข้อความ
                    Log.d("MqttViewModel", "Message received from topic: $topic with message: $message")
                }
            }
        }
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

}
