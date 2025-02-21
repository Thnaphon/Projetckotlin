package com.example.LockerApp.view


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
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.LockerApp.model.CompartmentDao

import com.example.LockerApp.model.Locker
import com.example.LockerApp.model.LockerDao
import com.example.LockerApp.viewmodel.LockerViewModelFactory
import com.example.LockerApp.viewmodel.MqttViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import androidx.compose.runtime.LaunchedEffect
import android.util.Log

@Composable
fun LockerUI(navController: NavController, lockerDao: LockerDao, accountid: Int, compartmentDao: CompartmentDao, onLockerClick: (String) -> Unit) {
    val viewModel: LockerViewModel = viewModel(factory = LockerViewModelFactory(lockerDao, compartmentDao))
    val mqttViewModel: MqttViewModel = viewModel()
    val lockers by viewModel.lockers.collectAsState()
    val lockerCount by viewModel.lockerCount.collectAsState()

    var showAddLockerCard by remember { mutableStateOf(false) } // เก็บสถานะการแสดงการ์ดเพิ่ม locker

    LaunchedEffect(lockers) {
        Log.d("LockerUI", "Lockers count updated: ${lockers.size}")
    }
    LaunchedEffect(lockers) {
        lockers.forEach { locker ->
            val topicToSend = "${locker.TokenTopic}/check"
            Log.d("LockerUI", "Sending message to: $topicToSend")
            mqttViewModel.sendMessage(topicToSend, "")

            try {
                // รอรับข้อความในเวลาที่กำหนด (20 วินาที)
                withTimeout(20000) {
                    mqttViewModel.waitForMessages("${locker.TokenTopic}/check/respond") { message ->
                        Log.d("LockerUI", "Received message: $message for locker ${locker.LockerID}")
                        viewModel.viewModelScope.launch {
                            if (message == "ACK") {
                                Log.d("LockerUI", "Locker ${locker.LockerID} is available")
                                viewModel.updateLockerStatus(locker.LockerID, "Available")
                            }
                        }
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.d("LockerUI", "Timeout reached for locker ${locker.LockerID}. Setting status to Unavailable")
                viewModel.updateLockerStatus(locker.LockerID, "Unavailable")
            }
        }
    }


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

            // ปุ่ม "+" เพื่อแสดงการ์ดเพิ่ม Locker
            IconButton(onClick = { showAddLockerCard = !showAddLockerCard }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Locker")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // กรองเฉพาะ locker ที่สถานะไม่เป็น "Unavailable"
            val availableLockers = lockers.filter { it.status != "Unavailable" }

            items(availableLockers.size, key = { index -> availableLockers[index].LockerID }) { index ->
                val locker = availableLockers[index]
                LockerCard(
                    locker = locker,
                    onClick = { onLockerClick(locker.LockerID.toString()) },
                    onUpdateStatus = { lockerID, newStatus -> viewModel.updateLockerStatus(lockerID, newStatus) }
                )
            }

            // แสดงการ์ดเพิ่ม locker หาก showAddLockerCard เป็น true
            if (showAddLockerCard) {
                item {
                    AddLockerCard(
                        mqttViewModel = mqttViewModel,
                        viewModel = viewModel,
                        navController = navController,
                        lockerDao = lockerDao,
                        accountid = accountid
                    )
                }
            }
        }
    }
}


@Composable
fun LockerCard(locker: Locker, onClick: () -> Unit, onUpdateStatus: (Int, String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() },
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
            Spacer(modifier = Modifier.height(8.dp))


        }
    }
}




@Composable
fun AddLockerCard(
    mqttViewModel: MqttViewModel,
    viewModel: LockerViewModel,
    navController: NavController,
    lockerDao: LockerDao,
    accountid: Int
) {
    var lockerDetail by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var TokenTopic by remember { mutableStateOf("") }
    var availableCompartment by remember { mutableStateOf("") }
    var Lockername by remember { mutableStateOf("") }

    suspend fun isTopicExist(topic: String): Boolean {
        val locker = lockerDao.getLockerByTopic(topic)
        return locker != null
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { /* Add Locker */ },
        elevation = 4.dp,
        backgroundColor = MaterialTheme.colors.surface
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
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
                mqttViewModel.sendMessage("request/locker", "")
                mqttViewModel.waitForMessages("respond/locker") { message ->
                    if (message.isNotEmpty()) {
                        try {
                            val jsonObject = JSONObject(message)
                            TokenTopic = jsonObject.getString("Token")
                            availableCompartment = jsonObject.getString("Compartment")

                            viewModel.viewModelScope.launch {
                                val exists = isTopicExist(TokenTopic)
//                                if (exists) {
//                                    isSaving = false
//                                    Log.e("LockerUI", "Topic $TokenTopic already exists.")
//                                } else {
                                    val usageTime = System.currentTimeMillis().toString()
                                    val formatusageTime = formatTimestamp(usageTime)
                                    val splitDateTime = formatusageTime.split(" ")
                                    val timePart = splitDateTime[0]
                                    val datePart = splitDateTime[1]

                                    viewModel.addLocker(
                                        Lockername,
                                        lockerDetail,
                                        TokenTopic,
                                        availableCompartment
                                    )
                                    val message = Message(
                                        token = TokenTopic,
                                        name = Lockername,
                                        availablecompartment = availableCompartment,
                                        date = datePart,
                                        time = timePart
                                    )

                                    mqttViewModel.sendMessageJson("lockers", message)
                                    isSaving = false
                                }
//                            }
                        } catch (e: Exception) {
                            Log.e("LockerUI", "Error parsing JSON: ${e.message}")
                            isSaving = false
                        }
                    }
                }
            }) {
                Text("Add Locker")
            }
        }


    // ฟังก์ชันแปลง timestamp เป็นรูปแบบ hh:mm dd/MM/yyyy
    fun formatTimestamp(timestamp: String): String {
        return try {
            val date = Date(timestamp.toLong())
            val sdf = SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault())
            sdf.format(date)
        } catch (e: Exception) {
            "Invalid date"
        }
    }
}


    // ฟังก์ชันแปลง timestamp เป็นรูปแบบ hh:mm dd/MM/yyyy
    fun formatTimestamp(timestamp: String): String {
        return try {
            val date = Date(timestamp.toLong())
            val sdf = SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault())
            sdf.format(date)
        } catch (e: Exception) {
            "Invalid date"
        }
    }
}
data class Message(
    val token: String,
    val name: String,
    val availablecompartment: String,
    val date: String,
    val time: String,

)
