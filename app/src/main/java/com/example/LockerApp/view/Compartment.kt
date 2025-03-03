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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LiveData
import coil.compose.rememberImagePainter
import com.example.LockerApp.viewmodel.UsageLockerViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream


@Composable
fun CompartmentUI(lockerId: Int, viewModel: LockerViewModel = viewModel(),accountid: Int) {


    val compartments by viewModel.compartments.collectAsState(emptyList())

    val mqttViewModel: MqttViewModel = viewModel()

    var showAddCard by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var nameItem by remember { mutableStateOf("") }
    var detailItem by remember { mutableStateOf("") }
    val mqttTopic by viewModel.getMqttTopicFromDatabase(lockerId).collectAsState(initial = null)
    var selectedImagePath by remember { mutableStateOf("") }
    val available_compartment by viewModel.getavailableCompartmentByLockerId(lockerId).collectAsState(initial = null)


    val availableCompartments: MutableList<String> = available_compartment?.split(",")?.toMutableList() ?: mutableListOf()

    val usageLockerViewModel: UsageLockerViewModel = viewModel()


    var selectedCompartmentId by remember { mutableStateOf<Int?>(null) }
    val compartmentsNumber by viewModel.compartmentsNumber.observeAsState(emptyList())


    LaunchedEffect(lockerId) {
        viewModel.loadCompartments(lockerId)
        viewModel.loadCompartmentsByLockerNumber(lockerId)
    }


    LaunchedEffect(mqttTopic) {
        mqttTopic?.let { topic ->
            val checkTopic = "$topic/check/compartment"
            mqttViewModel.sendMessage(checkTopic, "check") // ส่งข้อความ "check"
            Log.d("MQTT", "Published to topic: $checkTopic")
            mqttViewModel.subscribeToTopic("$topic/respond/compartment")
        }
        mqttViewModel.clearMessage()
    }

    LaunchedEffect(compartments) {
        // Log เมื่อข้อมูลของ compartments เปลี่ยน
        Log.d("CompartmentUI", "Compartments updated: ${compartments.size}")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
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
            columns = GridCells.Fixed(4),
            content = {
                items(compartments) { compartment ->
                    CompartmentCard(compartment)
                }
                if (showAddCard) {
                    item {
                        AddCompartmentCard(
                            onAdd = {
                                if (nameItem.isNotBlank()) {
                                    val compartment = Compartment(
                                        number_compartment = selectedCompartmentId ?: 0,
                                        Status = "return",
                                        LockerID = lockerId,
                                        Name_Item = nameItem,
                                        pic_item = selectedImagePath
                                    )



                                    Log.d("SubTopicCompartmentPic", "$mqttTopic/borrow/${compartment.CompartmentID}/status")
                                    // ตรวจสอบ log
                                    Log.d("CompartmentPic", "$selectedCompartmentId")
                                    Log.d("Compartmentadd", "Compartment added: $compartment")
                                    val usageTime = System.currentTimeMillis().toString()
                                    viewModel.addCompartment(compartment,lockerId)
                                    //logcompartment
//                                    usageLockerViewModel.insertUsageLocker(
//                                        lockerId,
//                                        compartmentIdInt,
//                                        usageTime,
//                                        "Create Compartment",
//                                        accountid,
//                                        "Success"
//                                    )
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
fun CompartmentCard(compartment: Compartment, viewModel: LockerViewModel = viewModel()) {
    var selectedCompartmentId by remember { mutableStateOf<Int?>(null) }
    var deleteCompartmentTrigger by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    // ใช้ LaunchedEffect ที่อยู่ในระดับของ Composable
    LaunchedEffect(deleteCompartmentTrigger) {
        if (deleteCompartmentTrigger) {
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
                EditCompartmentForm(compartment = compartment, onCancel = { isEditing = false },onCompartmentSelected = { selectedCompartmentId = it })
            } else {
                CompartmentView(compartment = compartment, onEdit = { isEditing = true }, onDelete = { deleteCompartmentTrigger = true })
            }
        }
    }



}

@Composable
fun EditCompartmentForm(compartment: Compartment, onCancel: () -> Unit,viewModel: LockerViewModel = viewModel(),onCompartmentSelected: (Int?) -> Unit,) {
    var selectedCompartmentId by remember { mutableStateOf<Int?>(null) }

    val available_compartment by viewModel.getavailableCompartmentByLockerId(compartment.LockerID).collectAsState(initial = null)


    val availableCompartments: MutableList<String> = available_compartment?.split(",")?.toMutableList() ?: mutableListOf()
    // ดึง compartmentIds ที่มีอยู่แล้วจากฐานข้อมูล
    val allCompartmentIds by viewModel.getAllCompartmentIds().observeAsState(emptyList())
    // กรอง compartmentId ที่ถูกใช้ไปแล้วออก
    val availableCompartmentIds = availableCompartments.filter {
        val compartmentId = it.toIntOrNull()
        compartmentId != null && compartmentId !in allCompartmentIds
    }
    var editedNameItem by remember { mutableStateOf(compartment.Name_Item) }
    var nameItem by remember { mutableStateOf("") }
    var selectedImagePath by remember { mutableStateOf(compartment.pic_item) }
    var editCompartmentTrigger by remember { mutableStateOf(false) }


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
            Column(
                modifier = Modifier
                    .background(Color(0xFF2A3D4F))
                    .fillMaxSize(),
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,

                    ) {
                    // Dropdown สำหรับเลือก Compartment
//                    if (availableCompartmentIds.isNotEmpty()) {
                    var expanded by remember { mutableStateOf(false) }

                    Box {
                        Row(
                            modifier = Modifier
                                .clickable { expanded = !expanded }
                                .padding(start = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedCompartmentId?.toString() ?: "  ",
                                style = MaterialTheme.typography.h4.copy(
                                    textDecoration = TextDecoration.Underline,
                                    color = Color.White
                                ),
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Dropdown Arrow",
                                tint = Color.White
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
//                    }

                    // ช่องกรอกชื่ออุปกรณ์
                    UnderlinedTextField(
                        value = nameItem,
                        onValueChange = { nameItem = it },
                        placeholder = compartment.Name_Item, // กรณีที่ต้องการให้ค่าเริ่มต้นเป็น "Equipment"
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onCancel() },
                        modifier = Modifier
                            .border(2.dp, Color.White, RoundedCornerShape(7.dp)) // ขอบสีขาว
                            .wrapContentSize()
                    )
                    {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                    IconButton(onClick = {
                        if (nameItem.isNotBlank() && selectedImagePath.isNotBlank() && selectedCompartmentId != null) {
                            editCompartmentTrigger=true
                            Log.d(
                                "Compartment", "$editCompartmentTrigger")
                            viewModel.updateCompartment(compartment.CompartmentID!!,nameItem,selectedImagePath,compartment.LockerID)
                            onCancel()
                        } else {
                            Log.d(
                                "Compartment",
                                "Name$nameItem, Image$selectedImagePath, or CompartmentID is blank$selectedCompartmentId"
                            )
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Confirm",
                            tint = Color.White
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
                Text(
                    "Locker ${compartment.LockerID} | Compartment ${compartment.number_compartment}",
                    style = MaterialTheme.typography.body2
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = compartment.Name_Item,
                        style = MaterialTheme.typography.h5
                    )
                    Spacer(modifier = Modifier.weight(1f))

                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit",
                            tint = Color(0xFF3961AA)
                        )
                    }

                    IconButton(onClick = { showDeleteOptions = !showDeleteOptions }) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFEE174A)
                        )
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = {
                                onDelete()
                                showDeleteOptions = false
                            },
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color.White)
                        ) {
                            Text(text = "Confirm", color = Color.Red)
                        }

                        Button(
                            onClick = { showDeleteOptions = false },
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color.White)
                        ) {
                            Text(text = "Cancel", color = Color.Black)
                        }
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
    val allCompartmentIds by viewModel.getAllCompartmentIds().observeAsState(emptyList())
    // กรอง compartmentId ที่ถูกใช้ไปแล้วออก
    val availableCompartmentIds = availableCompartments.filter {
        val compartmentId = it.toIntOrNull()
        compartmentId != null && compartmentId !in compartmentsNumber
    }

    // ข้างใน UI ของคุณ
    Card(
        shape = RoundedCornerShape(15.dp),
        modifier = Modifier
            .padding(8.dp)
            .height(320.dp),
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
                .fillMaxSize(),) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,

                    ) {
                    // Dropdown สำหรับเลือก Compartment
//                    if (availableCompartmentIds.isNotEmpty()) {
                    var expanded by remember { mutableStateOf(false) }

                    Box {
                        Row(
                            modifier = Modifier
                                .clickable { expanded = !expanded }
                                .padding(start = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedCompartmentId?.toString() ?: "  ",
                                style = MaterialTheme.typography.h4.copy(
                                    textDecoration = TextDecoration.Underline,
                                    color = Color.White
                                ),
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Dropdown Arrow",
                                tint = Color.White
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
//                    }

                    // ช่องกรอกชื่ออุปกรณ์
                    UnderlinedTextField(
                        value = nameItem,
                        onValueChange = { onNameItemChange(it) },
                        placeholder = "Equipment", // กรณีที่ต้องการให้ค่าเริ่มต้นเป็น "Equipment"
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onCancel() },
                        modifier = Modifier
                            .border(2.dp, Color.White, RoundedCornerShape(7.dp)) // ขอบสีขาว
                            .wrapContentSize()
                    )
                    {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
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
                    }) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Confirm",
                            tint = Color.White
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
    placeholder: String = "Equipment", // กำหนดค่าเริ่มต้นเป็น "Equipment"
    modifier: Modifier = Modifier
){
    TextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = MaterialTheme.typography.body2.copy(color = Color.White),
        modifier = modifier
            .padding(bottom = 4.dp), // เพิ่มระยะห่างจากขอบล่าง
        singleLine = true, // ให้กรอกข้อความได้แค่ 1 บรรทัด
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.body2.copy(color = Color.White.copy(alpha = 0.5f))
            )
        },
        colors = TextFieldDefaults.textFieldColors(
            backgroundColor = Color.Transparent, // ให้พื้นหลังเป็นโปร่งใส
            focusedIndicatorColor = Color.White, // สีเส้นขอบเมื่อกรอก
            unfocusedIndicatorColor = Color.White // สีเส้นขอบเมื่อไม่ได้กรอก
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

