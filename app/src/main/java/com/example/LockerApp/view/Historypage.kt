package com.example.LockerApp.view


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
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material3.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.LockerApp.Component.DropdownHistory
import com.example.LockerApp.Component.DropdownLocker
import com.example.LockerApp.Component.HeaderRow
import com.example.LockerApp.model.combine
import com.example.LockerApp.utils.formatDateHistory
import com.example.LockerApp.utils.formatTimestamp
import com.example.LockerApp.viewmodel.AccountViewModel
import com.example.LockerApp.viewmodel.LockerViewModel
import com.example.LockerApp.viewmodel.ManageAccountViewModel
import com.example.LockerApp.viewmodel.UsageLockerViewModel
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId


@Composable
fun UsageHistoryScreen(accountViewModel: AccountViewModel, usageLockerViewModel: UsageLockerViewModel, navController: NavController,viewModel: LockerViewModel) {
    var searchQuery by remember { mutableStateOf("") }
    val manageAccountViewModel : ManageAccountViewModel = viewModel()
    // ดึงข้อมูลจาก ViewModel
    val usageLockers by usageLockerViewModel.allUsageLockers.observeAsState(emptyList())
    val manageLockers by usageLockerViewModel.allManageLockers.observeAsState(emptyList())
    val manageAccounts by manageAccountViewModel.manageAccounts.observeAsState(emptyList())
    var filterShowcolumn by remember { mutableStateOf("All History") }
    var selectedlocker by remember { mutableStateOf("all locker") }
    val filteredUsageLockers = usageLockers.filter {
        (selectedlocker == "all locker" || it.locker_name == selectedlocker) &&
                (it.Usage.contains(searchQuery, ignoreCase = true) ||
                        it.locker_name.contains(searchQuery, ignoreCase = true) ||
                        it.number_compartment.toString().contains(searchQuery, ignoreCase = true) ||
                        it.name_equipment.contains(searchQuery, ignoreCase = true))

    }
    val filteredUsageLockersTransformed = filteredUsageLockers.map {
        combine(it.name_user, it.locker_name,it.number_compartment,it.UsageTime,it.Status,it.Usage,it.name_equipment,null) // ไม่มี Status สำหรับ filteredUsageLockers
    }

    val manageLockersTransformed = manageLockers.map {
        combine(it.name_user, it.locker_name,null,it.UsageTime, it.Status,it.Usage,null,null) // มี Status สำหรับ manageLockers
    }
    val manageAccountsTransformed = manageAccounts.map {
        combine(it.actoin_username, null,null,it.UsageTime, "Success",it.Usage,null,it.name_user) // มี Status สำหรับ manageLockers
    }

    val combinedLockers = (filteredUsageLockersTransformed + manageLockersTransformed+manageAccountsTransformed)
        .sortedByDescending  {
            val timestamp = it.UsageTime.toLong() // UsageTime เป็น String จึงแปลงเป็น Long
            val instant = Instant.ofEpochMilli(timestamp) // แปลงเป็น Instant
            val dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault()) // แปลงเป็น LocalDateTime
            dateTime
        }

    var usageLockerCount by remember { mutableStateOf(0) }
    if (filterShowcolumn =="All History")
    {
        usageLockerCount = combinedLockers.size
    }
    LaunchedEffect(Unit) {
        manageAccountViewModel.getAllManageAccounts()
    }



    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 10.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(text = "History", style = MaterialTheme.typography.h5.copy(fontWeight = FontWeight.Bold), color = Color.Black)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
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
                        Text(
                            "Search",
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,

                            )
                    },
                    singleLine = true,
                    colors = TextFieldDefaults.textFieldColors(
                        backgroundColor = Color.Transparent, // ตั้งให้พื้นหลังโปร่งใส
                        focusedIndicatorColor = Color.Transparent,  // สีเส้นขอบเมื่อเลือก
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .width(270.dp)
                        .height(56.dp)
                        .border(2.dp, Color(0xFF8D8B8B), RoundedCornerShape(25))

                )
                Spacer(modifier = Modifier.width(10.dp))

                DropdownLocker(viewModel=viewModel,selectedlocker = selectedlocker, onRoleChange = { selectedlocker = it })
                Spacer(modifier = Modifier.width(10.dp))

                DropdownHistory(selectedhistory = filterShowcolumn , onslectChange = { filterShowcolumn = it })


            }

            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 8.dp)
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(16.dp)
            ) {

                Column() {
                    HeaderRow()
                    Row (horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically)
                    {



                        val filteredCombinedLockers = when (filterShowcolumn) {
                            "All History" -> {
                                usageLockerCount = combinedLockers.size
                                combinedLockers.filter {
                                    it.Username.contains(searchQuery, ignoreCase = true) || // ค้นหาจาก Username
                                            it.Lockername?.contains(searchQuery, ignoreCase = true) == true || // ค้นหาจาก Lockername
                                            it.Usage.contains(searchQuery, ignoreCase = true) || // ค้นหาจาก Usage
                                            it.Equipment?.contains(searchQuery, ignoreCase = true) == true || // ค้นหาจาก Equipment
                                            it.Compartmentnumber.toString().contains(searchQuery, ignoreCase = true) // ค้นหาจาก Compartmentnumber
                                }
                            }

                            "Manage Locker" -> {

                                val filteredLocker = filteredUsageLockersTransformed.filter { usageLocker ->
                                    (usageLocker.Usage == "Edit Compartment" || usageLocker.Usage == "Create Compartment" || usageLocker.Usage == "Delete Compartment")}
                                val combinedLockers = manageLockersTransformed + filteredLocker
                                val filtercombinedLockers =combinedLockers.filter {
                                    it.Username.contains(searchQuery, ignoreCase = true) || // ค้นหาจาก Username
                                            it.Lockername?.contains(searchQuery, ignoreCase = true) == true || // ค้นหาจาก Lockername
                                            it.Usage.contains(searchQuery, ignoreCase = true) || // ค้นหาจาก Usage
                                            it.Equipment?.contains(searchQuery, ignoreCase = true) == true || // ค้นหาจาก Equipment
                                            it.Compartmentnumber.toString().contains(searchQuery, ignoreCase = true) // ค้นหาจาก Compartmentnumber
                                }
                                usageLockerCount = filtercombinedLockers.size
                                filtercombinedLockers
                            }
                            "Manage User" -> {
                                usageLockerCount = manageAccountsTransformed.size
                                manageAccountsTransformed.filter {
                                    it.Username.contains(searchQuery, ignoreCase = true) || // ค้นหาจาก Username
                                            it.Lockername?.contains(searchQuery, ignoreCase = true) == true || // ค้นหาจาก Lockername
                                            it.Usage.contains(searchQuery, ignoreCase = true) || // ค้นหาจาก Usage
                                            it.Equipment?.contains(searchQuery, ignoreCase = true) == true || // ค้นหาจาก Equipment
                                            it.Compartmentnumber.toString().contains(searchQuery, ignoreCase = true) // ค้นหาจาก Compartmentnumber
                                }
                            }
                            "Usage Locker" -> {
                                val filteredLocker = filteredUsageLockersTransformed.filter { usageLocker ->
                                    (usageLocker.Usage == "borrow" || usageLocker.Usage == "return") &&
                                            (usageLocker.Username.contains(searchQuery, ignoreCase = true) ||
                                                    usageLocker.Lockername?.contains(searchQuery, ignoreCase = true) == true || // ค้นหาจาก Lockername
                                                    usageLocker.Usage.contains(searchQuery, ignoreCase = true) || // ค้นหาจาก Usage
                                                    usageLocker.Equipment?.contains(searchQuery, ignoreCase = true) == true || // ค้นหาจาก Equipment
                                                    usageLocker.Compartmentnumber.toString().contains(searchQuery, ignoreCase = true))
                                }
                                usageLockerCount = filteredLocker.size
                                filteredLocker
                            }
                            else -> combinedLockers.filter {
                                it.Username.contains(searchQuery, ignoreCase = true) ||
                                        it.Lockername?.contains(searchQuery, ignoreCase = true) == true ||
                                        it.Usage.contains(searchQuery, ignoreCase = true)
                            }
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 26.dp, end = 10.dp)
                        ) {
                            items(filteredCombinedLockers) { filteredCombinedLockers ->
                                // Get account names outside of Composable using observeAsState()



                                // Use rememberUpdatedState to update the namelocker state only when LockerID changes





                                // Format date
                                val splitDateTime = formatTimestamp(filteredCombinedLockers.UsageTime).split(" ")

                                // Row with various columns
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                ) {
                                    // Column for account name
                                    Column(modifier = Modifier.width(100.dp)) {
                                        Text(
                                            text = filteredCombinedLockers.Username,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    // Column for locker name or edited account name
                                    Column(modifier = Modifier.width(100.dp)) {
                                        Text(
                                            text = if (filteredCombinedLockers.Lockername != null) {
                                                filteredCombinedLockers.Lockername ?: ""  // Display "Unknown Locker" if namelocker is null
                                            } else {
                                                filteredCombinedLockers.User_Edited?:""  // Display ByaccountNameUsageLocker if LockerID is null
                                            },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    // Column for compartment number
                                    Column(modifier = Modifier.width(50.dp)) {
                                        Text(
                                            text = filteredCombinedLockers.Compartmentnumber?.toString() ?: "  _",
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    // Column for item name
                                    Column(modifier = Modifier.width(130.dp)) {
                                        Text(
                                            text = filteredCombinedLockers.Equipment ?: "  _",
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    // Column for formatted usage date and time
                                    Column(modifier = Modifier.width(100.dp)) {
                                        Row {
                                            Text(
                                                text = formatDateHistory(splitDateTime[1]),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                        Row {
                                            Text(
                                                text = "At ${splitDateTime[0]}",
                                                maxLines = 1,
                                                fontSize = 12.sp,
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }

                                    // Column for usage status
                                    Column(modifier = Modifier.width(130.dp)) {
                                        Text(
                                            text = filteredCombinedLockers.Usage,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center,
                                            color = when (filteredCombinedLockers.Status.lowercase()) {
                                                "fail" -> Color.Red
                                                else -> Color.Black // Default color if not "fail"
                                            },
                                            fontWeight = when (filteredCombinedLockers.Status.lowercase()) {
                                                "fail" -> FontWeight.Bold
                                                else -> FontWeight.Normal // Default font weight
                                            }
                                        )
                                    }
                                }

                                // Divider between items
                                Divider(color = Color(0xFFE8E8E8), thickness = 1.dp)
                            }
                        }

                    }

                }


            }

        }
    }
}


