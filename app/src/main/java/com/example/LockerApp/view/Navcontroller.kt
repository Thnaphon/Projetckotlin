package com.example.LockerApp.view

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.LockerApp.model.LockerDatabase
import com.example.LockerApp.viewmodel.AccountViewModel
import com.example.LockerApp.viewmodel.BackupViewModel
import com.example.LockerApp.viewmodel.FaceLoginViewModel
import com.example.LockerApp.viewmodel.LockerViewModel
import com.example.LockerApp.viewmodel.LockerViewModelFactory
import com.example.LockerApp.viewmodel.MqttViewModel
import com.example.LockerApp.viewmodel.UsageLockerViewModel
import com.example.LockerApp.viewmodel.FaceRegisterViewModel
import kotlinx.coroutines.delay
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

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
//        startDestination = "main_menu"
    ) {
        composable("WelcomePage") {
            WelcomePage(
                navController = navController,
                accountViewModel = accountViewModel

            )
        }
        composable("BackupScreen") {
            BackupScreen(
                viewModel = viewModel
            )
        }

        composable("UsageHistoryScreen") {
            UsageHistoryScreen(
                accountViewModel = accountViewModel,
                usageLockerViewModel = usageLockerViewModel,
                navController = navController
            )
        }
        composable(
            "BorrowUI/{accountid}",
            arguments = listOf(navArgument("accountid") { type = NavType.IntType })
        ) { backStackEntry ->
            val accountid = backStackEntry.arguments?.getInt("accountid") ?: 0

            BorrowUI(
                viewModel = lockerViewModel,
                mqttViewModel = mqttViewModel,
                usageLockerViewModel = usageLockerViewModel,
                accountid = accountid // ส่งค่าไปใช้งานใน UI
            )
        }
        composable(
            "ReturnUI/{accountid}",
            arguments = listOf(navArgument("accountid") { type = NavType.IntType })
        ) { backStackEntry ->
            val accountid = backStackEntry.arguments?.getInt("accountid") ?: 0

            ReturnUI(
                viewModel = lockerViewModel,
                mqttViewModel = mqttViewModel,
                usageLockerViewModel = usageLockerViewModel,
                accountid = accountid // ส่งค่าไปใช้งานใน UI
            )
        }
        composable(
            "LockerUI/{accountid}",
            arguments = listOf(navArgument("accountid") { type = NavType.IntType })
        ) { backStackEntry ->
            val accountid = backStackEntry.arguments?.getInt("accountid") ?: 0

            LockerUI(
                lockerDao = lockerDao,
                navController = navController,
                compartmentDao = compartmentDao,
                accountid = accountid, // ส่งค่า accountid ไป
                onLockerClick = {
                    // ใส่โค้ดที่ต้องการให้ทำเมื่อมีการคลิกที่ Locker
                }
            )
        }




        composable("face_login") {
            val context = LocalContext.current
            val viewModel: FaceLoginViewModel = viewModel(
                factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
                    context.applicationContext as Application
                )
            )

            FaceLoginPage(
                navController = navController,
                viewModel = viewModel,
                onLoginSuccess = { accountid, name, role, phone ->
                    val route = "main_menu/$accountid"  // สร้าง route ที่จะส่งไป
                    navController.navigate(route) {   // ใช้ route ที่สร้างขึ้น
                        popUpTo("face_login") { inclusive = true }
                        Log.d("FaceAcountid", "$accountid")
                    }
                }
            )
        }

        composable(
            "main_menu/{accountid}",
            arguments = listOf(navArgument("accountid") { type = NavType.IntType })
        ) { backStackEntry ->
            // ดึง accountid จาก arguments ที่ส่งมาจาก route
            val accountid = backStackEntry.arguments?.getInt("accountid") ?: 0

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
                accountid = accountid  // ส่ง accountid ไปใช้ใน UI
            )
        }

        composable("compartment_screen/{lockerId}") { backStackEntry ->
            val lockerId = backStackEntry.arguments?.getString("lockerId")?.toIntOrNull()
            if (lockerId != null) {
                CompartmentUI(
                    lockerId = lockerId,
                    viewModel = lockerViewModel
                ) // ส่ง LockerViewModel
            }
        }

        composable(
            route = "face_verification/{originAccountId}?name={name}&role={role}&phone={phone}",
            arguments = listOf(
                navArgument("originAccountId") { type = NavType.IntType },
                navArgument("name") { nullable = true; defaultValue = null },
                navArgument("role") { nullable = true; defaultValue = null },
                navArgument("phone") { nullable = true; defaultValue = null }
            )
        ) { backStackEntry ->
            val originAccountId = backStackEntry.arguments?.getInt("originAccountId") ?: 0
            val context = LocalContext.current
            val viewModel: FaceLoginViewModel = viewModel(
                factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
                    context.applicationContext as Application
                )
            )

            FaceVerificationPage(
                navController = navController,
                viewModel = viewModel,
                expectedAccountId = originAccountId,
                onVerificationSuccess = {
                    // This is called when there's no registration data
                    // Navigate to the registration form with the verified accountId
                    navController.navigate("registration_form/$originAccountId") {
                        popUpTo("face_verification/$originAccountId") { inclusive = true }
                    }
                },
                onVerificationFailed = {
                    // Navigate back to main menu
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "admin_verification/{adminAccountId}?name={name}&role={role}&phone={phone}",
            arguments = listOf(
                navArgument("adminAccountId") { type = NavType.IntType },
                navArgument("name") { nullable = true; defaultValue = null },
                navArgument("role") { nullable = true; defaultValue = null },
                navArgument("phone") { nullable = true; defaultValue = null }
            )
        ) { backStackEntry ->
            val adminAccountId = backStackEntry.arguments?.getInt("adminAccountId") ?: 0
            val name = backStackEntry.arguments?.getString("name")
            val role = backStackEntry.arguments?.getString("role")
            val phone = backStackEntry.arguments?.getString("phone")

            AdminVerificationPage(
                navController = navController,
                adminAccountId = adminAccountId,
                name = name,
                role = role,
                phone = phone
            )
        }

//        composable(
//            "registration_form/{accountId}",
//            arguments = listOf(navArgument("accountId") { type = NavType.IntType })
//        ) { backStackEntry ->
//            val accountId = backStackEntry.arguments?.getInt("accountId") ?: 0
//
//            RegistrationForm(
//                navController = navController,
//                accountId = accountId
//            )
//        }

        composable(
            route = "face_register?name={name}&role={role}&phone={phone}/{accountid}",
            arguments = listOf(
                navArgument("accountid") { type = NavType.IntType },
                navArgument("name") { defaultValue = "" },
                navArgument("role") { defaultValue = "" },
                navArgument("phone") { defaultValue = "" },
            )
        ) {
            val accountid = it.arguments?.getInt("accountid")
            val name = it.arguments?.getString("name") ?: ""
            val role = it.arguments?.getString("role") ?: ""
            val phone = it.arguments?.getString("phone") ?: ""

            // Initialize FaceDetectionViewModel
            val viewModel: FaceRegisterViewModel = viewModel(
                factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
                    context.applicationContext as Application
                )
            )

            if (accountid != null) {
                FaceRegisterPage(
                    navController = navController,
                    viewModel = viewModel,
                    accountid = accountid,
                    participantName = name,
                    participantRole = role,
                    participantPhone = phone
                )
            }
        }

        composable(
            route = "face_capture?name={name}&role={role}&phone={phone}/{accountid}",
            arguments = listOf(
                navArgument("accountid") { type = NavType.IntType },
                navArgument("name") { defaultValue = "" },
                navArgument("role") { defaultValue = "" },
                navArgument("phone") { defaultValue = "" }
            )
        ) {
            val accountid = it.arguments?.getInt("accountid")
            val name = it.arguments?.getString("name") ?: ""
            val role = it.arguments?.getString("role") ?: ""
            val phone = it.arguments?.getString("phone") ?: ""

            // Initialize FaceRegisterViewModel
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
                    participantName = name,
                    participantRole = role,
                    participantPhone = phone
                )
            }
        }

    }
}