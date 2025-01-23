package com.example.LockerApp.view

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.LockerApp.viewmodel.LockerViewModel

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add

import androidx.compose.runtime.*
import androidx.navigation.NavController
import com.example.LockerApp.model.CompartmentDao

import com.example.LockerApp.model.Locker
import com.example.LockerApp.model.LockerDao
import com.example.LockerApp.viewmodel.LockerViewModelFactory
import com.example.LockerApp.viewmodel.MqttViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun LockerUI(navController: NavController, lockerDao: LockerDao, compartmentDao: CompartmentDao, onLockerClick: (String) -> Unit) {
    val viewModel: LockerViewModel = viewModel(factory = LockerViewModelFactory(lockerDao, compartmentDao))
    val mqttViewModel: MqttViewModel = viewModel()
    val lockers by viewModel.lockers.collectAsState()
    val lockerCount by viewModel.lockerCount.collectAsState()

    var showAddLockerScreen by remember { mutableStateOf(false) }
    val topicMqttList by viewModel.topicMqttList.collectAsState(initial = emptyList())

    LaunchedEffect(lockers) {
        Log.d("LockerUI", "Lockers count updated: ${lockers.size}")
    }

/*
    LaunchedEffect(topicMqttList) {
        coroutineScope { // สร้าง coroutine scope ใหม่
            topicMqttList.forEach { topic ->
                launch { // สร้าง coroutine ใหม่สำหรับแต่ละ topic
                    val newTopic = "$topic/isconnect" // สร้าง topic ใหม่
                    mqttViewModel.sendMessage(newTopic, "") // ส่งไปยัง
                    mqttViewModel.subscribeToTopic(newTopic)
                }
            }
        }
    }
*/
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Total Lockers: $lockerCount", style = MaterialTheme.typography.h6)

            IconButton(onClick = {
                showAddLockerScreen = true
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Locker")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (showAddLockerScreen) {
            AddLockerScreen(
                mqttViewModel = mqttViewModel,
                viewModel = viewModel,
                navController = navController,
                lockerDao = lockerDao
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(lockers.size, key = { index -> lockers[index].LockerID }) { index ->
                    val locker = lockers[index]
                    LockerCard(locker) {
                        onLockerClick(locker.LockerID.toString()) // เรียกใช้ onLockerClick เมื่อการ์ดถูกคลิก
                    }
                }
            }
        }
    }
}


@Composable
fun LockerCard(locker: Locker, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() }, // เรียกใช้ lambda function
        elevation = 4.dp,
        backgroundColor = MaterialTheme.colors.surface
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text("${locker.Lockername}", style = MaterialTheme.typography.h6)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Status: ${locker.status}", style = MaterialTheme.typography.body1)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Detail: ${locker.detail}", style = MaterialTheme.typography.body2)
        }
    }
}


@Composable
fun AddLockerScreen(
    mqttViewModel: MqttViewModel,
    viewModel: LockerViewModel,
    navController: NavController,
    lockerDao: LockerDao
) {
    var lockerDetail by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) } // สถานะการบันทึก
    var receivedTopic by remember { mutableStateOf("") }
    var availableCompartment by remember { mutableStateOf("") }// สถานะเก็บข้อความที่ได้รับจาก send/topic
    var Lockername by remember { mutableStateOf("") }// สถานะเก็บข้อความที่ได้รับจาก send/topic

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Add New Locker", style = MaterialTheme.typography.h6)

        Spacer(modifier = Modifier.height(20.dp))

        TextField(
            value = Lockername,
            onValueChange = { Lockername = it },
            label = { Text("Enter Locker Name") },
            modifier = Modifier.fillMaxWidth()
        )
        TextField(
            value = lockerDetail,
            onValueChange = { lockerDetail = it },
            label = { Text("Enter Locker Detail") },
            modifier = Modifier.fillMaxWidth()
        )


        Spacer(modifier = Modifier.height(20.dp))

        Button(onClick = {
            isSaving = true

            receivedTopic = "mock/topic"
            availableCompartment = "10"
            viewModel.addLocker(
                Lockername,
                lockerDetail,
                receivedTopic,  // mock data
                availableCompartment // mock data
            )
            isSaving = false
            // ส่งข้อความและรอรับการตอบกลับ
//            mqttViewModel.waitForMessages("request/locker") { message ->
//                if (message.isNotEmpty()) {
//                    try {
//                        // แปลง JSON ที่ได้รับเป็น JSONObject
//                        val jsonObject = JSONObject(message)
//
//                        // แยกข้อมูล receivedTopic และ Available_compartment
//                        receivedTopic = jsonObject.getString("Topic")
//                        availableCompartment = jsonObject.getString("Compartment")
//
//                        // บันทึกข้อมูลลงในฐานข้อมูล
//                        viewModel.addLocker(
//                            Lockername,
//                            lockerDetail,
//                            receivedTopic,  // บันทึก receivedTopic
//                            availableCompartment,
//                            // บันทึก Available_compartment
//                        )
//
//                        isSaving = false // เปลี่ยนสถานะการบันทึก
//                    } catch (e: Exception) {
//                        Log.e("LockerUI", "Error parsing JSON: ${e.message}")
//                        // Handle error if JSON is invalid
//                        isSaving = false
//                    }
//                }
//            }

        }) {
            Text("Add Locker")
        }
    }
}
