package com.example.LockerApp.view


import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.tooling.preview.Preview
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
    val mqttService = MqttService()
    var isBackupInProgress by remember { mutableStateOf(false) }
    var lastBackupDate by remember { mutableStateOf("21 March 2024 At 8:45 AM") }
    var scheduledBackup by remember { mutableStateOf("Daily") }
    var backupTime by remember { mutableStateOf("0:00 O’Clock") }
    var description by remember { mutableStateOf("Full Backup") }

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
        BackupSectionTitle(title = "Scheduled", showButton = true,viewModel=viewModel)
        BackupSection(title = "Scheduled")
        Spacer(modifier = Modifier.height(20.dp))

        // Recent Backup Section
        BackupSectionTitle(title = "Recent", showButton = false ,viewModel=viewModel)
        RecentBackup(title = "Recent")
    }
}

@Composable
fun BackupSectionTitle(viewModel: BackupViewModel,title: String, showButton:Boolean) {
    val context = LocalContext.current
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
                        Button(onClick = { viewModel.performBackup(context) }) {
                            Text("Backup")
                        }
                    }
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Button(onClick = { viewModel.performRestore(context) }) {
                            Text("Restore")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BackupSection(title: String) {
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
                    Text("Scheduled", color = Color.Black)
                }
                Column(
                    modifier = Modifier
                        .width(350.dp)
                ) {
                    Text("Description", color = Color.Black)
                }
            }
            Row() {
                ScheduledBackupCard()
            }
        }
    }
}

@Composable
fun ScheduledBackupCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),

    ) {
        Column() {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(65.dp)
                    .background(Color.White)
                    .padding(15.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier
                    .width(150.dp)) {
                    Text("Backup", color = Color.Black, fontWeight = FontWeight.Bold)

                }
                Column(modifier = Modifier
                    .width(150.dp)) {
                    Row(){
                        Text("21 March 2024", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                    Row(){
                        Text("At 8:45 AM", color = Color.Black)
                    }
                }
                Column(modifier = Modifier
                    .width(150.dp)) {
                    Row(){
                        Text("0:00 O'Clock", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                    Row(){
                        Text("Daily", color = Color.Black)
                    }
                }
                Column(modifier = Modifier
                    .width(350.dp)) {
                    Text("Full Backup", color = Color.Black)
                }
            }
        }
//        Row() {
//            Column(modifier = Modifier.padding(16.dp)) {
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.SpaceBetween
//                ) {
//                    Text("Scheduled", color = Color.White)
//                    Text(schedule, color = Color.White)
//                }
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.SpaceBetween
//                ) {
//                    Text("Time", color = Color.White)
//                    Text(time, color = Color.White)
//                }
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.SpaceBetween
//                ) {
//                    Text("Description", color = Color.White)
//                    Text(description, color = Color.White)
//                }
//            }
//        }
    }
}

@Composable
fun RecentBackup(title: String) {
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
                RecentBackupCard()
            }
        }
    }
}

@Composable
fun RecentBackupCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),

    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(65.dp)
                    .background(Color.White)
                    .padding(15.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier
                    .width(150.dp)) {
                    Text("Backup", color = Color.Black, fontWeight = FontWeight.Bold)

                }
                Column(modifier = Modifier
                    .width(150.dp)) {
                    Row(){
                        Text("21 March 2024", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                    Row(){
                        Text("At 8:45 AM", color = Color.Black)
                    }
                }
                Column(modifier = Modifier
                    .width(150.dp)) {
                    Text("Completed", color = Color.Black, fontWeight = FontWeight.Bold)
                }
                Column(modifier = Modifier
                    .width(350.dp)) {
                    Text("Full Backup", color = Color.Black)
                }

            }
//            Row(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(16.dp),
//                horizontalArrangement = Arrangement.SpaceBetween,
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Column {
//                    Text("Backup", fontWeight = FontWeight.Bold)
//                    Text(date, color = Color.Gray)
//                }
//                Text("Completed", color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
//                Text(description)
//                Button(onClick = { /* Restore Action */ }) {
//                    Text("Restore")
//                }
//            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewBackupScreen() {
    val mockViewModel = BackupViewModel()
    BackupScreen(viewModel = mockViewModel)
}
