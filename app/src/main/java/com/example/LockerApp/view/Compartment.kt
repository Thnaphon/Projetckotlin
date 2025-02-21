package com.example.LockerApp.view


import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.example.LockerApp.model.Compartment
import com.example.LockerApp.viewmodel.LockerViewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import androidx.compose.foundation.Image

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.lifecycle.viewModelScope

import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.LockerApp.viewmodel.MqttViewModel
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

@Composable
fun CompartmentUI(lockerId: Int, viewModel: LockerViewModel = viewModel()) {
    val compartments by viewModel.getCompartmentsByLocker(lockerId).collectAsState(initial = emptyList())
    val mqttViewModel: MqttViewModel = viewModel()

    var showAddCard by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var nameItem by remember { mutableStateOf("") }
    var detailItem by remember { mutableStateOf("") }
    val mqttTopic by viewModel.getMqttTopicFromDatabase(lockerId).collectAsState(initial = null)

    LaunchedEffect(mqttTopic) {
        mqttTopic?.let { topic ->
            val checkTopic = "$topic/check/compartment"
            mqttViewModel.sendMessage(checkTopic, "check") // ส่งข้อความ "check"
            Log.d("MQTT", "Published to topic: $checkTopic")
            mqttViewModel.subscribeToTopic("$topic/respond/compartment")

        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Compartments of Locker ID: $lockerId", style = MaterialTheme.typography.h5)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Total Compartments: ${compartments.size}", style = MaterialTheme.typography.body1)
            IconButton(onClick = { showAddCard = !showAddCard }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Compartment")
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            content = {
                items(compartments) { compartment ->
                    CompartmentCard(compartment)
                }
                if (showAddCard) {
                    item {
                        AddCompartmentCard(
                            onAdd = {
                                if (nameItem.isNotBlank() && detailItem.isNotBlank() ) {
                                    viewModel.addCompartment(
                                        Compartment(
                                            Status = "return",
                                            LockerID = lockerId,
                                            Name_Item = nameItem,
                                            pic_item = "test",
                                            detail = detailItem
                                        )
                                    )
                                    // Reset input fields

                                    nameItem = ""
                                    detailItem = ""

                                }
                            },

                            nameItem = nameItem,
                            onNameItemChange = { nameItem = it },
                            detailItem = detailItem,
                            onDetailItemChange = { detailItem = it },

                        )
                    }
                }
            }
        )
    }
}



@Composable
fun AddCompartmentCard(
    onAdd: () -> Unit,

    nameItem: String,
    onNameItemChange: (String) -> Unit,
    detailItem: String,
    onDetailItemChange: (String) -> Unit,

) {


    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = 4.dp,
        backgroundColor = MaterialTheme.colors.surface
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {


            TextField(value = nameItem, onValueChange = onNameItemChange, label = { Text("Item Name") })
            Spacer(modifier = Modifier.height(8.dp))
            TextField(value = detailItem, onValueChange = onDetailItemChange, label = { Text("Item Detail") })
            Spacer(modifier = Modifier.height(8.dp))


            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                onAdd()
            }) {
                Text("Add Compartment")
            }
        }
    }
}



@Composable
fun CompartmentCard(compartment: Compartment) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = 4.dp,
        backgroundColor = MaterialTheme.colors.surface
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text("Compartment: ${compartment.CompartmentID}", style = MaterialTheme.typography.h6)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Status: ${compartment.Status}", style = MaterialTheme.typography.body1)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Item Name: ${compartment.Name_Item}", style = MaterialTheme.typography.body2)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

