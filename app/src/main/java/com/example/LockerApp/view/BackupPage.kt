package com.example.LockerApp.view


import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.LockerApp.model.BackupSettings
import com.example.LockerApp.model.LockerDatabase
import com.example.LockerApp.service.MqttService
import com.example.LockerApp.viewmodel.BackupViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupScreen(viewModel: BackupViewModel) {
    val context = LocalContext.current
    val mqttService = MqttService() // สร้าง mqttService ที่นี่
    var isBackupInProgress by remember { mutableStateOf(false) }
    var backupStatusMessage by remember { mutableStateOf("") }
    // ตัวแปรสำหรับแสดงวันที่สำรองข้อมูลล่าสุด
    var lastBackupDate by remember { mutableStateOf("ไม่พบข้อมูลการสำรอง") }
    var isRestoreInProgress by remember { mutableStateOf(false) }
    // ดึงข้อมูลการสำรองล่าสุดจากฐานข้อมูล
    LaunchedEffect(Unit) {
        // ในกรณีนี้เราจะไม่ดึงข้อมูลจากฐานข้อมูลจริง ๆ แต่ให้แสดงผลเป็นข้อมูลจำลอง
        lastBackupDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top

    ) {

        Text(
            "Backup",
            style = MaterialTheme.typography.h5.copy(fontWeight = FontWeight.Bold),
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.Backup,
                contentDescription = "History Icon",
                tint = Color.Black,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Scheduled",
                style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.SemiBold),
                color = Color.Black
            )
            Spacer(modifier = Modifier.weight(1f))
            Card(
                modifier = Modifier
                    .size(47.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .border(2.dp, Color(0xFF3961AA), RoundedCornerShape(15.dp)), // มุมมนของการ์ด
                elevation = 8.dp, // ความสูงของเงา
                backgroundColor = Color.White // พื้นหลังสีขาวของการ์ด
            ) {
                IconButton(
                    onClick = {},
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
        androidx.compose.material3.Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp)
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Button(
                onClick = {
                    isBackupInProgress = true
                    backupStatusMessage = "กำลังสำรองข้อมูล..."
                    // เรียกฟังก์ชัน performBackupToPi
                    viewModel.performBackupToPi(mqttService, context)
                },
            ) {
                Text("สำรองข้อมูล")
            }

            // แสดงข้อความสถานะการสำรองข้อมูล
            if (isBackupInProgress) {
                Text(text = backupStatusMessage)
            }

            Button(
                onClick = {
                    isRestoreInProgress = true
                    // เรียกฟังก์ชัน restoreBackupFromPi
                    viewModel.restoreBackupFromPi(mqttService, context)
                },
            ) {
                Text("รีสโตร์ข้อมูล")
            }
        }
    }
}
