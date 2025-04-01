package com.Locker.LockerApp.view

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberImagePainter
import com.Locker.LockerApp.model.Compartment
import com.Locker.LockerApp.viewmodel.LockerViewModel
import com.Locker.LockerApp.viewmodel.MqttViewModel
import com.Locker.LockerApp.viewmodel.UsageLockerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ReturnUI(viewModel: LockerViewModel, mqttViewModel: MqttViewModel,usageLockerViewModel: UsageLockerViewModel,accountid: Int,accountname:String) {
    var selectedLocker by remember { mutableStateOf(0) } // เริ่มต้นที่ All Lockers
    val lockers by viewModel.lockers.collectAsState() // ใช้ StateFlow ในการเก็บค่า locker
    val compartments by viewModel.compartments.collectAsState(initial = emptyList())
    var statusMessage by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var isWaitingForClose by remember { mutableStateOf(false) }
    val compartmentNumber by viewModel.getAllCompartmentNumber(selectedLocker).observeAsState(initial = emptyList())
    var nameSelectedLocker by remember {mutableStateOf(" ")}

    LaunchedEffect(Unit) {
        viewModel.loadCompartments(selectedLocker)
        viewModel.loadLockersUi()  // โหลดข้อมูลใหม่เมื่อเลือก Locker
        compartments?.forEach { Number ->
            viewModel.getMqttTopicFromDatabase(Number.LockerID).collect { mqttTopic ->
                mqttTopic?.let {
                    mqttViewModel.subscribeToTopic("$it/borrow/${Number.number_compartment}/status")
                    mqttViewModel.subscribeToTopic("$it/return/${Number.number_compartment}/status")
                }
            }
        }

    }
    LaunchedEffect(selectedLocker) {
        viewModel.loadCompartments(selectedLocker)
    }



    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top

    ) {
        Text("Return", style = MaterialTheme.typography.h5.copy(fontWeight = FontWeight.Bold), color = Color.Black)

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "${compartments.filter { compartment ->
                    compartment.usagestatus == "borrow" &&
                            compartment.status == "available" &&
                            lockers.find { it.LockerID == compartment.LockerID }?.status == "available" && (compartment.Usage_By == accountid.toString() || compartment.Usage_By=="Default")
                }.size} Compartments",
                style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.SemiBold),
                color = Color.Black
            )

            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                Box(
                    modifier = Modifier
                        .wrapContentWidth()
                        .height(48.dp) // ตั้งค่าความสูงให้เหมือนปุ่ม
                        .border(2.dp, Color.Black, RoundedCornerShape(15.dp)) // เพิ่มขอบมน
                        .clickable { expanded = true }
                        .padding(horizontal = 16.dp, vertical = 12.dp), // จัดการ padding
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.wrapContentWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedLocker == 0) "All Lockers" else "Locker $nameSelectedLocker",
                            style = MaterialTheme.typography.body1
                        )
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown, // เปลี่ยนไอคอนเป็นลูกศรลง
                            contentDescription = "Dropdown Icon"
                        )
                    }
                }
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.wrapContentSize()
                ) {
                    lockers.forEach { locker ->
                        DropdownMenuItem(onClick = {
                            selectedLocker = locker.LockerID
                            nameSelectedLocker = locker.Lockername
                            expanded = false
                        }) {
                            Text("Locker ${locker.Lockername}")
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


        LazyVerticalGrid(
            modifier = Modifier.width(1000.dp),
            columns = GridCells.Fixed(4), // กำหนดจำนวนคอลัมน์เป็น 3
            content = {
                items(
                    compartments.filter { compartment ->
                        compartment.usagestatus == "borrow" &&
                                compartment.status == "available" &&
                                lockers.find { it.LockerID == compartment.LockerID }?.status == "available" && (compartment.Usage_By == accountid.toString() || compartment.Usage_By=="Default")
                    }
                ){ compartment ->
                    CompartmentCardReturn(
                        compartment = compartment,
                        mqttViewModel = mqttViewModel,  // ส่ง mqttViewModel
                        viewModel = viewModel,          // ส่ง viewModel
                        usageLockerViewModel = usageLockerViewModel, // ส่ง usageLockerViewModel
                        accountid = accountid,          // ส่ง accountid
                        accountname=accountname,
                        onStatusChange = { status -> isWaitingForClose = status }

                    )
                }
            },

            )

    }
}



@Composable
fun CompartmentCardReturn(
    compartment: Compartment,
    mqttViewModel: MqttViewModel,
    viewModel: LockerViewModel,
    usageLockerViewModel: UsageLockerViewModel,
    accountid: Int,
    accountname:String,
    onStatusChange: (Boolean) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) } // State สำหรับแสดง Dialog
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val mqttData by mqttViewModel.mqttData.collectAsState()

    var Topic = remember { mutableStateOf(" ") }

    val lockerName by viewModel.getLockername(compartment.LockerID).collectAsState(initial = "Loading...")
    val safeLockerName = lockerName ?: "Unknown"
    var IsClick by remember { mutableStateOf(false) }

    LaunchedEffect(mqttData) {
        IsClick = false
        Log.d("mqttData", "MQTT Topic: ${mqttData.first}, Message: ${mqttData.second}")
        if (mqttData.first == Topic.value && mqttData.second == "OPEN") {
            val splitData = mqttData.first.split("/")
            val usageTime = System.currentTimeMillis().toString()
            val topicMap = mapOf(
                "token" to splitData[0],
                "action" to splitData[1],
                "compartmentId" to splitData[2].toInt(),
                "status" to splitData[3]
            )
            val compartmentId = topicMap["compartmentId"] as? Int ?: 0
            val action = topicMap["action"] as? String ?: ""
            val status = topicMap["status"] as? String ?: ""

            val compartment_Id = viewModel.getCompartmentId(compartment.LockerID, compartmentId).first()
            Log.d("compartment_Id","$compartment_Id")

            // Use coroutineScope.launch to run this task in background
            coroutineScope.launch {
                // Update compartment status in background thread

                viewModel.updateCompartmentStatus(
                    compartment_Id,
                    action,
                    compartment.LockerID,
                    "Default"
                )

                // Insert usageLocker data in background thread
                usageLockerViewModel.insertUsageLocker(
                    lockerName.toString(),
                    compartmentId,
                    usageTime,
                    "Return",
                    accountname,
                    "Success",
                    compartment.Name_Item,
                    accountid
                )
            }

        }
    }

    LaunchedEffect(IsClick) {
        while (IsClick == true){
            Log.d("Loop", "กำลังทำงานในลูป...")

            // รอ 1 วินาทีก่อนทำซ้ำ (ป้องกันลูปเร็วเกินไป)
            delay(8000)

            val splitData = Topic.value.split("/")
            val usageTime = System.currentTimeMillis().toString()
            val topicMap = mapOf(
                "token" to splitData[0],
                "action" to splitData[1],
                "compartmentId" to splitData[2].toInt(),
                "status" to splitData[3]
            )
            val compartmentId = topicMap["compartmentId"] as? Int ?: 0
            val action = topicMap["action"] as? String ?: ""


            val compartment_Id = viewModel.getCompartmentId(compartment.LockerID, compartmentId).first()
            usageLockerViewModel.insertUsageLocker(
                lockerName.toString(),
                compartment_Id,
                usageTime,
                action,
                accountname,
                "Fail",
                compartment.Name_Item,
                accountid
            )
            IsClick = false
        }


    }





    Card(
        shape = RoundedCornerShape(15.dp),
        modifier = Modifier
            .padding(8.dp)
            .height(320.dp),
        elevation = 4.dp
    ) {
        Box {

            Column {


                Column(
                    verticalArrangement = Arrangement.Top,
                ) {
                    val imageFile = File(compartment.pic_item)
                    Box(
                        modifier = Modifier
                            .width(280.dp)  // กำหนดขนาดรูปภาพ
                            .height(225.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (imageFile.exists()) {
                            Image(
                                painter = rememberImagePainter(imageFile),
                                contentDescription = "Item Image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop // ปรับให้ภาพเต็มพื้นที่ที่กำหนด
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row (Modifier.wrapContentSize().padding(bottom = 8.dp)){
                            Text(
                                text = "Locker ${safeLockerName.take(11)}${if (safeLockerName.length > 11) "..." else ""} | Comp ${compartment.number_compartment}",
                                fontSize = 13.sp
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,

                            ) {
                            Text(
                                text = "${compartment.Name_Item}",
                                style = MaterialTheme.typography.h5
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            Icon(
                                imageVector = Icons.Outlined.FileUpload,
                                contentDescription = "Upload Icon",
                                modifier = Modifier
                                    .size(45.dp)
                                    .clickable { showDialog = !showDialog }, // กดแล้วเปิด Dialog
                                tint = Color(0xFF3961AA)
                            )
                        }

                    }
                }
            }
            AnimatedVisibility(visible = showDialog) {
                Column(
                    modifier = Modifier
                        // กำหนดขนาดรูปภาพ
                        .height(140.dp)
                    ,
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally

                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .background(
                                Color(0xFF2A3D4F),
                                shape = RoundedCornerShape(15.dp)
                            )



                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(top = 10.dp, start = 5.dp, end = 5.dp, bottom = 5.dp)
                        ) {
                            Text(
                                text = "Do you want to return?",
                                style = MaterialTheme.typography.body1.copy(fontSize = 13.sp),
                                color = Color.White,

                                )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                TextButton(
                                    onClick = {
                                        IsClick = true
                                        coroutineScope.launch {
                                            viewModel.getMqttTopicFromDatabase(compartment.LockerID)
                                                .collect { topicMqtt -> // เปลี่ยนจาก onEach เป็น collect ตรงๆ
                                                    Log.d("return", "$topicMqtt")
                                                    topicMqtt?.let { mqttTopic ->
                                                        Topic.value =
                                                            "$mqttTopic/return/${compartment.number_compartment}/status"
                                                        mqttViewModel.sendMessage(
                                                            "$mqttTopic/return/${compartment.number_compartment}/open",
                                                            " "
                                                        )

                                                    }
                                                }
                                        }


//

                                        showDialog = false
                                    }, shape = RoundedCornerShape(8), // ขอบมน
                                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.White), // สีพื้นหลัง
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Confirm", color = Color.Black)
                                }


                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                            ) {
                                TextButton(onClick = { showDialog = false }) {
                                    Text("Cancel", color = Color.White)
                                }
                            }
                        }

                    }
                }
            }
        }
    }
}



