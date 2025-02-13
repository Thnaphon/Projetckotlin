package com.example.LockerApp.view

import android.app.Application
import android.util.Log
import androidx.compose.runtime.Composable
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
    val lockerViewModel = LockerViewModelFactory(lockerDao, compartmentDao).create(LockerViewModel::class.java) // ส่ง compartmentDao

    val accountViewModel: AccountViewModel = viewModel()
    val usageLockerViewModel: UsageLockerViewModel = viewModel()
    val viewModel : BackupViewModel = viewModel()


    NavHost(
        navController = navController,

        startDestination = "WelcomePage"
//        startDestination = "main_menu"
    ) {
        composable("WelcomePage") {
            WelcomePage(
                navController = navController
            )
        }
        composable("BackupScreen") {
            BackupScreen(
                viewModel = viewModel
            )
        }

        composable("UsageHistoryScreen") {
            UsageHistoryScreen(
                usageLockerViewModel = usageLockerViewModel,
                navController = navController
            )
        }
        composable("BorrowUI/{accountid}", arguments = listOf(navArgument("accountid") { type = NavType.IntType })) { backStackEntry ->
            val accountid = backStackEntry.arguments?.getInt("accountid") ?: 0

            BorrowUI(
                viewModel = lockerViewModel,
                mqttViewModel = mqttViewModel,
                usageLockerViewModel = usageLockerViewModel,
                accountid = accountid // ส่งค่าไปใช้งานใน UI
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

        composable("main_menu/{accountid}", arguments = listOf(navArgument("accountid") { type = NavType.IntType })) { backStackEntry ->
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
                CompartmentUI(lockerId = lockerId, viewModel = lockerViewModel) // ส่ง LockerViewModel
            }
        }

        composable(
            route = "face_detection?name={name}&role={role}&phone={phone}",
            arguments = listOf(
                navArgument("name") { defaultValue = "" },
                navArgument("role") { defaultValue = "" },
                navArgument("phone") { defaultValue = "" },
            )
        ) {
            val name = it.arguments?.getString("name") ?: ""
            val role = it.arguments?.getString("role") ?: ""
            val phone = it.arguments?.getString("phone") ?: ""

            // Initialize FaceDetectionViewModel
            val viewModel: FaceRegisterViewModel = viewModel(
                factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
                    context.applicationContext as Application
                )
            )

            FaceRegisterPage(
                navController = navController,
                viewModel = viewModel,
                participantName = name,
                participantRole = role,
                participantPhone = phone
            )
        }
    }
}