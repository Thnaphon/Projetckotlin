package com.example.LockerApp.view


import ParticipantScreen
import android.app.Activity
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.SpaceDashboard
import androidx.compose.material.icons.outlined.Splitscreen
import androidx.compose.material.icons.outlined.Upload
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
    accountid: Int,
    context: Context,// รับค่า accountid
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
            // ปุ่มเมนูต่างๆ
            MenuItem(
                icon = Icons.Outlined.FileUpload,
                label = "Borrow",
                backgroundColor = Color(0xFF49D457),
                iconSize = 25.dp, // ขนาดของไอคอน
                selected = showBorrowUI,
                onClick = {
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
                onClick = {
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
                onClick = {
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
            Spacer(modifier = Modifier.height(15.dp))
            Text("Setting", style = MaterialTheme.typography.h6, color = Color.Black)
            Spacer(modifier = Modifier.height(10.dp))
            // ปุ่มสำหรับโชว์ล็อคเกอร์
            MenuItem(icon = Icons.Outlined.SpaceDashboard,
                label = "Locker",
                backgroundColor = Color(0xFFBE39E7),
                iconSize = 25.dp, // ขนาดของไอคอน
                selected = showLockerUI,
                onClick = {
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
            )
            MenuItem(icon = Icons.Outlined.Group,
                label = "Participant",
                backgroundColor = Color(0xFF908E98),
                iconSize = 25.dp, // ขนาดของไอคอน
                selected = showParticipantUI,
                onClick = {
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
            )
            MenuItem(icon = Icons.Outlined.CloudUpload,
                label = "Backup",
                backgroundColor = Color(0xFFEE6617),
                iconSize = 25.dp, // ขนาดของไอคอน
                selected = showBackupScreen,
                onClick = {
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            when {
                showBorrowUI -> {
                    BorrowUI(
                        viewModel = viewModel,
                        mqttViewModel = mqttViewModel,
                        usageLockerViewModel = usageLockerViewModel,
                        accountid = accountid
                    )

                }

                showReturnUI -> {
                    ReturnUI(
                        viewModel = viewModel,
                        mqttViewModel = mqttViewModel,
                        usageLockerViewModel = usageLockerViewModel,
                        accountid = accountid
                    ) // เพิ่มการเรียก ReturnUI
                }

                showLockerUI -> {
                    LockerUI(
                        navController = navController,
                        lockerDao = lockerDao,
                        compartmentDao = compartmentDao,
                        accountid = accountid,
                    ) { id ->
                        lockerId = id
                        showCompartmentUI = true
                        showLockerUI = false
                    }
                }

                showCompartmentUI -> {
                    CompartmentUI(lockerId = lockerId.toInt(), accountid = accountid)
                }

                showParticipantUI -> {
                    ParticipantScreen(
                        accountViewModel = accountViewModel,
                        navController = navController,
                        faceLoginViewModel = faceLoginViewModel,
                        accountid = accountid,
                        adminname = nameUser,
                        adminrole = nameUser,
                        viewModel = viewModel
                    ) // แสดงหน้า Participant ที่นี่
                }

                showUsageHistoryScreen -> {
                    UsageHistoryScreen(
                        accountViewModel = accountViewModel,
                        usageLockerViewModel = usageLockerViewModel,
                        navController = navController,
                        viewModel = viewModel
                    ) // แสดงหน้า Participant ที่นี่
                }

                showBackupScreen -> {
                    BackupScreen(viewModel = backupViewModel) // แสดงหน้า Participant ที่นี่
                }


                else -> {
                    BorrowUI(
                        viewModel = viewModel,
                        mqttViewModel = mqttViewModel,
                        usageLockerViewModel = usageLockerViewModel,
                        accountid = accountid
                    )
                }
            }
        }
    }
}


@Composable
fun MenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    iconTint: Color = Color.White, // สีของไอคอน
    iconSize: Dp = 24.dp, // ขนาดของไอคอน
    backgroundColor: Color = Color.LightGray,
    selected: Boolean// สีพื้นหลังของไอคอน
) {
    val bgColor = if (selected) Color(0xFF3A4750) else Color.Transparent
    val textColor = if (selected) Color.White else Color.Black
    Row(
        modifier = Modifier
            .fillMaxWidth()

            .clickable(onClick = onClick)
            .background(bgColor, shape = RoundedCornerShape(15.dp))

            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(iconSize * 1.65f) // ให้ขนาด Box ใหญ่กว่าไอคอนนิดหน่อย
                .background(
                    backgroundColor,
                    shape = RoundedCornerShape(10.dp)
                ) // กำหนดสีพื้นหลังและขอบมน
                .padding(8.dp), // ให้มีระยะห่างระหว่างขอบกับไอคอน
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = iconTint, // กำหนดสีของไอคอน
                modifier = Modifier.size(iconSize) // กำหนดขนาดของไอคอน
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, color = textColor)
    }
}


