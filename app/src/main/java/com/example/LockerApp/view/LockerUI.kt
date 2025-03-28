package com.example.LockerApp.view


import android.content.Context
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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.TextButton
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.LockerApp.viewmodel.ManageLockerViewModel
import com.example.LockerApp.viewmodel.UsageLockerViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.delay

data class Message(
    val token: String,
    val name: String,
    val availablecompartment: String,
    val date: String,
    val time: String,

    )

@Composable
fun LockerUI(navController: NavController, lockerDao: LockerDao, accountid: Int,accountname:String, compartmentDao: CompartmentDao, onLockerClick: (String) -> Unit) {
    val viewModel: LockerViewModel = viewModel(factory = LockerViewModelFactory(lockerDao, compartmentDao))
    val mqttViewModel: MqttViewModel = viewModel()
    val lockers by viewModel.lockers.collectAsState()
    val lockerCount by viewModel.lockerCount.collectAsState()
    val ManageLockerViewModel :ManageLockerViewModel=viewModel()
    var showAddLockerCard by remember { mutableStateOf(false) } // เก็บสถานะการแสดงการ์ดเพิ่ม locker

    LaunchedEffect(lockers) {
        Log.d("LockerUI", "Lockers count updated: ${lockers.size}")
    }
    LaunchedEffect(lockers) {
        lockers.forEach { locker ->
            val topicToSend = "${locker.TokenTopic}/check"
            Log.d("LockerUI", "Sending message to: $topicToSend")
            mqttViewModel.sendMessage(topicToSend, "")


        }

    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Lockers", style = MaterialTheme.typography.h5.copy(fontWeight = FontWeight.Bold), color = Color.Black)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                "$lockerCount Lockers",
                style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.SemiBold),
                color = Color.Black
            )

            // ปุ่ม "+" เพื่อแสดงการ์ดเพิ่ม Locker
            Card(
                modifier = Modifier
                    .size(47.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .border(2.dp, Color(0xFF3961AA), RoundedCornerShape(15.dp)), // มุมมนของการ์ด
                elevation = 8.dp, // ความสูงของเงา
                backgroundColor = Color.White // พื้นหลังสีขาวของการ์ด
            ) {
                IconButton(
                    onClick = { showAddLockerCard = !showAddLockerCard
                        mqttViewModel.sendMessage("request/locker", "")
                    },
                    modifier = Modifier
                        .fillMaxSize() // ขยายขนาดให้เต็มการ์ด
                        .padding(4.dp) // เพิ่ม padding รอบๆ IconButton

                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "Add Locker",
                        tint = Color(0xFF3961AA)
                    )
                }
            }
        }



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
                    onUpdateStatus = { lockerID, newStatus -> viewModel.updateLockerStatus(lockerID, newStatus) },
                    accountid = accountid,
                    accountname=accountname
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
                        accountid = accountid,
                        accountname=accountname,
                        ManageLockerViewModel = ManageLockerViewModel,
                        showAddLockerCard = { showAddLockerCard = false }
                    )
                }
            }
        }
    }
}


@Composable
fun LockerCard(locker: Locker, onClick: () -> Unit, onUpdateStatus: (Int, String) -> Unit,accountid: Int,accountname:String) {
    val viewModel: LockerViewModel = viewModel()
    var showDeleteOptions by remember { mutableStateOf(false) }
    var deleteCompartmentTrigger by remember { mutableStateOf(false) }
    var showEditOptions by remember { mutableStateOf(false) }
    val ManageLockerViewModel: ManageLockerViewModel = viewModel()

    LaunchedEffect(deleteCompartmentTrigger) {
        if (deleteCompartmentTrigger) {
            val usageTime = System.currentTimeMillis().toString()
            ManageLockerViewModel.insertManageLocker(
                locker_name = locker.Lockername,
                usageTime = usageTime,
                usage = "Delete Locker",
                name_user = accountname,
                Status = "Success"
            )
            viewModel.deleteLocker(locker.LockerID)
            deleteCompartmentTrigger = false // รีเซ็ต trigger หลังจากทำงานเสร็จ

        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .size(width = 230.dp, height = 280.dp)
            .padding(8.dp)
            .clickable { onClick() },
        elevation = 4.dp,
        backgroundColor = MaterialTheme.colors.surface,
        shape = RoundedCornerShape(15.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()

        ) {

            Column(
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                )
                {
                    Text("${locker.Lockername}", style = MaterialTheme.typography.h6)

                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = { showEditOptions=!showEditOptions},
                        modifier = Modifier.size(29.dp) // ลดขนาดปุ่ม
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit",
                            tint = Color(0xFF3961AA),
                            modifier = Modifier.size(29.dp) // ลดขนาดไอคอน
                        )
                    }
                    Spacer(modifier = Modifier.width(5.dp))
                    IconButton(
                        onClick = { showDeleteOptions = !showDeleteOptions},
                        modifier = Modifier.size(29.dp) // ลดขนาดปุ่ม
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFEE174A),
                            modifier = Modifier.size(29.dp) // ลดขนาดไอคอน
                        )
                    }

                }
                Row(
                    verticalAlignment = Alignment.Top, modifier = Modifier
                        .height(25.dp)
                        .fillMaxWidth() // กำหนดให้ Row ขยายเต็มความกว้าง
                ) {
                    Text("${locker.detail}", style = MaterialTheme.typography.body1)
                }

                Row(
                    modifier = Modifier
                        .border(1.dp, color = Color(0xFFEAEAEA), RoundedCornerShape(10.dp))
                        .weight(1f) // ขยายเต็มพื้นที่ที่เหลือใน Column
                        .fillMaxWidth()
                ) {

                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth() // กำหนดให้ Row ขยายเต็มความกว้าง
                        .height(45.dp)
                        .border(1.dp, color = Color(0xFFEAEAEA), RoundedCornerShape(10.dp))
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                )
                {
                    val compartments =
                        locker.availableCompartment.split(",").filter { it.isNotEmpty() }
                    val Countcompartments = compartments.size
                    val UsedCompartment by viewModel.getAllCompartmentNumber(locker.LockerID)
                        .observeAsState(initial = emptyList())
                    val CountUsedCompartment = UsedCompartment.size
                    Text("Compartments to used    ", style = MaterialTheme.typography.body2)
                    Text(
                        "$CountUsedCompartment/$Countcompartments",
                        style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.SemiBold)
                    )

                }
            }

            DeleteConfirmation(
                showDeleteOptions = showDeleteOptions,
                onConfirmDelete = {
                    deleteCompartmentTrigger = true
                    showDeleteOptions = false
                },
                onCancelDelete = {
                    showDeleteOptions = false
                }
            )
            Editlocker(
                showEditOptions = showEditOptions,
                locker = locker,
                accountid=accountid,
                accountname=accountname,
                onConfirmEdit = {
                    // Handle Locker Edit Confirmation logic
                    showEditOptions = false
                },
                onCancelEdit = {
                    showEditOptions = false
                }
                ,
            )
        }
    }
}

@Composable
fun DeleteConfirmation(
    showDeleteOptions: Boolean,
    onConfirmDelete: () -> Unit,
    onCancelDelete: () -> Unit
) {
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
                    text = "Are you going to delete this Locker?",
                    color = Color.White,
                    style = MaterialTheme.typography.body1
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        onConfirmDelete()
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.White),
                    modifier = Modifier.fillMaxWidth().height(40.dp)
                ) {
                    Text(text = "Confirm", color = Color.Red)
                }

                TextButton(onClick = {onCancelDelete()  }) {
                    Text("Cancel", color = Color.White)
                }



            }
        }
    }
}



@Composable
fun AddLockerCard(
    mqttViewModel: MqttViewModel,
    viewModel: LockerViewModel,
    navController: NavController,
    lockerDao: LockerDao,
    accountid: Int,
    accountname:String,
    ManageLockerViewModel: ManageLockerViewModel,
    showAddLockerCard: () -> Unit
) {
    var lockerDetail by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var TokenTopic by remember { mutableStateOf("") }
    var availableCompartment by remember { mutableStateOf("") }
    var Lockername by remember { mutableStateOf("") }
    var receivedTopic by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    LaunchedEffect(key1 = true) {
        mqttViewModel.observeMqttData() // เรียกฟังก์ชันนี้เพื่อติดตามข้อมูล
    }

    val mqttData by mqttViewModel.mqttData.collectAsState()



    suspend fun isTopicExist(topic: String): Boolean {
        val locker = lockerDao.getLockerByTopic(topic)
        return locker != null
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .size(width = 230.dp, height = 280.dp)
            .padding(8.dp)
            .clickable { /* Add Locker */ },
        elevation = 4.dp,
        backgroundColor = androidx.compose.ui.graphics.Color(0xFF2A3D4F),
        shape = RoundedCornerShape(15.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text("Create Locker", style = MaterialTheme.typography.h6, color = Color.White)


            Spacer(modifier = Modifier.height(5.dp))
            TextField(
                value = Lockername,
                onValueChange = { Lockername = it },
                label = { Text("Locker Name", color = Color.White,) },
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = Color.Transparent,
                    textColor = Color.White,// กำหนดให้พื้นหลังเป็นโปร่งใส
                    focusedIndicatorColor = Color.White, // กำหนดเส้นใต้เมื่อโฟกัสเป็นสีขาว
                    unfocusedIndicatorColor = Color.White,
                ),
                textStyle = TextStyle(
                    fontSize = 25.sp, // ปรับขนาดตัวอักษรขณะพิมพ์
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(14.dp))
            TextField(
                value = lockerDetail,
                onValueChange = { lockerDetail = it },
                label = { Text("Locker Detail", color = Color.White) },
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = Color.Transparent,
                    textColor = Color.White,// กำหนดให้พื้นหลังเป็นโปร่งใส
                    focusedIndicatorColor = Color.White, // กำหนดเส้นใต้เมื่อโฟกัสเป็นสีขาว
                    unfocusedIndicatorColor = Color.White,
                ),
                textStyle = TextStyle(
                    fontSize = 25.sp, // ปรับขนาดตัวอักษรขณะพิมพ์
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(22.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 6.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically

            ) {
                IconButton(
                    onClick = {
                        // รีเซ็ตค่าที่กรอกไป
                        Lockername = ""
                        lockerDetail = ""

                        // ซ่อนการ์ดสำหรับเพิ่ม Locker
                        showAddLockerCard()
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFF2A3D4F)) // ใส่สีพื้นหลังของปุ่ม
                        .padding(4.dp) // เพิ่ม padding ภายในเพื่อล้อมรอบขอบ
                        .border(2.dp, Color.White, RoundedCornerShape(7.dp)) // ขอบด้านใน
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Cancel", tint = Color.White)
                }

                Spacer(modifier = Modifier.width(25.dp))

                IconButton(
                    onClick = {

                        isSaving = true
                        showAddLockerCard()

                        Log.d(
                            "mqttDats",
                            "MQTT Topic: ${mqttData.first}, Message: ${mqttData.second}"
                        )

                        if (mqttData.first == "respond/locker") {

                            try {
                                val jsonObject = JSONObject(mqttData.second)
                                TokenTopic = jsonObject.getString("Token")
                                Log.d("TokenTopic", "TokenTopic $TokenTopic")
                                availableCompartment = jsonObject.getString("Compartment")
                                Log.d(
                                    "availableCompartment",
                                    "availableCompartment $availableCompartment"
                                )



                                viewModel.viewModelScope.launch {
                                    val exists = isTopicExist(TokenTopic)
                                    if (exists) {
                                        isSaving = false
                                        Log.e("LockerUI", "Topic $TokenTopic already exists.")
                                    } else {
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
                                        ){

                                            viewModel.getLatestLocker { LokcerId ->
                                                LokcerId?.let {
                                                    Log.d("Usage","${it.LockerID} /$accountid")
                                                    ManageLockerViewModel.insertManageLocker(
                                                        locker_name = it.Lockername,
                                                        usageTime = usageTime,
                                                        usage = "Create Locker",
                                                        name_user = accountname,
                                                        Status = "Success"
                                                    )
                                                }
                                            }

                                        }
                                        availableCompartment.forEach{ compartment ->
                                            if (compartment != ',') {
                                                mqttViewModel.subscribeToTopic("$TokenTopic/borrow/$compartment/status")
                                                mqttViewModel.subscribeToTopic("$TokenTopic/return/$compartment/status")
                                            }
                                        }


                                        Log.d("Create", "TokenTopic $TokenTopic")



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
                                }

                            } catch (e: Exception) {
                                Log.e("LockerUI", "Error parse JSON: ${e.message}")
                                isSaving = false
                            }
                        }
//
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(Color.White),
                )
                {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = "Add Locker",
                        tint = Color(0xFF2A3D4F)
                    )
                }
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




@Composable
fun Editlocker(
    showEditOptions: Boolean,
    onConfirmEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    locker: Locker,
    accountid:Int,
    accountname:String

) {
    val ManageLockerViewModel: ManageLockerViewModel = viewModel()
    var updatedLocker by remember { mutableStateOf(locker.copy()) }
    val viewModel: LockerViewModel = viewModel()
    var isConfirmed by remember { mutableStateOf(false) }

    AnimatedVisibility(visible = showEditOptions) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { /* Add Locker */ },
            elevation = 4.dp,
            backgroundColor = androidx.compose.ui.graphics.Color(0xFF2A3D4F),
            shape = RoundedCornerShape(15.dp)
        ) {
            Column(
                modifier = Modifier
                    .wrapContentHeight()
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                TextField(
                    value = updatedLocker.Lockername,
                    onValueChange = { updatedLocker = updatedLocker.copy(Lockername = it) },
                    label = { Text("Locker Name", color = Color.White) },
                    colors = TextFieldDefaults.textFieldColors(
                        backgroundColor = Color.Transparent,
                        textColor = Color.White,// กำหนดให้พื้นหลังเป็นโปร่งใส
                        focusedIndicatorColor = Color.White, // กำหนดเส้นใต้เมื่อโฟกัสเป็นสีขาว
                        unfocusedIndicatorColor = Color.White,
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                TextField(
                    value = updatedLocker.detail,
                    onValueChange = { updatedLocker = updatedLocker.copy(detail = it) },
                    label = { Text("Locker Detail", color = Color.White) },
                    colors = TextFieldDefaults.textFieldColors(
                        backgroundColor = Color.Transparent,
                        textColor = Color.White,// กำหนดให้พื้นหลังเป็นโปร่งใส
                        focusedIndicatorColor = Color.White, // กำหนดเส้นใต้เมื่อโฟกัสเป็นสีขาว
                        unfocusedIndicatorColor = Color.White,
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(

                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .wrapContentHeight()
                        .fillMaxWidth()

                ) {
                    Switch(
                        checked = isConfirmed,
                        onCheckedChange = {
                            isConfirmed = it
                            updatedLocker = updatedLocker.copy(status = if (it) "available" else "unavailable")
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White, // สีของ thumb เมื่อ switch เปิด
                            uncheckedThumbColor = Color.Gray, // สีของ thumb เมื่อ switch ปิด
                            checkedTrackColor = Color(0xFF34F747), // สีของ track เมื่อ switch เปิด
                            uncheckedTrackColor = Color.LightGray // สีของ track เมื่อ switch ปิด
                        ),
                        modifier = Modifier.scale(1.5f)// ปรับขนาดของ Switch
                    )
                    Text(
                        text = if (isConfirmed) "Active" else "Inactive",
                        color = if (isConfirmed) Color.White else Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                    Spacer(modifier = Modifier.weight(1f))

                    IconButton(
                        onClick = { onCancelEdit() },
                        modifier = Modifier
                            .scale(0.8f)
                            .background(Color(0xFF2A3D4F)) // ใส่สีพื้นหลังของปุ่ม
                            .padding(4.dp) // เพิ่ม padding ภายในเพื่อล้อมรอบขอบ
                            .border(2.dp, Color.White, RoundedCornerShape(7.dp)) // ขอบด้านใน
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Cancel", tint = Color.White,modifier = Modifier.scale(0.8f))
                    }

                    IconButton(
                        onClick = {
                            viewModel.updateLocker(updatedLocker.LockerID,updatedLocker.status,updatedLocker.Lockername,updatedLocker.detail)
                            val usageTime = System.currentTimeMillis().toString()
                            ManageLockerViewModel.insertManageLocker(
                                locker_name = updatedLocker.Lockername,
                                usageTime = usageTime,
                                usage = "Edit Locker",
                                name_user = accountname,
                                Status = "Success"
                            )
                            Log.d("EditLocker","Test")
                            onCancelEdit()}
                        ,
                        modifier = Modifier
                            .scale(0.8f)

                            .clip(RoundedCornerShape(7.dp))
                            .background(Color.White),

                        )
                    {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = "Add Locker",
                            tint = Color(0xFF2A3D4F)
                            ,modifier = Modifier.scale(0.8f)
                        )
                    }
                }


            }
        }
    }
}

