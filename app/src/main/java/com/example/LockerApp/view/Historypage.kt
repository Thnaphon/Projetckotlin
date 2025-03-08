package com.example.LockerApp.view

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.TextField

import androidx.compose.material.icons.filled.List

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.AlertDialog
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material3.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberImagePainter
import com.example.LockerApp.viewmodel.AccountViewModel
import com.example.LockerApp.viewmodel.LockerViewModel
import com.example.LockerApp.viewmodel.UsageLockerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.log

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun UsageHistoryScreen(accountViewModel: AccountViewModel, usageLockerViewModel: UsageLockerViewModel, navController: NavController,viewModel: LockerViewModel) {
    var searchQuery by remember { mutableStateOf("") }

    // ดึงข้อมูลจาก ViewModel
    val usageLockers by usageLockerViewModel.allUsageLockers.observeAsState(emptyList())
    val manageLockers by usageLockerViewModel.allManageLockers.observeAsState(emptyList())



    val filteredUsageLockers = usageLockers.filter {
        it.Usage.contains(searchQuery, ignoreCase = true) ||
                it.LockerID.toString().contains(searchQuery, ignoreCase = true) ||
                it.CompartmentID.toString().contains(searchQuery, ignoreCase = true)

    }

    val usageLockerCount = filteredUsageLockers.size

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                ,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(text = "History", style = MaterialTheme.typography.h5.copy(fontWeight = FontWeight.Bold), color = Color.Black)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = "History Icon",
                        tint = Color.Black,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "$usageLockerCount History", style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.SemiBold), color = Color.Black)
                }

                Spacer(modifier = Modifier.weight(1f))
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search, // ใช้ไอคอนค้นหา
                            contentDescription = "Search Icon"
                        )},
                    placeholder = {
                        Text("Search")  // ใช้ placeholder แทน label
                    },
                    colors = TextFieldDefaults.textFieldColors(
                        backgroundColor = Color.Transparent, // ตั้งให้พื้นหลังโปร่งใส
                        focusedIndicatorColor = Color.Transparent,  // สีเส้นขอบเมื่อเลือก
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .width(270.dp)
                        .padding(8.dp)
                        .border(1.dp, Color.Black, RoundedCornerShape(25))
                )
                Spacer(modifier = Modifier.width(10.dp))

                DropdownLocker(viewModel)
                Spacer(modifier = Modifier.width(10.dp))

                DropdownHistory(viewModel)


            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 8.dp)
                        .weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(

                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFEEEEEE))
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically

                        ) {

                            Text(
                                "Name",
                                Modifier.weight(0.8f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "Locker",
                                Modifier
                                    .weight(0.8f)
                                    .padding(start = 16.dp),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "Com.",
                                Modifier
                                    .weight(0.5f)
                                    .padding(start = 16.dp),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "Equipment",
                                Modifier
                                    .weight(0.8f)
                                    .padding(start = 16.dp),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "Usage Time",
                                Modifier.weight(1.4f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "Usage",
                                Modifier.weight(1f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "Status",
                                Modifier.weight(1f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                        }

                    }
                    LazyColumn (modifier = Modifier
                        .fillMaxWidth().padding(start = 8.dp, end = 8.dp))
                    {
                        items(filteredUsageLockers) { usageLocker ->
                            val accountNameUsageLocker by accountViewModel.getAccountNameById(usageLocker.AccountID).observeAsState("Unknown")
                            val compartmentList by viewModel.getCompartmentBycompartmentId(usageLocker.CompartmentID).collectAsState(initial = emptyList())
                            Log.d("compartmentList","$compartmentList")
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 17.dp, bottom = 12.dp)
                            ) {
                                Text(
                                    text = accountNameUsageLocker,
                                    modifier = Modifier.weight(0.8f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Locker ${usageLocker.LockerID.toString()}",
                                    modifier = Modifier
                                        .weight(0.8f)
                                        .padding(start = 16.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = compartmentList.firstOrNull()?.number_compartment?.toString() ?: "N/A",
                                    modifier = Modifier
                                        .weight(0.5f)
                                        .padding(start = 16.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = compartmentList.firstOrNull()?.Name_Item?.toString() ?: "N/A",
                                    modifier = Modifier
                                        .weight(0.8f)
                                        .padding(start = 16.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = formatTimestamp(usageLocker.UsageTime),
                                    modifier = Modifier.weight(1.4f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = usageLocker.Usage,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = usageLocker.Status,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            }
                            Divider(color = Color(0xFFE8E8E8), thickness = 1.dp)
                        }

                        items(manageLockers) { manageLocker ->
                            val accountNameUsageLocker by accountViewModel.getAccountNameById(manageLocker.AccountID).observeAsState("Unknown")
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 17.dp, bottom = 12.dp)
                            ) {
                                Text(
                                    text = accountNameUsageLocker,
                                    modifier = Modifier.weight(0.8f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Locker ${manageLocker.LockerID.toString()}",
                                    modifier = Modifier
                                        .weight(0.8f)
                                        .padding(start = 16.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "N/A",
                                    modifier = Modifier
                                        .weight(0.5f)
                                        .padding(start = 16.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "N/A",
                                    modifier = Modifier
                                        .weight(0.8f)
                                        .padding(start = 16.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = formatTimestamp(manageLocker.UsageTime),
                                    modifier = Modifier.weight(1.4f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = manageLocker.Usage,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = manageLocker.Status,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            }
                            Divider(color = Color(0xFFE8E8E8), thickness = 1.dp)
                        }

                    }

                }
            }
        }
    }
}

// ฟังก์ชันแปลง timestamp เป็นรูปแบบ hh:mm dd/MM/yyyy
fun formatTimestamp(timestamp: String): String {
    return try {
        val date = Date(timestamp.toLong())
        val sdf = SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault())
        sdf.format(date)
    } catch (e: Exception) {
        "Invalid date"
    }
}
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DropdownLocker(viewModel: LockerViewModel) {

    val lockers by viewModel.lockers.collectAsState()
    var selectedLocker by remember { mutableStateOf(0) }
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        Box(
            modifier = Modifier
                .wrapContentWidth()
                .height(48.dp) // ตั้งค่าความสูงให้เหมือนปุ่ม
                .border(2.dp, Color.Black, RoundedCornerShape(15.dp)) // เพิ่มขอบมน
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
                    text = if (selectedLocker == 0) "All Lockers" else "Locker $selectedLocker",
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
            modifier = Modifier.wrapContentSize()
        ) {
            lockers.forEach { locker ->
                DropdownMenuItem(onClick = {
                    selectedLocker = locker.LockerID
                    expanded = false
                }) {
                    Text("Locker ${locker.LockerID}")
                }
            }
            DropdownMenuItem(onClick = {
                selectedLocker = 0 // เลือก All Lockers
                expanded = false
            }) {
                Text("All Lockers")
            }
        }
    }
}
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DropdownHistory(viewModel: LockerViewModel) {

    val lockers by viewModel.lockers.collectAsState()
    var selectedLocker by remember { mutableStateOf(0) }
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        Box(
            modifier = Modifier
                .wrapContentWidth()
                .height(48.dp) // ตั้งค่าความสูงให้เหมือนปุ่ม
                .border(2.dp, Color.Black, RoundedCornerShape(15.dp)) // เพิ่มขอบมน
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
                    text = if (selectedLocker == 0) "All Lockers" else "Locker $selectedLocker",
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
            modifier = Modifier.wrapContentSize()
        ) {
            lockers.forEach { locker ->
                DropdownMenuItem(onClick = {
                    selectedLocker = locker.LockerID
                    expanded = false
                }) {
                    Text("Locker ${locker.LockerID}")
                }
            }
            DropdownMenuItem(onClick = {
                selectedLocker = 0 // เลือก All Lockers
                expanded = false
            }) {
                Text("All History")
            }
        }
    }
}

