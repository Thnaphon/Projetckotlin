package com.example.LockerApp.view


import android.graphics.Bitmap

import android.net.Uri
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts

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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewModelScope

import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.LockerApp.viewmodel.MqttViewModel
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileUpload

import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LiveData
import coil.compose.rememberImagePainter
import com.example.LockerApp.viewmodel.ManageLockerViewModel
import com.example.LockerApp.viewmodel.UsageLockerViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream


@Composable
fun CompartmentUI(lockerId: Int, viewModel: LockerViewModel = viewModel(),accountid: Int,accountname:String) {


    val compartments by viewModel.compartments.collectAsState(emptyList())

    val mqttViewModel: MqttViewModel = viewModel()

    var showAddCard by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var nameItem by remember { mutableStateOf("") }
    var detailItem by remember { mutableStateOf("") }
    val mqttTopic by viewModel.getMqttTopicFromDatabase(lockerId).collectAsState(initial = null)
    var selectedImagePath by remember { mutableStateOf("") }
    val available_compartment by viewModel.getavailableCompartmentByLockerId(lockerId).collectAsState(initial = null)
    val lockername by viewModel.getLockername(lockerId).collectAsState(initial = null)

    val availableCompartments: MutableList<String> = available_compartment?.split(",")?.toMutableList() ?: mutableListOf()

    val usageLockerViewModel: UsageLockerViewModel = viewModel()


    var selectedCompartmentId by remember { mutableStateOf<Int?>(null) }
    val compartmentsNumber by viewModel.compartmentsNumber.observeAsState(emptyList())
    val namelocker by viewModel.getLockername(lockerId).collectAsState(initial = null)
    val mqttData by mqttViewModel.mqttData.collectAsState()
    var Topic = remember { mutableStateOf(" ") }
    LaunchedEffect(mqttData) {

        if (mqttData.first == Topic.value && mqttData.second == "OPEN") {
            val usageTime = System.currentTimeMillis().toString()
            viewModel.getLatestCompartment { CompartmentId ->
                CompartmentId?.let {
                    Log.d("UsageCompartment","${it.CompartmentID} /$accountid")
                    usageLockerViewModel.insertUsageLocker(
                        lockername.toString(),
                        it.CompartmentID,
                        usageTime,
                        "Create Compartment",
                        accountname,
                        "Success",
                        it.Name_Item
                    )
                }
            }
        }

    }


    LaunchedEffect(lockerId) {
        viewModel.loadCompartments(lockerId)
        viewModel.loadCompartmentsByLockerNumber(lockerId)
    }




    LaunchedEffect(compartments) {
        // Log เมื่อข้อมูลของ compartments เปลี่ยน
        Log.d("CompartmentUI", "Compartments updated: ${compartments.size}")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text("Compartments of Locker $namelocker", style = MaterialTheme.typography.h5.copy(fontWeight = FontWeight.Bold), color = Color.Black)
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Total Compartments: ${compartments.size}", style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.SemiBold), color = Color.Black)
            IconButton(onClick = { showAddCard = !showAddCard }) {
                selectedImagePath = ""
                Icon(Icons.Filled.Add, contentDescription = "Add Compartment")
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            content = {
                items(compartments) { compartment ->
                    CompartmentCard(compartment=compartment,viewModel=viewModel,accountid=accountid,accountname=accountname)
                }
                if (showAddCard) {

                    item {
                        AddCompartmentCard(
                            onAdd = {
                                if (nameItem.isNotBlank()) {
                                    val compartment = Compartment(
                                        number_compartment = selectedCompartmentId ?: 0,
                                        usagestatus = "return",
                                        status = "available",
                                        LockerID = lockerId,
                                        Name_Item = nameItem,
                                        pic_item = selectedImagePath
                                    )




                                    // ตรวจสอบ log
                                    Log.d("CompartmentPic", "$selectedCompartmentId")
                                    Log.d("Compartmentadd", "Compartment added: $compartment")

                                    viewModel.addCompartment(compartment,lockerId)
                                    Topic.value = "$mqttTopic/create/$selectedCompartmentId/status"
                                    mqttViewModel.subscribeToTopic("$mqttTopic/borrow/${compartment.number_compartment}/status")
                                    mqttViewModel.subscribeToTopic("$mqttTopic/return/${compartment.number_compartment}/status")
                                    mqttViewModel.sendMessage("$mqttTopic/create/$selectedCompartmentId","")


                                    // Reset input fields
                                    nameItem = ""
                                    detailItem = ""
                                    selectedCompartmentId = null
                                } else {
                                    Log.d("Compartment", "Name or Detail is blank")
                                }
                            },
                            nameItem = nameItem,
                            onNameItemChange = { nameItem = it },
                            selectedImagePath = selectedImagePath,  // ส่ง selectedImagePath ไปด้วย
                            onImageSelected = { selectedImagePath = it },
                            availableCompartments = availableCompartments, // ส่งไปที่นี่
                            onCompartmentSelected = { selectedCompartmentId = it },
                            onCancel = { showAddCard = false }

                        )
                    }
                }
            }
        )
    }
}

@Composable
fun CompartmentCard(compartment: Compartment, viewModel: LockerViewModel = viewModel(),accountid: Int,accountname: String) {
    var selectedCompartmentId by remember { mutableStateOf<Int?>(null) }
    var deleteCompartmentTrigger by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val usageLockerViewModel: UsageLockerViewModel = viewModel()
    val lockername by viewModel.getLockername(compartment.LockerID).collectAsState(initial = null)
    // ใช้ LaunchedEffect ที่อยู่ในระดับของ Composable
    LaunchedEffect(deleteCompartmentTrigger) {
        if (deleteCompartmentTrigger) {
            val usageTime = System.currentTimeMillis().toString()
            usageLockerViewModel.insertUsageLocker(
                lockername.toString(),
                compartment.CompartmentID,
                usageTime,
                "Delete Compartment",
                accountname,
                "Success",
                compartment.Name_Item
            )
            viewModel.delteCompartment(compartment.LockerID, compartment.CompartmentID)

            deleteCompartmentTrigger = false // รีเซ็ต trigger หลังจากทำงานเสร็จ
        }
    }

    Card(
        shape = RoundedCornerShape(15.dp),
        modifier = Modifier
            .padding(8.dp)
            .height(320.dp),
        elevation = 4.dp
    ) {
        Column {
            if (isEditing) {
                EditCompartmentForm(compartment = compartment, onCancel = { isEditing = false },onCompartmentSelected = { selectedCompartmentId = it },accountid=accountid,accountname=accountname)
            } else {
                CompartmentView(compartment = compartment, onEdit = { isEditing = true }, onDelete = { deleteCompartmentTrigger = true })
            }
        }
    }



}

@Composable
fun EditCompartmentForm(compartment: Compartment, onCancel: () -> Unit,viewModel: LockerViewModel = viewModel(),onCompartmentSelected: (Int?) -> Unit,accountid:Int,accountname:String) {
    var selectedCompartmentId by remember { mutableStateOf<Int?>(null) }

    val available_compartment by viewModel.getavailableCompartmentByLockerId(compartment.LockerID).collectAsState(initial = null)
    val usageLockerViewModel: UsageLockerViewModel = viewModel()

    val availableCompartments: MutableList<String> = available_compartment?.split(",")?.toMutableList() ?: mutableListOf()
    // ดึง compartmentIds ที่มีอยู่แล้วจากฐานข้อมูล
    val compartmentsNumber by viewModel.compartmentsNumber.observeAsState(emptyList())
    // ดึง compartmentIds ที่มีอยู่แล้วจากฐานข้อมูล

    // กรอง compartmentId ที่ถูกใช้ไปแล้วออก
    val availableCompartmentIds = availableCompartments.filter {
        val compartmentId = it.toIntOrNull()
        compartmentId != null && compartmentId !in compartmentsNumber
    }
    var editedNameItem by remember { mutableStateOf(compartment.Name_Item) }
    var nameItem by remember { mutableStateOf(compartment.Name_Item) }
    var selectedImagePath by remember { mutableStateOf(compartment.pic_item) }
    var editCompartmentTrigger by remember { mutableStateOf(false) }
    var statusCompartment by remember { mutableStateOf(false) }
    var updatedLockerStatus by remember { mutableStateOf("") }
    val lockername by viewModel.getLockername(compartment.LockerID).collectAsState(initial = null)
    if (compartment.status=="available")  statusCompartment = true else statusCompartment = false
    Card(
        shape = RoundedCornerShape(15.dp),
        modifier = Modifier
            .fillMaxSize(),
        elevation = 4.dp
    ) {
        Column(

            horizontalAlignment = Alignment.Start,

            ) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .background(Color.LightGray)
                    .fillMaxWidth()
                    .height(185.dp)
            ) {
                SelectImage(
                    selectedImagePath = selectedImagePath,
                    onImageSelected = { path -> selectedImagePath = path }
                )

            }
            Column(modifier = Modifier
                .background(Color(0xFF2A3D4F))
                .fillMaxSize()
                .padding(start = 14.dp, end = 8.dp, top = 12.dp),) {
                Row (modifier = Modifier
                    .padding()
                    .wrapContentSize()
                ){
                    Text(
                        text = "Comp",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                    // ระยะห่างระหว่างข้อความ
                    Text(
                        text = "Equipment",
                        color = Color.White,
                        modifier = Modifier.padding(start = 33.dp),
                        fontSize = 12.sp
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 2.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,

                    ) {
                    var expanded by remember { mutableStateOf(false) }

                    Box(modifier = Modifier.height(49.dp)) {

                        Row(
                            modifier = Modifier
                                .clickable { expanded = !expanded }
                                .height(35.dp)
                                .width(50.dp),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = selectedCompartmentId?.toString() ?: "${compartment.number_compartment}",
                                style = MaterialTheme.typography.h6.copy(

                                    color = Color.White,
                                ),
                                modifier = Modifier // ให้ Text ขยายเต็มความกว้าง
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Dropdown Arrow",
                                tint = Color.White
                            )


                        }
                        Row(modifier = Modifier
                            .align(Alignment.BottomStart)){
                            Box(
                                modifier = Modifier
                                    .height(1.dp) // ความหนาของเส้นใต้
                                    .background(Color.White)
                                    .width(50.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            availableCompartmentIds.forEach { compartment ->
                                val compartmentId = compartment.toIntOrNull()
                                compartmentId?.let {
                                    DropdownMenuItem(onClick = {
                                        selectedCompartmentId = it
                                        onCompartmentSelected(it)
                                        expanded = false
                                    }) {
                                        Text(it.toString())
                                    }
                                }
                            }
                        }
                    }


                    // ช่องกรอกชื่ออุปกรณ์
                    UnderlinedTextField(
                        value = nameItem,
                        onValueChange = { nameItem = it },
                        placeholder = compartment.Name_Item, // กรณีที่ต้องการให้ค่าเริ่มต้นเป็น "Equipment"
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .height(56.dp),

                        )
                }

                Row(
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = statusCompartment,
                        onCheckedChange = {
                            statusCompartment = it
                            if (it){
                                updatedLockerStatus="available"
                            } else {
                                updatedLockerStatus="unavailable"
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White, // สีของ thumb เมื่อ switch เปิด
                            uncheckedThumbColor = Color.Gray, // สีของ thumb เมื่อ switch ปิด
                            checkedTrackColor = Color(0xFF34F747), // สีของ track เมื่อ switch เปิด
                            uncheckedTrackColor = Color.LightGray // สีของ track เมื่อ switch ปิด
                        ),
                        modifier = Modifier.scale(1.1f)
                    )
                    Text(
                        text = if (statusCompartment) "Active" else "Inactive",
                        color = if (statusCompartment) Color.White else Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .width(60.dp)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = { onCancel() },
                        modifier = Modifier
                            .scale(0.7f)
                            .background(Color(0xFF2A3D4F)) // ใส่สีพื้นหลังของปุ่ม
                            .padding(4.dp) // เพิ่ม padding ภายในเพื่อล้อมรอบขอบ
                            .border(2.dp, Color.White, RoundedCornerShape(7.dp)) // ขอบด้านใน
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Cancel", tint = Color.White,modifier = Modifier.scale(0.7f))
                    }
                    IconButton(onClick = {
                        val compartmentNumber = selectedCompartmentId ?: compartment.number_compartment
                        if (nameItem.isNotBlank() && selectedImagePath.isNotBlank() ) {
                            editCompartmentTrigger=true
                            Log.d("Compartment", "$updatedLockerStatus")
                            val usageTime = System.currentTimeMillis().toString()
                            viewModel.updateCompartment(compartment.CompartmentID!!,nameItem,selectedImagePath,compartment.LockerID,compartmentNumber,updatedLockerStatus)
                            usageLockerViewModel.insertUsageLocker(
                                lockername.toString(),
                                compartment.CompartmentID,
                                usageTime,
                                "Edit Compartment",
                                accountname,
                                "Success",
                                compartment.Name_Item
                            )
                            onCancel()
                        } else {
                            Log.d(
                                "Compartment",
                                "Name$nameItem, Image$selectedImagePath, or CompartmentID is blank$selectedCompartmentId"
                            )
                        }
                    },modifier = Modifier
                        .scale(0.7f)
                        .clip(RoundedCornerShape(7.dp))
                        .background(Color.White),

                        )
                    {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = "Edit Locker",
                            tint = Color(0xFF2A3D4F)
                            ,modifier = Modifier.scale(0.7f)
                        )
                    }
                }
            }
        }
    }
}




@Composable
fun CompartmentView(compartment: Compartment, onEdit: () -> Unit, onDelete: () -> Unit) {
    val imageFile = File(compartment.pic_item)
    var showDeleteOptions by remember { mutableStateOf(false) }
    val viewModel:LockerViewModel= viewModel()
    val lockerName by viewModel.getLockername(compartment.LockerID).collectAsState(initial = "Loading...")
    val safeLockerName = lockerName ?: "Unknown"
    Box(
        modifier = Modifier
            .fillMaxWidth()


    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize() // ทำให้ Column หลักขยายเมื่อมีการเปลี่ยนแปลง
        ) {
            Box(
                modifier = Modifier
                    .width(280.dp)
                    .height(225.dp),
                contentAlignment = Alignment.Center
            ) {
                if (imageFile.exists()) {
                    Image(
                        painter = rememberImagePainter(imageFile),
                        contentDescription = "Item Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)

            ) {
                Row (
                    Modifier
                        .wrapContentSize()
                        .padding(bottom = 8.dp)){
                    Text(
                        text = "Locker ${safeLockerName.take(11)}${if (safeLockerName.length > 11) "..." else ""} | Comp ${compartment.number_compartment}",
                        fontSize = 13.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = compartment.Name_Item,
                        style = MaterialTheme.typography.h5,
                        maxLines = 1,  // จำกัดจำนวนบรรทัดที่แสดง
                        overflow = TextOverflow.Ellipsis ,
                        modifier = Modifier.width(130.dp)
                    )

                    Row (modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End){
                        Box(
                            modifier = Modifier
                                .size(35.dp) // ขนาดของกล่อง
                                .clickable(onClick = onEdit),
                            contentAlignment = Alignment.Center
                        ){
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Edit",
                                tint = Color(0xFF3961AA),

                                )
                        }
                        Box(
                            modifier = Modifier
                                .size(35.dp) // ขนาดของกล่อง
                                .clickable(onClick = { showDeleteOptions = !showDeleteOptions }),
                            contentAlignment = Alignment.Center
                        ){
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Delete",
                                tint = Color(0xFFEE174A),

                                )
                        }
                    }
                }
            }
        }

        // Box ยืนยันการลบ
        AnimatedVisibility(visible = showDeleteOptions) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFB71C1C))
                    .padding(16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Are you going to delete this Compartment?",
                        color = Color.White,
                        style = MaterialTheme.typography.body1
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            onDelete()
                            showDeleteOptions = false
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color.White),
                        modifier = Modifier.fillMaxWidth().height(40.dp)
                    ) {
                        Text(text = "Confirm", color = Color.Red)
                    }

                    TextButton(onClick = { showDeleteOptions = false }) {
                        Text("Cancel", color = Color.White)
                    }

                }
            }
        }
    }
}






@Composable
fun AddCompartmentCard(
    onAdd: () -> Unit,
    nameItem: String,
    onNameItemChange: (String) -> Unit,
    selectedImagePath: String,
    onImageSelected: (String) -> Unit,
    availableCompartments: MutableList<String>,
    onCompartmentSelected: (Int?) -> Unit,
    viewModel: LockerViewModel = viewModel() ,
    onCancel: () -> Unit
) {
    var selectedCompartmentId by remember { mutableStateOf<Int?>(null) }

    val compartmentsNumber by viewModel.compartmentsNumber.observeAsState(emptyList())
    // ดึง compartmentIds ที่มีอยู่แล้วจากฐานข้อมูล

    // กรอง compartmentId ที่ถูกใช้ไปแล้วออก
    val availableCompartmentIds by remember(availableCompartments, compartmentsNumber) {
        derivedStateOf {
            availableCompartments.filter {
                val compartmentId = it.toIntOrNull()
                compartmentId != null && compartmentId !in compartmentsNumber
            }
        }
    }

    // ข้างใน UI ของคุณ
    Card(
        shape = RoundedCornerShape(15.dp),
        modifier = Modifier
            .padding(8.dp)
            .height(325.dp),
        elevation = 4.dp
    ) {
        Column(

            horizontalAlignment = Alignment.Start,

            ) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .background(Color.LightGray)
                    .fillMaxWidth()
                    .height(185.dp)
            ) {
                SelectImage(
                    selectedImagePath = selectedImagePath,
                    onImageSelected = onImageSelected
                )

            }
            Column(modifier = Modifier
                .background(Color(0xFF2A3D4F))
                .fillMaxSize()
                .padding(start = 14.dp, end = 8.dp, top = 12.dp),) {
                Row (modifier = Modifier
                    .padding()
                    .wrapContentSize()
                ){
                    Text(
                        text = "Comp",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                    // ระยะห่างระหว่างข้อความ
                    Text(
                        text = "Equipment",
                        color = Color.White,
                        modifier = Modifier.padding(start = 33.dp),
                        fontSize = 12.sp
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 2.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,

                    ) {
                    var expanded by remember { mutableStateOf(false) }

                    Box(modifier = Modifier.height(49.dp)) {

                        Row(
                            modifier = Modifier
                                .clickable { expanded = !expanded }
                                .height(35.dp)
                                .width(50.dp),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = selectedCompartmentId?.toString() ?: "  ",
                                style = MaterialTheme.typography.h6.copy(

                                    color = Color.White,
                                ),
                                modifier = Modifier // ให้ Text ขยายเต็มความกว้าง
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Dropdown Arrow",
                                tint = Color.White
                            )


                        }
                        Row(modifier = Modifier
                            .align(Alignment.BottomStart)){
                            Box(
                                modifier = Modifier
                                    .height(1.dp) // ความหนาของเส้นใต้
                                    .background(Color.White)
                                    .width(50.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            availableCompartmentIds.forEach { compartment ->
                                val compartmentId = compartment.toIntOrNull()
                                compartmentId?.let {
                                    DropdownMenuItem(onClick = {
                                        selectedCompartmentId = it
                                        onCompartmentSelected(it)
                                        expanded = false
                                    }) {
                                        Text(it.toString())
                                    }
                                }
                            }
                        }
                    }


                    // ช่องกรอกชื่ออุปกรณ์
                    UnderlinedTextField(
                        value = nameItem,
                        onValueChange = { onNameItemChange(it) },
                        placeholder = "Equipment", // กรณีที่ต้องการให้ค่าเริ่มต้นเป็น "Equipment"
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .height(56.dp),

                        )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {


                    IconButton(
                        onClick = { onCancel() },
                        modifier = Modifier
                            .scale(0.7f)
                            .background(Color(0xFF2A3D4F)) // ใส่สีพื้นหลังของปุ่ม
                            .padding(4.dp) // เพิ่ม padding ภายในเพื่อล้อมรอบขอบ
                            .border(2.dp, Color.White, RoundedCornerShape(7.dp)) // ขอบด้านใน
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Cancel", tint = Color.White,modifier = Modifier.scale(0.7f))
                    }
                    IconButton(onClick = {
                        if (nameItem.isNotBlank() && selectedImagePath.isNotBlank() && selectedCompartmentId != null) {
                            Log.d(
                                "Compartment",
                                "Adding compartment with Name: $nameItem, Image: $selectedImagePath, CompartmentID: $selectedCompartmentId"
                            )
                            onAdd()
                            onCancel()
                        } else {
                            Log.d("Compartment", "Name$nameItem, Image$selectedImagePath, or CompartmentID is blank$selectedCompartmentId")
                        }
                    },modifier = Modifier
                        .scale(0.7f)

                        .clip(RoundedCornerShape(7.dp))
                        .background(Color.White),

                        )
                    {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = "Add Locker",
                            tint = Color(0xFF2A3D4F)
                            ,modifier = Modifier.scale(0.7f)
                        )
                    }
                }
            }
        }
    }
}












@Composable
fun SelectImage(selectedImagePath: String, onImageSelected: (String) -> Unit) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val imagePath = saveImageToInternalStorage(context, it) // คัดลอกไป Internal Storage
            imagePath?.let { path ->
                onImageSelected(path) // ส่งพาธของรูปที่บันทึกกลับไป
            }
        }
    }

    Box(
        modifier = Modifier
            .width(280.dp)
            .height(185.dp)
            .clickable { launcher.launch("image/*") },
        contentAlignment = Alignment.Center

    ) {
        if (selectedImagePath.isNotBlank()) {
            val imageFile = File(selectedImagePath)
            if (imageFile.exists()) {
                Image(
                    painter = rememberImagePainter(imageFile),
                    contentDescription = "Selected Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        } else {
            IconButton(onClick = { launcher.launch("image/*") }) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Camera Icon",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Upload Picture", color = Color.White)
                }
            }
        }
    }
}


fun saveImageToInternalStorage(context: Context, imageUri: Uri): String? {
    return try {
        // อ่านข้อมูลจาก URI ของรูปที่เลือก
        val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
        val fileName = "image_${System.currentTimeMillis()}.jpg" // ตั้งชื่อไฟล์ให้ไม่ซ้ำกัน
        val file = File(context.filesDir, fileName) // บันทึกลง Internal Storage

        inputStream?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output) // คัดลอกไฟล์จาก URI ไปยัง Internal Storage
            }
        }
        Log.d("ImagePath", "Saved image path: ${file.absolutePath}")
        file.absolutePath // คืนค่าพาธของไฟล์ที่บันทึก
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
@Composable
fun UnderlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "Equipment",
    modifier: Modifier = Modifier
) {

    TextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = MaterialTheme.typography.body1.copy(color = Color.White),
        modifier = modifier
            .padding(bottom = 4.dp)
            .fillMaxWidth(), // ทำให้เต็มความกว้าง
        singleLine = true,
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.body2.copy(color = Color.White.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth(), // ทำให้ข้อความเต็มพื้นที่
                textAlign = TextAlign.Start
            )
        },
        colors = TextFieldDefaults.textFieldColors(
            backgroundColor = Color.Transparent,
            focusedIndicatorColor = Color.White,
            unfocusedIndicatorColor = Color.White
        )

    )

}

@Composable
fun DeleteConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Are you sure?") },
        text = { Text(text = "Are you going to delete this Compartment?") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFAA1E40))
            ) {
                Text("Confirm", color = Color.White)
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

