package com.Locker.LockerApp.view


import androidx.compose.foundation.background

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape

import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.filled.Person

import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload

import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PowerSettingsNew

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.Locker.LockerApp.Component.MenuItem
import com.Locker.LockerApp.model.CompartmentDao
import com.Locker.LockerApp.model.LockerDao
import com.Locker.LockerApp.viewmodel.AccountViewModel
import com.Locker.LockerApp.viewmodel.BackupViewModel

import com.Locker.LockerApp.viewmodel.LockerViewModel
import com.Locker.LockerApp.viewmodel.MqttViewModel
import com.Locker.LockerApp.viewmodel.UsageLockerViewModel

@Composable
fun MainMenuUser(
    viewModel: LockerViewModel,
    mqttViewModel: MqttViewModel,
    navController: NavHostController,
    accountViewModel: AccountViewModel,
    usageLockerViewModel: UsageLockerViewModel,
    accountid: Int,
    nameUser: String,
    role: String

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
                .width(280.dp)
                .fillMaxHeight()
                .background(Color(0xFFEEEEEE))

                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                "Locker Usage",
                style = MaterialTheme.typography.h5.copy(fontWeight = FontWeight.Bold),
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(Color.White, shape = CircleShape)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = "user",
                        tint = Color.Black,
                        modifier = Modifier.size(50.dp)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = nameUser,
                        style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Bold),
                        color = Color.Black,
                        modifier = Modifier.padding(start = 17.dp)
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = role,
                        color = Color.Black,
                        modifier = Modifier.padding(start = 17.dp),
                        fontSize = 12.sp
                    )
                }

            }
            Spacer(modifier = Modifier.height(10.dp))
            MenuItem(
                icon = Icons.Outlined.FileUpload,
                label = "Borrow",
                backgroundColor = Color(0xFF49D457),
                iconSize = 25.dp, // ขนาดของไอคอน
                selected = showBorrowUI,
                onClick ={
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
            )




            MenuItem(icon = Icons.Outlined.FileDownload,
                label = "Return",
                backgroundColor = Color(0xFFFFC353),
                iconSize = 25.dp, // ขนาดของไอคอน
                selected = showReturnUI,
                onClick ={
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
            )
            MenuItem(icon = Icons.Outlined.History,
                label = "History",
                backgroundColor = Color(0xFF1749EE),
                iconSize = 25.dp, // ขนาดของไอคอน
                selected = showUsageHistoryScreen,
                onClick ={
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
            )

            Spacer(modifier = Modifier.weight(1f)) // ดันให้ปุ่ม Logout ไปอยู่ด้านล่างสุด

            MenuItem(
                icon = Icons.Outlined.PowerSettingsNew, // ใช้ไอคอน Logout
                label = "Logout",
                backgroundColor = Color(0xFFEE174A), // สีแดงเพื่อบอกว่าเป็น Logout
                iconSize = 25.dp,
                selected = false,
                onClick = {
                    navController.navigate("WelcomePage") {
                        // Clear the back stack to prevent the user from going back to the previous screen
                        popUpTo("WelcomePage") { inclusive = true }
                    }

                }
            )
        }

        // Content Area
        Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
            when {
                showBorrowUI -> {
                    BorrowUI(viewModel = viewModel, mqttViewModel = mqttViewModel, usageLockerViewModel= usageLockerViewModel, accountid = accountid,accountname=nameUser)

                }
                showReturnUI -> {
                    ReturnUI(viewModel = viewModel, mqttViewModel = mqttViewModel,usageLockerViewModel= usageLockerViewModel, accountid = accountid,accountname=nameUser) // เพิ่มการเรียก ReturnUI
                }


                showUsageHistoryScreen -> {
                    UsageHistoryScreenUser(usageLockerViewModel=  usageLockerViewModel ,viewModel=viewModel,accountname=nameUser,accountid = accountid) // แสดงหน้า Participant ที่นี่
                }



                else -> {
                    BorrowUI(viewModel = viewModel, mqttViewModel = mqttViewModel, usageLockerViewModel= usageLockerViewModel, accountid = accountid,accountname=nameUser)
                }
            }
        }
    }
}