package com.example.LockerApp.view

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.LockerApp.model.LockerDatabase
import com.example.LockerApp.viewmodel.AccountViewModel
import com.example.LockerApp.viewmodel.LockerViewModel
import com.example.LockerApp.viewmodel.LockerViewModelFactory
import com.example.LockerApp.viewmodel.MqttViewModel


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

    NavHost(
        navController = navController,
        startDestination = "mqtt_screen"
    ) {
        composable("mqtt_screen") {
            MqttScreen(
                viewModel = mqttViewModel,
                navController = navController
            )
        }

        composable("main_menu") {
            MainMenuUI(
                viewModel = lockerViewModel,
                onNavigateToMqtt = { navController.navigate("mqtt_screen") },
                mqttViewModel = mqttViewModel,
                navController = navController,
                lockerDao = lockerDao,
                compartmentDao = compartmentDao,
                accountViewModel = accountViewModel// ส่ง compartmentDao
            )
        }

        composable("compartment_screen/{lockerId}") { backStackEntry ->
            val lockerId = backStackEntry.arguments?.getString("lockerId")?.toIntOrNull()
            if (lockerId != null) {
                CompartmentUI(lockerId = lockerId, viewModel = lockerViewModel) // ส่ง LockerViewModel
            }
        }

        composable("add_locker") {
            AddLockerScreen(
                navController = navController,
                viewModel = lockerViewModel, // ตรวจสอบให้แน่ใจว่าส่ง LockerViewModel
                lockerDao = lockerDao,
                mqttViewModel = mqttViewModel
            )
        }
        composable("face_detection") { // เพิ่มการนำทางไปยังหน้า FaceDetectionPage
            FaceDetectionPage(navController = navController)
        }
    }
}