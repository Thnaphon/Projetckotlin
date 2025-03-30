package com.example.LockerApp.service

import android.content.Context
import android.util.Log
import com.example.LockerApp.model.UsageLockerDao
import kotlinx.coroutines.delay
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.InputStream
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

class MqttService() {


    private val _connectionStatus = MutableStateFlow("Disconnected")
    val connectionStatus: StateFlow<String> = _connectionStatus

    private val _mqttData = MutableStateFlow(Pair("", ""))
    val mqttData: StateFlow<Pair<String, String>> = _mqttData

    // ทำให้ mqttClient เป็น non-nullable
    private var mqttClient: MqttClient? = null

    private var messageCallback: ((String) -> Unit)? = null

    // ฟังก์ชันสำหรับตั้งค่า messageCallback
    fun onMessageReceived(callback: (String) -> Unit) {
        this.messageCallback = callback
    }
    private var onMessageReceivedListener: ((String, MqttMessage) -> Unit)? = null
    fun setOnMessageReceivedListener(listener: (String, MqttMessage) -> Unit) {
        this.onMessageReceivedListener = listener
    }

    // ฟังก์ชันการเชื่อมต่อ
    fun connect(context: Context) {


        try {
            if (mqttClient?.isConnected == true) {
                mqttClient?.disconnect()
                _connectionStatus.value = "Disconnected"
                Log.d("Mqtt", "Disconnected from broker")
            }

            // สร้าง client
            mqttClient = MqttClient("ssl://172.20.10.7:8883", MqttClient.generateClientId(), null)


            val options = MqttConnectOptions()
            options.isCleanSession = true

            // โหลดไฟล์ CA และ Client Certificate
            val sslContext = context.createSslContext("ca.crt", "client.p12", "1234")

            options.socketFactory = sslContext.socketFactory

            // เชื่อมต่อกับ MQTT Broker
            mqttClient?.connect(options)
            _connectionStatus.value = "Connected"
            Log.d("Mqtt", "Connected to broker with TLS")

            // ตั้งค่าคอลแบ็ก
            mqttClient?.setCallback(object : org.eclipse.paho.client.mqttv3.MqttCallback {
                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    topic?.let { t ->
                        message?.let {
                            _mqttData.value = Pair(t, it.toString())
                            Log.d("Mqtt", "Topic: ${_mqttData.value.first}, Message: ${_mqttData.value.second}")
                        } ?: Log.d("Mqtt", "Received message is null on topic: $t")
                    } ?: Log.d("Mqtt", "Received topic is null")
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




    fun Context.createSslContext(
        caCrtFile: String,
        clientP12File: String,
        password: String
    ): SSLContext {
        try {
            // โหลด CA Certificate
            val caInput: InputStream = assets.open("certs/$caCrtFile")
            val caKeyStore = KeyStore.getInstance(KeyStore.getDefaultType())
            caKeyStore.load(null, null)
            caKeyStore.setCertificateEntry(
                "ca",
                java.security.cert.CertificateFactory.getInstance("X.509")
                    .generateCertificate(caInput)
            )
            caInput.close()

            // สร้าง TrustManager
            val trustManagerFactory =
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            trustManagerFactory.init(caKeyStore)

            // โหลด Client Certificate
            val clientInput: InputStream = assets.open("certs/$clientP12File")
            val clientKeyStore = KeyStore.getInstance("PKCS12")
            clientKeyStore.load(clientInput, password.toCharArray())
            clientInput.close()

            // สร้าง KeyManager
            val keyManagerFactory =
                KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
            keyManagerFactory.init(clientKeyStore, password.toCharArray())

            // ตั้งค่า SSL Context
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(keyManagerFactory.keyManagers, trustManagerFactory.trustManagers, null)
            return sslContext

        } catch (e: Exception) {
            throw RuntimeException("Failed to create SSL Context: ${e.message}", e)
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
        if (mqttClient == null || !mqttClient!!.isConnected) {
            Log.e("Mqtt", "MQTT Client is not connected. Cannot send message.")
            return
        }
        try {
            val mqttMessage = MqttMessage(message.toByteArray())
            mqttMessage.qos = 1 // QoS 1: ส่งใหม่ถ้ายังไม่ได้รับ
            mqttClient!!.publish(topic, mqttMessage)
            Log.d("MQTT", "Message sent to topic: $topic")
        } catch (e: Exception) {
            Log.e("Mqtt", "Failed to send MQTT message", e)
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

    fun clearMessage() {
        _mqttData.value = Pair("", "")
        Log.d("Mqtt", "Message cleared")
    }
}