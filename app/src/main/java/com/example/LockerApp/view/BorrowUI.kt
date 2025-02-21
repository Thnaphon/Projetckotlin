package com.example.LockerApp.view

import android.net.http.UrlRequest.Status
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.Card
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.ExposedDropdownMenuDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.example.LockerApp.viewmodel.LockerViewModel
import com.example.LockerApp.viewmodel.MqttViewModel
import com.example.LockerApp.viewmodel.UsageLockerViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun BorrowUI(viewModel: LockerViewModel, usageLockerViewModel: UsageLockerViewModel, mqttViewModel: MqttViewModel, accountid: Int) {
    var selectedLocker by remember { mutableStateOf(0) } // เริ่มต้นที่ All Lockers
    val lockers by viewModel.lockers.collectAsState() // ใช้ StateFlow ในการเก็บค่า locker
    val compartments by viewModel.getCompartmentsByLocker(selectedLocker).collectAsState(initial = emptyList())

    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Borrow", style = MaterialTheme.typography.h4)

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("${compartments.size} Compartments", style = MaterialTheme.typography.body1)

            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                TextField(
                    readOnly = true,
                    value = if (selectedLocker == 0) "All Lockers" else "Locker $selectedLocker",
                    onValueChange = {},
                    label = { Text("Select Locker") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = TextFieldDefaults.textFieldColors(
                        backgroundColor = MaterialTheme.colors.surface,
                        focusedIndicatorColor = MaterialTheme.colors.primary,
                        unfocusedIndicatorColor = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .width(150.dp)
                        .clickable { expanded = true }
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    lockers.forEach { locker ->
                        DropdownMenuItem(onClick = {
                            selectedLocker = locker.LockerID
                            expanded = false
                        }) {
                            Text("Locker ${locker.LockerID}")
                        }
                    }
                    DropdownMenuItem(onClick = {
                        selectedLocker = 0 // เลือก All Lockers
                        expanded = false
                    }) {
                        Text("All Lockers")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3), // กำหนดจำนวนคอลัมน์เป็น 3
            content = {
                items(compartments.filter { it.Status == "return" }) { compartment ->
                    Card(
                        modifier = Modifier
                            .padding(4.dp)
                            .clickable {
                                // ดึง topic MQTT สำหรับ compartment
                                mqttViewModel.cancelWaitingForMessages()

                                // **ล้างค่าที่ได้รับ** ที่เก็บไว้ใน StateFlow
                                mqttViewModel.clearReceivedMessage()
                                viewModel.getMqttTopicForCompartment(compartment.CompartmentID).onEach { topicMqtt ->
                                    mqttViewModel.sendMessage("$topicMqtt/borrow/${compartment.CompartmentID}/open", " ")

                                    mqttViewModel.waitForMessages("$topicMqtt/borrow/${compartment.CompartmentID}/status") { messagestatus ->
                                        viewModel.viewModelScope.launch {
                                            if (messagestatus == "CLOSE") {
                                                val usageTime =
                                                    System.currentTimeMillis().toString()
                                                val usage =
                                                    "Borrow" // ใช้คำว่า "Borrow" หรือสถานะที่เหมาะสม
                                                val Status = "Success"
                                                usageLockerViewModel.insertUsageLocker(
                                                    compartment.LockerID,
                                                    compartment.CompartmentID,
                                                    usageTime,
                                                    usage,
                                                    accountid,
                                                    Status
                                                )
                                                viewModel.updateCompartmentStatus(
                                                    compartment.CompartmentID,
                                                    "borrowed",
                                                    compartment.LockerID
                                                )
                                            }
                                        }
                                    }


                                }.launchIn(viewModel.viewModelScope)
                            },

                        elevation = 4.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text("Locker ${compartment.LockerID} | Compartment ${compartment.CompartmentID}", style = MaterialTheme.typography.body2)
                            Text("${compartment.Name_Item}", style = MaterialTheme.typography.h5)



                        }
                    }
                }
            }
        )
    }
}

