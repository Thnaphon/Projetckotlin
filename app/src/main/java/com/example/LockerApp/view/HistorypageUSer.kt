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
import androidx.compose.runtime.rememberUpdatedState
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
import com.example.LockerApp.model.Compartment
import com.example.LockerApp.viewmodel.AccountViewModel
import com.example.LockerApp.viewmodel.LockerViewModel
import com.example.LockerApp.viewmodel.ManageAccountViewModel
import com.example.LockerApp.viewmodel.UsageLockerViewModel
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import kotlin.math.log

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun UsageHistoryScreenUser(accountViewModel: AccountViewModel, usageLockerViewModel: UsageLockerViewModel, navController: NavController,viewModel: LockerViewModel,accountid:Int) {
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
                        it.number_compartment.toString().contains(searchQuery, ignoreCase = true)) && (it.Usage == "borrow" || it.Usage == "return")

    }


    var usageLockerCount by remember { mutableStateOf(0) }

    usageLockerCount = filteredUsageLockers.size

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
                    HeaderRowuser()
                    Row (horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically)
                    {



                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 26.dp, end = 10.dp)
                        ) {
                            items(filteredUsageLockers) { filteredUsageLockers ->
                                // Get account names outside of Composable using observeAsState()


                                // Use rememberUpdatedState to update the namelocker state only when LockerID changes


                                // Handle compartment list updates using LaunchedEffect
                                var compartmentList by remember { mutableStateOf(emptyList<Compartment>()) }


                                // Format date
                                val splitDateTime = formatTimestamp(filteredUsageLockers.UsageTime).split(" ")

                                // Row with various columns
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                ) {


                                    // Column for locker name or edited account name
                                    Column(modifier = Modifier.width(100.dp)) {
                                        Text(
                                            text = filteredUsageLockers.locker_name ?: "",
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    // Column for compartment number
                                    Column(modifier = Modifier.width(50.dp)) {
                                        Text(
                                            text = compartmentList.firstOrNull()?.number_compartment?.toString() ?: "_",
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    // Column for item name
                                    Column(modifier = Modifier.width(130.dp)) {
                                        Text(
                                            text = compartmentList.firstOrNull()?.Name_Item?.toString() ?: "_",
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
                                            text = filteredUsageLockers.Usage,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                    Column(modifier = Modifier.width(130.dp)) {
                                        Text(
                                            text = filteredUsageLockers.Status,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center,
                                            color = when (filteredUsageLockers.Status.lowercase()) {
                                                "fail" -> Color.Red
                                                else -> Color.Black // Default color if not "fail"
                                            },
                                            fontWeight = when (filteredUsageLockers.Status.lowercase()) {
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
@Composable
fun HeaderRowuser() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(Color(0xFFEEEEEE))
            .padding(start = 26.dp, end = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Column(modifier = Modifier.width(100.dp)) {
            Text(
                "Locker",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
        }
        Column(modifier = Modifier.width(45.dp)) {
            Text(
                "Com.",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
        }
        Column(modifier = Modifier.width(130.dp)) {
            Text(
                "Equipment",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
        }
        Column(modifier = Modifier.width(100.dp)) {
            Text(
                "Date/Time",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
        }
        Column(modifier = Modifier.width(130.dp)) {
            Text(
                "Usage",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
        }
        Column(modifier = Modifier.width(130.dp)) {
            Text(
                "Status",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
        }
    }

}

