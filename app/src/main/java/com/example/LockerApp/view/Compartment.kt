package com.example.LockerApp.view

import android.graphics.Bitmap
import android.graphics.Color
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
import androidx.lifecycle.viewmodel.compose.viewModel

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
    var showQRCode by remember { mutableStateOf(true) } // ตั้งค่าเป็น true เพื่อให้แสดง QR Code ทันที

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

            // แสดง QR Code เมื่อ showQRCode เป็น true
            if (showQRCode) {
                Spacer(modifier = Modifier.height(16.dp))
                val qrCodeBitmap = generateQRCode("https://drive.google.com/drive/folders/1D9ako6sSs4peHLsoEzXRsPKa24B22_6F?usp=drive_link")
                Image(bitmap = qrCodeBitmap.asImageBitmap(), contentDescription = "QR Code", modifier = Modifier.size(150.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                onAdd()
                // ปิดการแสดง QR Code ถ้าต้องการให้มันหายไปหลังจากเพิ่มข้อมูล
                showQRCode = false
            }) {
                Text("Add Compartment")
            }
        }
    }
}

fun generateQRCode(content: String): Bitmap {
    val qrCodeWriter = QRCodeWriter()
    val bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, 200, 200)

    val width = bitMatrix.width
    val height = bitMatrix.height
    val pixels = IntArray(width * height)

    for (y in 0 until height) {
        for (x in 0 until width) {
            pixels[y * width + x] = if (bitMatrix.get(x, y)) {
                Color.BLACK
            } else {
                Color.WHITE
            }
        }
    }

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
    bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    return bitmap
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
