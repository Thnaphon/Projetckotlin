package com.example.LockerApp.view


import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RestorePage
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.LockerApp.model.BackupLog
import com.example.LockerApp.model.BackupSettings
import com.example.LockerApp.model.LockerDatabase
import com.example.LockerApp.service.MqttService
import com.example.LockerApp.viewmodel.AccountViewModel
import com.example.LockerApp.viewmodel.BackupViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupScreen(viewModel: BackupViewModel,accountname: String) {



    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text("Backup", style = MaterialTheme.typography.h5.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(10.dp))

        // Scheduled Backup Section
        BackupSectionTitle(title = "Scheduled", showButton = true,viewModel=viewModel,accountname=accountname)
        BackupSection(title = "Scheduled",viewModel=viewModel)
        Spacer(modifier = Modifier.height(20.dp))

        // Recent Backup Section
        BackupSectionTitle(title = "Recent", showButton = false ,viewModel=viewModel,accountname=accountname)
        RecentBackup(title = "Recent",viewModel=viewModel)
    }
}

@Composable
fun BackupSectionTitle(viewModel: BackupViewModel,title: String, showButton:Boolean,accountname: String) {
    val settings by viewModel.backupSettings.collectAsState()
    val context = LocalContext.current
    val accountViewModel: AccountViewModel = viewModel()

    LaunchedEffect(Unit) {
        // ทำสิ่งที่ต้องการเมื่อหน้าจอกลับมาจากหน้าก่อนหน้า เช่นการรีเฟรชข้อมูล
        viewModel.loadBackupData(context) // ตัวอย่างเช่นเรียกฟังก์ชันใน ViewModel ที่ทำการโหลดข้อมูลสำรองใหม่
    }

    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 15.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Backup,
                    contentDescription = "$title Icon",
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
        if (showButton) {
            Column {
                Row() {
                    Column {
                        Card(
                            modifier = Modifier
                                .width(56.dp)
                                .height(56.dp)
                                .clip(RoundedCornerShape(15.dp))
                                .border(
                                    2.dp,
                                    Color(0xFF3961AA),
                                    RoundedCornerShape(15.dp)
                                ), // มุมมนของการ์ด
                            elevation = 8.dp, // ความสูงของเงา
                            backgroundColor = Color.White // พื้นหลังสีขาวของการ์ด
                        ) {
                            IconButton(
                                onClick = {

                                    viewModel.performBackup(context,accountname,settings?.description ?: "Full Backup")


                                },
                                modifier = Modifier
                                    .fillMaxSize() // ขยายขนาดให้เต็มการ์ด
                                    .padding(4.dp) // เพิ่ม padding รอบๆ IconButton
                                ,


                                ) {
                                Icon(
                                    Icons.Filled.Backup,
                                    contentDescription = "Backup Icon",
                                    tint = Color(0xFF3961AA)
                                )
                            }
                        }



                    }
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Card(
                            modifier = Modifier
                                .width(56.dp)
                                .height(56.dp)
                                .clip(RoundedCornerShape(15.dp))
                                .border(
                                    2.dp,
                                    Color(0xFF3961AA),
                                    RoundedCornerShape(15.dp)
                                ), // มุมมนของการ์ด
                            elevation = 8.dp, // ความสูงของเงา
                            backgroundColor = Color.White // พื้นหลังสีขาวของการ์ด
                        ) {
                            IconButton(
                                onClick = {

                                    viewModel.performRestore(context,accountname,settings?.description ?: "Full Backup")


                                },
                                modifier = Modifier
                                    .fillMaxSize() // ขยายขนาดให้เต็มการ์ด
                                    .padding(4.dp) // เพิ่ม padding รอบๆ IconButton
                                ,


                                ) {
                                Icon(
                                    Icons.Filled.RestorePage,
                                    contentDescription = "Restore Icon",
                                    tint = Color(0xFF3961AA)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BackupSection(title: String,viewModel:BackupViewModel) {
    Card(
        elevation = 8.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .background(Color(0xFFEEEEEE))
                    .padding(vertical = 10.dp)
                    .padding(horizontal = 15.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .width(150.dp)
                ) {
                    Text("Operation", color = Color.Black)
                }
                Column(
                    modifier = Modifier
                        .width(150.dp)
                ) {
                    Text("Time", color = Color.Black)
                }
                Column(
                    modifier = Modifier
                        .width(150.dp)
                ) {
                    Text("Scheduled", color = Color.Black)
                }
                Column(
                    modifier = Modifier
                        .width(200.dp)
                ) {
                    Text("Description", color = Color.Black)
                }
                Column(modifier = Modifier
                    .width(150.dp)) {

                }

            }
            Row() {
                ScheduledBackupCard(viewModel=viewModel)
            }
        }
    }
}

@Composable
fun ScheduledBackupCard(viewModel: BackupViewModel) {
    var isEditing by remember { mutableStateOf(false) }
    val settings by viewModel.backupSettings.collectAsState()

    var backupTime by remember { mutableStateOf(settings?.backupTime ?: "None") }
    var frequency by remember { mutableStateOf(settings?.frequency ?: "None") }

    var backupText by remember { mutableStateOf(settings?.description ?:"Full Backup") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = if (isEditing) Color(0xFF2A3D4F) else Color.White // สีพื้นหลังเฉพาะตอนแก้ไข
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .padding(15.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier
                    .width(150.dp)
                    ) {

                    Text("Backup", color = if (isEditing) Color.White else Color.Black, fontWeight = FontWeight.Bold)
                }
                Column(modifier = Modifier
                    .width(150.dp)) {
                    if (isEditing) {
                        TimeDropdownMenu(
                            selectedTime = backupTime,
                            onTimeSelected = { backupTime = it }
                        )

                    } else {
                        Text(backupTime, color = Color.Black, fontWeight = FontWeight.Bold) // ปกติเป็นสีดำ
                    }
                }
                Column(modifier = Modifier
                    .width(150.dp)
                    ) {
                    if (isEditing) {
                        SecheduledDropdownMenu(
                            selectedFrequency = frequency,
                            onTimeSelected = { frequency = it }
                        )
                    } else {
                        Text(frequency, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
                Column(modifier = Modifier
                    .width(200.dp)
                    ) {
                    if (isEditing) {
                        // แสดง TextField สำหรับแก้ไข "Full Backup"
                        TextField(
                            value = backupText,  // หรือใส่ค่าที่ต้องการให้แก้ไข
                            onValueChange = { backupText = it } ,
                            colors = TextFieldDefaults.textFieldColors(
                                backgroundColor = Color.Transparent,
                                focusedIndicatorColor = Color.White,  // สีเส้นขอบเมื่อมีการโฟกัส
                                unfocusedIndicatorColor = Color.White // สีเส้นขอบเมื่อไม่ได้โฟกัส
                            ),
                            textStyle = TextStyle(color = Color.White)

                        )
                    } else {
                        // แสดงข้อความปกติ "Full Backup"
                        Text(backupText, color = if (isEditing) Color.White else Color.Black)
                    }
                }
                Column(modifier = Modifier
                    .width(120.dp)
                    ) {
                    if (isEditing) {
                        Row (
                            verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.SpaceBetween,modifier = Modifier.fillMaxWidth()){

                            IconButton(
                                onClick = {
                                    viewModel.updateBackupSettings(frequency, backupTime, backupText)
                                    isEditing = false
                                },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(Color.White),
                            ) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = "",
                                    tint = Color(0xFF2A3D4F)
                                )
                            }

                            IconButton(
                                onClick = {
                                    isEditing = false
                                },
                                modifier = Modifier
                                    .background(Color(0xFF2A3D4F)) // ใส่สีพื้นหลังของปุ่ม
                                    .padding(4.dp) // เพิ่ม padding ภายในเพื่อล้อมรอบขอบ
                                    .border(2.dp, Color.White, RoundedCornerShape(7.dp))

                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Cancel", tint = Color.White)
                            }
                        }

                    } else {
                        Box(
                            contentAlignment = Alignment.CenterEnd,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(
                                onClick = { isEditing = true }
                            ) {
                                Icon(
                                    Icons.Filled.Edit,
                                    contentDescription = "Edit",
                                    tint = Color.Black
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}




@Composable
fun RecentBackup(title: String,viewModel: BackupViewModel) {
    Card(
        elevation = 8.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .background(Color(0xFFEEEEEE))
                    .padding(vertical = 10.dp)
                    .padding(horizontal = 15.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .width(150.dp)
                ) {
                    Text("Operation", color = Color.Black)

                }
                Column(
                    modifier = Modifier
                        .width(150.dp)
                ) {
                    Text("Date/Time", color = Color.Black)
                }
                Column(
                    modifier = Modifier
                        .width(150.dp)
                ) {
                    Text("Status", color = Color.Black)
                }
                Column(
                    modifier = Modifier
                        .width(350.dp)
                ) {
                    Text("Description", color = Color.Black)
                }

            }
            Row{
                RecentBackupCard(viewModel)
            }
        }
    }
}

@Composable
fun RecentBackupCard(viewModel: BackupViewModel) {
    val backupLogs by viewModel.allBackupLogs.collectAsState(initial = emptyList())

    Card(
        modifier = Modifier.fillMaxWidth(),

    ) {
        LazyColumn {
            items(backupLogs) { backupLogs ->
                val splitDateTime = formatTimestamp(backupLogs.date_time).split(" ")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(65.dp)
                        .background(Color.White)
                        .padding(15.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .width(150.dp)
                    ) {
                        Text(backupLogs.operation, color = Color.Black, fontWeight = FontWeight.Bold)

                    }
                    Column(
                        modifier = Modifier
                            .width(150.dp)
                    ) {
                        Row() {
                            Text(formatDateHistory(splitDateTime[1]), color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                        Row() {
                            Text("At ${splitDateTime[0]}", color = Color.Black)
                        }
                    }
                    Column(
                        modifier = Modifier
                            .width(150.dp)
                    ) {
                        Text(backupLogs.status, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                    Column(
                        modifier = Modifier
                            .width(350.dp)
                    ) {
                        Text(backupLogs.description, color = Color.Black)
                    }

                }
            }
        }
    }
}
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun TimeDropdownMenu(
    selectedTime: String,
    onTimeSelected: (String) -> Unit
) {
    val timeOptions = listOf("08:00", "12:00", "16:00", "20:00","24:00") // รายการเวลา
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        TextField(
            value = selectedTime,
            onValueChange = { }, // Read-Only
            readOnly = true,
            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }, // แทนที่ menuAnchor()
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = Color.Transparent,
                focusedIndicatorColor = Color.White,
                unfocusedIndicatorColor = Color.White,
                textColor = Color.White
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            timeOptions.forEach { time ->
                DropdownMenuItem(
                    content = { Text(time, color = Color.Black) },
                    onClick = {
                        onTimeSelected(time)
                        expanded = false
                    }
                )
            }
        }
    }
}
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SecheduledDropdownMenu(
    selectedFrequency: String,
    onTimeSelected: (String) -> Unit
) {
    val timeOptions = listOf("1 Day", "1 Week", "2 Week", "1 Month","3 Month","6 Month","1 Year") // รายการเวลา
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        TextField(
            value = selectedFrequency,
            onValueChange = { }, // Read-Only
            readOnly = true,
            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }, // แทนที่ menuAnchor()
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = Color.Transparent,
                focusedIndicatorColor = Color.White,
                unfocusedIndicatorColor = Color.White,
                textColor = Color.White
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            timeOptions.forEach { time ->
                DropdownMenuItem(
                    content = { Text(time, color = Color.Black) },
                    onClick = {
                        onTimeSelected(time)
                        expanded = false
                    }
                )
            }
        }
    }
}