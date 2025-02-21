package com.example.LockerApp.view


import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
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
import androidx.compose.ui.platform.LocalContext
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

    // ตัวแปรสำหรับแสดงวันที่สำรองข้อมูลล่าสุด
    var lastBackupDate by remember { mutableStateOf("ไม่พบข้อมูลการสำรอง") }

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
        verticalArrangement = Arrangement.Center
    ) {
        // แสดงข้อความเกี่ยวกับสถานะการสำรองข้อมูลล่าสุด
        Text(text = "วันที่สำรองข้อมูลล่าสุด: $lastBackupDate")

        Spacer(modifier = Modifier.height(20.dp))

        // ปุ่มสำหรับเริ่มการสำรองข้อมูล
        Button(
            onClick = {
                // เรียกฟังก์ชันสำรองข้อมูล
                viewModel.performBackup(context)
                // เรียกฟังก์ชันส่งไฟล์สำรองไปยัง Pi
                viewModel.performBackupToPi(mqttService, context) // ส่ง mqttService ที่นี่
                Toast.makeText(context, "กำลังทำการสำรองข้อมูล...", Toast.LENGTH_SHORT).show()
            }
        ) {
            Text("สำรองข้อมูลทันที")
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ปุ่มสำหรับการคืนค่าข้อมูลจากไฟล์สำรอง
        Button(
            onClick = {
                // เรียกฟังก์ชัน restore ข้อมูล
                viewModel.performRestore(context)
                Toast.makeText(context, "กำลังทำการคืนค่าข้อมูล...", Toast.LENGTH_SHORT).show()
            }
        ) {
            Text("คืนค่าข้อมูลจากสำรอง")
        }

        Spacer(modifier = Modifier.height(20.dp))

        // แสดงสถานะการสำรองข้อมูล
        Text(
            text = "สถานะการสำรองข้อมูล: ${if (lastBackupDate == "ไม่พบข้อมูลการสำรอง") "ยังไม่มีการสำรอง" else "สำรองข้อมูลล่าสุดเมื่อ $lastBackupDate"}"
        )

        Spacer(modifier = Modifier.height(20.dp))

        // แสดงชื่อไฟล์และที่อยู่ของไฟล์สำรอง
        Text("ชื่อไฟล์สำรอง: ${viewModel.backupFileName.value}")
        Text("ที่อยู่ไฟล์สำรอง: ${viewModel.backupFilePath.value}")
    }
}
