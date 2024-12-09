package com.example.LockerApp.view

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.example.LockerApp.viewmodel.MqttViewModel


@Composable
fun MqttScreen(viewModel: MqttViewModel, navController: NavHostController) {
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val receivedMessage by viewModel.receivedMessage.collectAsState()

    // State สำหรับข้อความที่ต้องการส่ง
    var messageToSend by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // แสดงสถานะการเชื่อมต่อ
        Text(
            text = "Connection Status: $connectionStatus",
            style = MaterialTheme.typography.h6
        )

        Spacer(modifier = Modifier.height(20.dp))

        // แสดงข้อความที่ได้รับ
//        Text(
//            text = "Received Message: $receivedMessage",
//            style = MaterialTheme.typography.body1
//        )



        Spacer(modifier = Modifier.height(10.dp))

        // ปุ่มเชื่อมต่อ
        Button(
            onClick = { viewModel.connect() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Connect")
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ปุ่มตัดการเชื่อมต่อ
        Button(
            onClick = { viewModel.disconnect() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Disconnect")
        }


        Spacer(modifier = Modifier.height(20.dp))

        // ปุ่ม Next ที่จะทำงานเมื่อ connectionStatus คือ "Connected"
        if (connectionStatus == "Connected") {
            Button(
                onClick = {
                    // นำทางไปยัง MainMenuUI
                    navController.navigate("main_menu")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Next")
            }
        }
    }
}
