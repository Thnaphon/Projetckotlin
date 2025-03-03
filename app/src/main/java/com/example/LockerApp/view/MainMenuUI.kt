package com.example.LockerApp.view

import ParticipantScreen
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.LockerApp.model.CompartmentDao
import com.example.LockerApp.model.LockerDao
import com.example.LockerApp.viewmodel.AccountViewModel
import com.example.LockerApp.viewmodel.BackupViewModel
import com.example.LockerApp.viewmodel.FaceLoginViewModel

import com.example.LockerApp.viewmodel.LockerViewModel
import com.example.LockerApp.viewmodel.MqttViewModel
import com.example.LockerApp.viewmodel.UsageLockerViewModel

@Composable
fun MainMenuUI(
    viewModel: LockerViewModel,
    onNavigateToMqtt: () -> Unit,
    mqttViewModel: MqttViewModel,
    navController: NavHostController,
    lockerDao: LockerDao,
    compartmentDao: CompartmentDao,
    accountViewModel: AccountViewModel,
    usageLockerViewModel: UsageLockerViewModel,
    backupViewModel: BackupViewModel,
    faceLoginViewModel: FaceLoginViewModel,
    accountid: Int // รับค่า accountid

) {
    var showBorrowUI by remember { mutableStateOf(false) }
    var showLockerUI by remember { mutableStateOf(false) }
    var showAddLockerUI by remember { mutableStateOf(false) }
    var showCompartmentUI by remember { mutableStateOf(false) }
    var showReturnUI by remember { mutableStateOf(false) } // เพิ่มตัวแปรควบคุมการแสดง ReturnUI
    var lockerId by remember { mutableStateOf("") }
    var showParticipantUI by remember { mutableStateOf(false) }
    var showUsageHistoryScreen by remember { mutableStateOf(false) }
    var showBackupScreen by remember { mutableStateOf(false) }
    var showUploadpicture by remember { mutableStateOf(false) }



    Row(modifier = Modifier.fillMaxSize()) {
        // Sidebar
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(250.dp)
                .background(Color(0xFFEEEEEE))
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Text("Locker Usage", style = MaterialTheme.typography.h5, color = Color.Black)

            Spacer(modifier = Modifier.height(20.dp))

            // ปุ่มเมนูต่างๆ
            MenuItem(icon = Icons.Default.KeyboardArrowUp, label = "Borrow") {
                showBorrowUI = true
                showLockerUI = false
                showAddLockerUI = false
                showCompartmentUI = false
                showReturnUI = false // ปิด ReturnUI
                showParticipantUI = false
                showBackupScreen = false
                showUsageHistoryScreen = false
                showUploadpicture = false
            }
            MenuItem(icon = Icons.Default.KeyboardArrowDown, label = "Return") {
                showBorrowUI = false
                showLockerUI = false
                showAddLockerUI = false
                showCompartmentUI = false
                showReturnUI = true // แสดง ReturnUI
                showParticipantUI = false
                showBackupScreen = false
                showUsageHistoryScreen = false
                showUploadpicture = false
            }

            // ปุ่มสำหรับโชว์ล็อคเกอร์
            MenuItem(icon = Icons.Default.Home, label = "Show Locker") {
                showLockerUI = true
                showBorrowUI = false
                showAddLockerUI = false
                showCompartmentUI = false
                showReturnUI = false // ปิด ReturnUI
                showParticipantUI = false
                showBackupScreen = false
                showUsageHistoryScreen = false
                showUploadpicture = false
            }
            MenuItem(icon = Icons.Default.AccountCircle, label = "Participant") {
                showBorrowUI = false
                showLockerUI = false
                showAddLockerUI = false
                showCompartmentUI = false
                showReturnUI = false
                showParticipantUI = true // เพิ่มสถานะการแสดงหน้า Participant
                showBackupScreen = false
                showUsageHistoryScreen = false
                showUploadpicture = false
            }
            MenuItem(icon = Icons.Default.Share, label = "History") {
                showBorrowUI = false
                showLockerUI = false
                showAddLockerUI = false
                showCompartmentUI = false
                showReturnUI = false
                showParticipantUI = false
                showUsageHistoryScreen = true// เพิ่มสถานะการแสดงหน้า Participant
                showBackupScreen = false
                showUploadpicture = false
            }
            MenuItem(icon = Icons.Default.Share, label = "Backup") {
                showBorrowUI = false
                showLockerUI = false
                showAddLockerUI = false
                showCompartmentUI = false
                showReturnUI = false
                showParticipantUI = false
                showUsageHistoryScreen = false// เพิ่มสถานะการแสดงหน้า Participant
                showBackupScreen = true
                showUploadpicture = false
            }
            MenuItem(icon = Icons.Default.Share, label = "GoogleDrive") {
                showBorrowUI = false
                showLockerUI = false
                showAddLockerUI = false
                showCompartmentUI = false
                showReturnUI = false
                showParticipantUI = false
                showUsageHistoryScreen = false// เพิ่มสถานะการแสดงหน้า Participant
                showBackupScreen = false
                showUploadpicture = true
            }
        }

        // Content Area
        Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
            when {
                showBorrowUI -> {
                    BorrowUI(viewModel = viewModel, mqttViewModel = mqttViewModel, usageLockerViewModel= usageLockerViewModel, accountid = accountid)

                }
                showReturnUI -> {
                    ReturnUI(viewModel = viewModel, mqttViewModel = mqttViewModel,usageLockerViewModel= usageLockerViewModel, accountid = accountid) // เพิ่มการเรียก ReturnUI
                }
                showLockerUI -> {
                    LockerUI(navController = navController, lockerDao = lockerDao, compartmentDao = compartmentDao,accountid = accountid,) { id ->
                        lockerId = id
                        showCompartmentUI = true
                        showLockerUI = false
                    }
                }

                showCompartmentUI -> {
                    CompartmentUI(lockerId = lockerId.toInt(),accountid = accountid)
                }
                showParticipantUI -> {
                    ParticipantScreen(accountViewModel= accountViewModel,navController = navController , accountid = accountid , faceLoginViewModel = faceLoginViewModel) // แสดงหน้า Participant ที่นี่
                }
                showUsageHistoryScreen -> {
                    UsageHistoryScreen(accountViewModel= accountViewModel,usageLockerViewModel=  usageLockerViewModel, navController = navController) // แสดงหน้า Participant ที่นี่
                }
                showBackupScreen -> {
                    BackupScreen(viewModel = backupViewModel) // แสดงหน้า Participant ที่นี่
                }
                showUploadpicture -> {
                    uploadPicScreen()
                }

                else -> {
                    Text("Content Area", style = MaterialTheme.typography.h4)
                }
            }
        }
    }
}


@Composable
fun MenuItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = Color.Black)
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, color = Color.Black)
    }
}


@Preview()
@Composable
fun Preview02() {
    MenuItem(
        icon = Icons.Default.Home,
        label = "Borrow",
        onClick = { /* ดำเนินการเมื่อกด */ }
    )
}
