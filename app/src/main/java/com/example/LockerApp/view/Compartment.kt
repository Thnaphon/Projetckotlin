package com.example.LockerApp.view


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
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import com.example.LockerApp.model.Compartment


@Composable
fun CompartmentUI(lockerId: Int, viewModel: LockerViewModel = viewModel()) {
    val compartments by viewModel.getCompartmentsByLocker(lockerId).collectAsState(initial = emptyList())

    var showAddCard by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var nameItem by remember { mutableStateOf("") }
    var detailItem by remember { mutableStateOf("") }
    var picItem by remember { mutableStateOf("") }

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
                                if (status.isNotBlank() && nameItem.isNotBlank() && detailItem.isNotBlank() && picItem.isNotBlank()) {
                                    viewModel.addCompartment(
                                        Compartment(
                                            Status = status,
                                            LockerID = lockerId,
                                            Name_Item = nameItem,
                                            detail_item = detailItem,
                                            pic_item = picItem
                                        )
                                    )
                                    // Reset input fields
                                    status = ""
                                    nameItem = ""
                                    detailItem = ""
                                    picItem = ""
                                }
                            },
                            status = status,
                            onStatusChange = { status = it },
                            nameItem = nameItem,
                            onNameItemChange = { nameItem = it },
                            detailItem = detailItem,
                            onDetailItemChange = { detailItem = it },
                            picItem = picItem,
                            onPicItemChange = { picItem = it }
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
    status: String,
    onStatusChange: (String) -> Unit,
    nameItem: String,
    onNameItemChange: (String) -> Unit,
    detailItem: String,
    onDetailItemChange: (String) -> Unit,
    picItem: String,
    onPicItemChange: (String) -> Unit
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
            TextField(value = status, onValueChange = onStatusChange, label = { Text("Status") })
            Spacer(modifier = Modifier.height(8.dp))
            TextField(value = nameItem, onValueChange = onNameItemChange, label = { Text("Item Name") })
            Spacer(modifier = Modifier.height(8.dp))
            TextField(value = detailItem, onValueChange = onDetailItemChange, label = { Text("Item Detail") })
            Spacer(modifier = Modifier.height(8.dp))
            TextField(value = picItem, onValueChange = onPicItemChange, label = { Text("Item Picture URL") })
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onAdd) {
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
            Text("Item Detail: ${compartment.detail_item}", style = MaterialTheme.typography.body2)
        }
    }
}