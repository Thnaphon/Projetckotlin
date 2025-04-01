package com.example.LockerApp.Component

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.LockerApp.viewmodel.LockerViewModel

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DropdownLocker(viewModel: LockerViewModel, selectedlocker: String, onRoleChange: (String) -> Unit) {

    val lockers by viewModel.lockers.collectAsState()
    var selectedLocker by remember { mutableStateOf(0) }
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        Box(
            modifier = Modifier
                .wrapContentWidth()
                .height(56.dp) // ตั้งค่าความสูงให้เหมือนปุ่ม
                .border(2.dp, Color(0xFF8D8B8B), RoundedCornerShape(15.dp)) // เพิ่มขอบมน
                .clickable { expanded = true }
                .padding(horizontal = 16.dp, vertical = 12.dp), // จัดการ padding
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.wrapContentWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedLocker == 0) "All Lockers" else "All Users",
                    style = MaterialTheme.typography.body1
                )
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown, // เปลี่ยนไอคอนเป็นลูกศรลง
                    contentDescription = "Dropdown Icon"
                )
            }
        }
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.wrapContentSize().verticalScroll(rememberScrollState())
        ) {
            lockers.forEach { locker ->
                DropdownMenuItem(onClick = {
                    onRoleChange(locker.LockerID.toString())
                    expanded = false
                }

                ) {
                    Text("Locker ${locker.Lockername}")
                }
            }
            DropdownMenuItem(onClick = {
                onRoleChange("all locker")
                expanded = false

            }) {
                Text("All Lockers")
            }




        }
    }
}