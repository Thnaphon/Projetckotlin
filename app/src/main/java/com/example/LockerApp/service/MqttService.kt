package com.example.LockerApp.service

import android.util.Log
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MqttService {

    private val _connectionStatus = MutableStateFlow("Disconnected")
    val connectionStatus: StateFlow<String> = _connectionStatus

    private val _receivedMessage = MutableStateFlow("")
    val receivedMessage: StateFlow<String> = _receivedMessage

    // ทำให้ mqttClient เป็น non-nullable
    private var mqttClient: MqttClient? = null

    // ฟังก์ชันการเชื่อมต่อ
    fun connect() {
        try {
            if (mqttClient?.isConnected == true) {
                // ถ้ากำลังเชื่อมต่ออยู่ ให้ตัดการเชื่อมต่อก่อน
                mqttClient?.disconnect()
                _connectionStatus.value = "Disconnected"
                Log.d("Mqtt", "Disconnected from broker")
            }

            // กำหนดค่า mqttClient ใหม่
            mqttClient = MqttClient("tcp://test.mosquitto.org:1883", MqttClient.generateClientId(), null)

            val options = MqttConnectOptions()
            //options.userName = "your-username"  // ถ้ามี
            //options.password = "your-password".toCharArray()  // ถ้ามี

            // ตั้งค่าลิ้ง Last Will and Testament
            options.setWill("your/topic", "yourMessage".toByteArray(), 1, false) // false for retain

            // เชื่อมต่อกับ MQTT Broker
            mqttClient?.connect(options)
            _connectionStatus.value = "Connected"
            Log.d("Mqtt", "Connected to broker")

            // ตั้งค่าคอลแบ็กสำหรับการรับข้อความ
            mqttClient?.setCallback(object : org.eclipse.paho.client.mqttv3.MqttCallback {
                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    message?.let {
                        _receivedMessage.value = it.toString()
                        Log.d("Mqtt", "Received message: ${it.toString()} on topic: $topic")
                    }
                }

                override fun connectionLost(cause: Throwable?) {
                    _connectionStatus.value = "Disconnected"
                    Log.e("Mqtt", "Connection lost: ${cause?.message}")
                }

                override fun deliveryComplete(token: org.eclipse.paho.client.mqttv3.IMqttDeliveryToken?) {}
            })
        } catch (e: MqttException) {
            _connectionStatus.value = "Connection Failed"
            Log.e("Mqtt", "Failed to connect: ${e.message}")
        }
    }

    // ฟังก์ชันการตัดการเชื่อมต่อ
    fun disconnect() {
        try {
            mqttClient?.disconnect()
            _connectionStatus.value = "Disconnected"
        } catch (e: MqttException) {
            Log.e("Mqtt", "Failed to disconnect: ${e.message}")
        }
    }

    // ฟังก์ชันส่งข้อความ
    fun sendMessage(topic: String, message: String) {
        try {
            val mqttMessage = MqttMessage()
            mqttMessage.payload = message.toByteArray()
            mqttClient?.publish(topic, mqttMessage)
        } catch (e: MqttException) {
            Log.e("Mqtt", "Failed to send message: ${e.message}")
        }
    }

    // ฟังก์ชันสำหรับการ subscribe โดยรับพารามิเตอร์หัวข้อเอง
    fun subscribeToTopic(topic: String) {
        if (mqttClient?.isConnected == true) {
            try {
                mqttClient?.subscribe(topic, 1)
                Log.d("Mqtt", "Subscribed to topic: $topic")
            } catch (e: MqttException) {
                Log.e("Mqtt", "Failed to subscribe to topic: ${e.message}")
            }
        } else {
            Log.e("Mqtt", "MQTT Client is not connected. Cannot subscribe.")
        }
    }

    // ฟังก์ชันสำหรับให้ ViewModel เข้าถึง mqttClient
    fun getClient(): MqttClient? {
        return mqttClient
    }
    // ฟังก์ชันสำหรับการ unsubscribe
    fun unsubscribeFromTopic(topic: String) {
        if (mqttClient?.isConnected == true) {
            try {
                mqttClient?.unsubscribe(topic)
                Log.d("Mqtt", "Unsubscribed from topic: $topic")
            } catch (e: MqttException) {
                Log.e("Mqtt", "Failed to unsubscribe from topic: ${e.message}")
            }
        } else {
            Log.e("Mqtt", "MQTT Client is not connected. Cannot unsubscribe.")
        }
    }




}