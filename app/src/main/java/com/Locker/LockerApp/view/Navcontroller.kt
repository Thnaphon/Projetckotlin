package com.Locker.LockerApp.view

import android.annotation.SuppressLint
import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.Locker.LockerApp.model.LockerDatabase
import com.Locker.LockerApp.viewmodel.AccountViewModel
import com.Locker.LockerApp.viewmodel.BackupViewModel
import com.Locker.LockerApp.viewmodel.FaceLoginViewModel
import com.Locker.LockerApp.viewmodel.LockerViewModel
import com.Locker.LockerApp.viewmodel.LockerViewModelFactory
import com.Locker.LockerApp.viewmodel.MqttViewModel
import com.Locker.LockerApp.viewmodel.UsageLockerViewModel
import com.Locker.LockerApp.viewmodel.FaceRegisterViewModel

@SuppressLint("SuspiciousIndentation")
@Composable
fun LockerApp() {
    val context = LocalContext.current
    val navController = rememberNavController()

    // ใช้ context แทนการส่ง application
    val mqttViewModel = MqttViewModel(context.applicationContext as Application)

    // สร้าง LockerDatabase และ LockerDao
    val lockerDatabase = LockerDatabase.getDatabase(context)
    val lockerDao = lockerDatabase.lockerDao() // ดึง LockerDao
    val compartmentDao = lockerDatabase.compartmentDao() // ดึง CompartmentDao

    // สร้าง LockerViewModel โดยใช้ ViewModelFactory
    val lockerViewModel = LockerViewModelFactory(
        lockerDao,
        compartmentDao
    ).create(LockerViewModel::class.java) // ส่ง compartmentDao

    val accountViewModel: AccountViewModel = viewModel()
    val usageLockerViewModel: UsageLockerViewModel = viewModel()
    val viewModel: BackupViewModel = viewModel()


    val faceLoginViewModel: FaceLoginViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
            context.applicationContext as Application
        )
    )


//    val mqttData by mqttViewModel.mqttData.collectAsState()
//    var Topic = lockerViewModel.topic
//    LaunchedEffect(mqttData) {
//        Log.d("mqttData", "MQTT Topic: ${mqttData.first}, Message: ${mqttData.second}")
//        Log.d("checkmqttdata","${mqttData.first}  ${Topic.value}")
//        Log.d("checkmqttdata","${mqttData.second} == CLOSE")
//        if(mqttData.first == Topic.value && mqttData.second == "CLOSE"){
//            val splitData = mqttData.first.split("/")
//            val usageTime = System.currentTimeMillis().toString()
//            val topicMap = mapOf(
//                "token" to splitData[0],
//                "action" to splitData[1]+"ed",
//                "compartmentId" to splitData[2].toInt(),
//                "status" to splitData[3]
//            )
//            val compartmentId = topicMap["compartmentId"] as? Int ?: 0
//            val action = topicMap["action"] as? String ?: ""
//            val status =  topicMap["status"] as? String ?: ""
//            lockerViewModel.updateCompartmentStatus(
//                compartmentId,
//                action,
//                compartment.LockerID
//            )
//            usageLockerViewModel.insertUsageLocker(
//                compartment.LockerID,
//                compartmentId,
//                usageTime,
//                action,
//                accountid,
//                status
//            )
//
//        }
//    }


//    val lastInteractionTime = remember { mutableStateOf(System.currentTimeMillis()) }
//    val timeoutDuration = 1 * 60 * 1000L // 1 นาที
//    var isSessionTimeout by remember { mutableStateOf(false) }
//
//    fun onUserInteraction() {
//        Log.d("SessionTimeout", "User interacted!")
//        lastInteractionTime.value = System.currentTimeMillis() // อัพเดตเวลาการโต้ตอบล่าสุด
//        isSessionTimeout = false // รีเซ็ตสถานะหมดเวลา
//        Log.d("SessionTimeout", "Timeout status: $isSessionTimeout")
//    }

// เช็คการหมดเวลา
//    LaunchedEffect(lastInteractionTime.value) {
//        while (true) {
//            delay(1000) // ตรวจสอบทุกวินาที
//            val currentTime = System.currentTimeMillis()
//            Log.d("SessionTimeout", "Current time: $currentTime, Last interaction time: ${lastInteractionTime.value}")
//
//            // ตรวจสอบว่าเวลาปัจจุบันห่างจากการโต้ตอบล่าสุดเกินเวลาหมดเวลาแล้วหรือไม่
//            if (currentTime - lastInteractionTime.value > timeoutDuration) {
//                // ถ้าไม่มีการโต้ตอบภายใน 3 นาที
//                isSessionTimeout = true // กำหนดสถานะหมดเวลา
//                Log.d("SessionTimeout", "Session expired, navigating to WelcomePage")
//
//                navController.navigate("WelcomePage") {
//                    popUpTo("WelcomePage") { inclusive = true }
//                }
//                break // หยุด loop หลังจากที่เกิด timeout
//            }
//        }
//    }
//
//    Box(
//        modifier = Modifier
//            .fillMaxSize()  // เพื่อให้ Box มีขนาดเต็มหน้าจอ
//            .pointerInput(Unit) {
//                detectTapGestures(
//                    onTap = { // เปลี่ยนจาก onPress เป็น onTap
//                        onUserInteraction() // รีเซ็ตเวลาการโต้ตอบเมื่อมีการคลิก
//                    }
//                )
//            }
//    ) {
//        // เพิ่มข้อความหรือ UI ที่แสดงเมื่อ session หมดเวลา
//        if (isSessionTimeout) {
//            Text("Session expired. Please log in again.", modifier = Modifier.align(Alignment.Center))
//        }
//    }


    // ฟังก์ชันที่จัดการการโต้ตอบ


    NavHost(
        navController = navController,

        startDestination = "WelcomePage"
//        startDestination = "main_menu/1/service/service"
//        startDestination = "face_capture?name=enemyspotted&role=admin&phone=0634215062/2"
    ) {
        composable("WelcomePage") {
            WelcomePage(
                navController = navController,
                accountViewModel = accountViewModel,
                faceLoginViewModel = faceLoginViewModel
            )
        }

        composable(
            "BackupScreen/{name}",
            arguments = listOf(
                navArgument("name") { type = NavType.StringType }

            )
        ) { backStackEntry ->
            val name = backStackEntry.arguments?.getString("name") ?: "Unknown"
            BackupScreen(
                viewModel = viewModel, accountname = name
            )
        }

        composable("UsageHistoryScreen") {
            UsageHistoryScreen(
                accountViewModel = accountViewModel,
                usageLockerViewModel = usageLockerViewModel,
                navController = navController,
                viewModel = lockerViewModel
            )
        }

        composable(
            "BorrowUI/{accountid}/{name}",
            arguments = listOf(
                navArgument("accountid") { type = NavType.IntType },
                navArgument("name") { type = NavType.StringType }

            )
        ) { backStackEntry ->
            val accountid = backStackEntry.arguments?.getInt("accountid") ?: 0
            val name = backStackEntry.arguments?.getString("name") ?: "Unknown"
            BorrowUI(
                viewModel = lockerViewModel,
                mqttViewModel = mqttViewModel,
                usageLockerViewModel = usageLockerViewModel,
                accountid = accountid, // ส่งค่าไปใช้งานใน UI
                accountname = name
            )
        }
        composable(
            "ReturnUI/{accountid}/{name}",
            arguments = listOf(
                navArgument("accountid") { type = NavType.IntType },
                navArgument("name") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val accountid = backStackEntry.arguments?.getInt("accountid") ?: 0
            val name = backStackEntry.arguments?.getString("name") ?: "Unknown"
            ReturnUI(
                viewModel = lockerViewModel,
                mqttViewModel = mqttViewModel,
                usageLockerViewModel = usageLockerViewModel,
                accountid = accountid, // ส่งค่าไปใช้งานใน UI
                accountname = name
            )
        }
        composable(
            "LockerUI/{accountid}/{name}",
            arguments = listOf(
                navArgument("accountid") { type = NavType.IntType },
                navArgument("name") { type = NavType.StringType }

            )
        ) { backStackEntry ->
            val accountid = backStackEntry.arguments?.getInt("accountid") ?: 0
            val name = backStackEntry.arguments?.getString("name") ?: "Unknown"
            LockerUI(
                lockerDao = lockerDao,
                navController = navController,
                compartmentDao = compartmentDao,
                accountid = accountid, // ส่งค่า accountid ไป
                accountname = name,
                onLockerClick = {
                    // ใส่โค้ดที่ต้องการให้ทำเมื่อมีการคลิกที่ Locker
                }

            )
        }





        composable(
            "compartment_screen/{lockerId}/{accountid}/{name}",
            arguments = listOf(
                navArgument("accountid") { type = NavType.IntType },
                navArgument("name") { type = NavType.StringType }

            )
        ) { backStackEntry ->
            val lockerId = backStackEntry.arguments?.getString("lockerId")?.toIntOrNull()
            val accountid = backStackEntry.arguments?.getInt("accountid") ?: 0
            val name = backStackEntry.arguments?.getString("name") ?: "Unknown"
            if (lockerId != null) {
                CompartmentUI(
                    lockerId = lockerId,
                    viewModel = lockerViewModel,
                    accountid = accountid,
                    accountname = name
                ) // ส่ง LockerViewModel
            }
        }

        composable(
            route = "admin_verification/{adminAccountId}/{adminname}/{adminrole}?name={name}&role={role}&phone={phone}",
            arguments = listOf(
                navArgument("adminAccountId") { type = NavType.IntType },
                navArgument("adminname") { nullable = true; defaultValue = null },
                navArgument("adminrole") { nullable = true; defaultValue = null },
                navArgument("name") { nullable = true; defaultValue = null },
                navArgument("role") { nullable = true; defaultValue = null },
                navArgument("phone") { nullable = true; defaultValue = null }
            )
        ) { backStackEntry ->
            val adminAccountId = backStackEntry.arguments?.getInt("adminAccountId") ?: 0
            val adminname = backStackEntry.arguments?.getString("adminname")
            val adminrole = backStackEntry.arguments?.getString("adminrole")
            val name = backStackEntry.arguments?.getString("name")
            val role = backStackEntry.arguments?.getString("role")
            val phone = backStackEntry.arguments?.getString("phone")

            if (adminname != null && adminrole != null) {
                AdminVerificationPage(
                    navController = navController,
                    adminAccountId = adminAccountId,
                    adminname = adminname,
                    adminrole = adminrole,
                    name = name,
                    role = role,
                    phone = phone
                )
            }
        }

        composable(
            "main_menu/{accountid}/{name}/{role}",
            arguments = listOf(
                navArgument("accountid") { type = NavType.IntType },
                navArgument("name") { type = NavType.StringType },
                navArgument("role") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            // ดึง accountid จาก arguments ที่ส่งมาจาก route
            val accountid = backStackEntry.arguments?.getInt("accountid") ?: 0
            val name = backStackEntry.arguments?.getString("name") ?: "Unknown"
            val role = backStackEntry.arguments?.getString("role") ?: "Unknown"
            MainMenuUI(
                viewModel = lockerViewModel,
                onNavigateToMqtt = { navController.navigate("mqtt_screen") },
                mqttViewModel = mqttViewModel,
                navController = navController,
                lockerDao = lockerDao,
                compartmentDao = compartmentDao,
                accountViewModel = accountViewModel,
                usageLockerViewModel = usageLockerViewModel,
                backupViewModel = viewModel,
                faceLoginViewModel = faceLoginViewModel,
                accountid = accountid,  // ส่ง accountid ไปใช้ใน UI
                accountname = name,
                context = context,
                nameUser = name,
                role = role
            )
        }
        composable(
            "main_menu_user/{accountid}/{name}/{role}",
            arguments = listOf(
                navArgument("accountid") { type = NavType.IntType },
                navArgument("name") { type = NavType.StringType },
                navArgument("role") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            // ดึง accountid จาก arguments ที่ส่งมาจาก route
            val accountid = backStackEntry.arguments?.getInt("accountid") ?: 0
            val name = backStackEntry.arguments?.getString("name") ?: "Unknown"
            val role = backStackEntry.arguments?.getString("role") ?: "Unknown"
            MainMenuUser(
                viewModel = lockerViewModel,
                mqttViewModel = mqttViewModel,
                navController = navController,
                accountViewModel = accountViewModel,
                usageLockerViewModel = usageLockerViewModel,
                accountid = accountid,  // ส่ง accountid ไปใช้ใน UI
                nameUser = name,
                role = role
            )
        }

        composable(
            route = "face_capture/{accountid}/{adminname}/{adminrole}?name={name}&role={role}&phone={phone}",
            arguments = listOf(
                navArgument("accountid") { type = NavType.IntType },
                navArgument("adminname") { defaultValue = "" },
                navArgument("adminrole") { defaultValue = "" },
                navArgument("name") { defaultValue = "" },
                navArgument("role") { defaultValue = "" },
                navArgument("phone") { defaultValue = "" }
            )
        ) {
            val accountid = it.arguments?.getInt("accountid")
            val adminname = it.arguments?.getString("adminname") ?: ""
            val adminrole = it.arguments?.getString("adminrole") ?: ""
            val name = it.arguments?.getString("name") ?: ""
            val role = it.arguments?.getString("role") ?: ""
            val phone = it.arguments?.getString("phone") ?: ""
            val viewModel: FaceRegisterViewModel = viewModel(
                factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
                    context.applicationContext as Application
                )
            )

            if (accountid != null) {
                FaceCapturePage(
                    navController = navController,
                    viewModel = viewModel,
                    accountid = accountid,
                    adminname = adminname,
                    adminrole = adminrole,
                    participantName = name,
                    participantRole = role,
                    participantPhone = phone
                )
            }
        }
    }
}