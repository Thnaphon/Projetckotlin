package com.example.LockerApp.view


import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.MenuDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.LockerApp.model.Account
import com.example.LockerApp.model.ManageAccount
import com.example.LockerApp.viewmodel.AccountViewModel
import com.example.LockerApp.viewmodel.FaceLoginViewModel
import com.example.LockerApp.viewmodel.LockerViewModel
import com.example.LockerApp.viewmodel.ManageAccountViewModel
import com.example.LockerApp.viewmodel.MqttViewModel
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale


@Composable
fun ParticipantScreen(
    accountViewModel: AccountViewModel,
    navController: NavController,
    faceLoginViewModel: FaceLoginViewModel,
    accountid: Int,
    adminname: String,
    adminrole: String,
    viewModel: LockerViewModel
) {
    Log.d("value","$adminname , $adminrole")
    var isAddDialogVisible by remember { mutableStateOf(false) }
    var isEditDialogVisible by remember { mutableStateOf(false) }
    var isEditMode by remember { mutableStateOf(false) }
    var isPdpDialogVisible by remember { mutableStateOf(false) }
    var isAdminVerificationDialogVisible by remember { mutableStateOf(false) }

    val manageAccountViewModel: ManageAccountViewModel = viewModel()
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var accountIdToEdit by remember { mutableStateOf<Int?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    var showFaceVerification by remember { mutableStateOf(false) } // Add this state variable
    var selectedRole by remember { mutableStateOf("All Users") }

    val currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    val userDetails by accountViewModel.userDetails.observeAsState(emptyList())
    val filteredUsers = userDetails.filter {
        (selectedRole == "All Users" || it.Role == selectedRole) && // ฟิลเตอร์ Role
                (it.Name.contains(searchQuery, ignoreCase = true) ||
                        it.Role.contains(searchQuery, ignoreCase = true) ||
                        it.Phone.contains(searchQuery, ignoreCase = true))
    }
    val userCount = filteredUsers.size


    // Reset the face login state when entering the screen
    LaunchedEffect(Unit) {
        faceLoginViewModel.refreshFaceData()
        faceLoginViewModel.resetToScanning()
    }

    if (isAdminVerificationDialogVisible) {
        Dialog(
            onDismissRequest = { isAdminVerificationDialogVisible = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            AdminVerificationPage(
                navController = navController,
                adminAccountId = accountid,
                adminname = adminname,
                adminrole = adminrole,
                name = name,
                role = role,
                phone = phone
            )
        }

    }


    // Show face verification overlay if needed
    if (showFaceVerification) {
        FaceVerificationOverlay(
            navController = navController,
            viewModel = faceLoginViewModel,
            expectedAccountId = accountid,
            adminname = adminname,
            adminrole = adminrole,
            name = name,
            role = role,
            phone = phone,
            onDismiss = {
                showFaceVerification = false
            },
            onVerificationSuccess = {
                showFaceVerification = false  // The navigation to face_capture will be handled by the overlay
            },
            onVerificationFailed = {
                showFaceVerification = false  // Just close the overlay on failure
            }
        )
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
            // เว้นพื้นที่สำหรับฟอร์มกรอกข้อมูลด้านล่าง
        ) {
            // Row สำหรับ meta data
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.Center // ทำให้ข้อความอยู่กลาง
            ) {
                Text(
                    text = "Participant",
                    style = MaterialTheme.typography.h5.copy(fontWeight = FontWeight.Bold),
                    color = Color.Black
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Group,
                        contentDescription = "User Icon",
                        tint = Color.Black,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$userCount users",
                        style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.Black
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search, // ใช้ไอคอนค้นหา
                            contentDescription = "Search Icon"
                        )
                    },
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
                        .height(56.dp)
                        .border(2.dp, Color(0xFF8D8B8B), RoundedCornerShape(25))
                )
                Spacer(modifier = Modifier.width(10.dp))
                DropdownUser(selectedRole = selectedRole, onRoleChange = { selectedRole = it })
                Spacer(modifier = Modifier.width(10.dp))
                androidx.compose.material.Card(
                    modifier = Modifier
                        .width(56.dp)
                        .height(56.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .border(
                            2.dp,
                            Color(0xFF3961AA),
                            RoundedCornerShape(15.dp)
                        ), // มุมมนของการ์ด
                    elevation = 8.dp, // ความสูงของเงา
                    backgroundColor = Color.White // พื้นหลังสีขาวของการ์ด
                ) {
                    IconButton(
                        onClick = {
                            if (!isEditDialogVisible) {
                                name = ""
                                role = ""
                                phone = ""
                                isAddDialogVisible = true
                                isEditMode = false
                                accountIdToEdit = null
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize() // ขยายขนาดให้เต็มการ์ด
                            .padding(4.dp) // เพิ่ม padding รอบๆ IconButton
                        ,
                        enabled = !isEditDialogVisible

                    ) {
                        Icon(
                            Icons.Outlined.PersonAdd,
                            contentDescription = "Add User",
                            tint = Color(0xFF3961AA)
                        )
                    }
                }
            }

            // Row ที่สอง สำหรับ LazyColumn ข้อมูล
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
                        modifier = Modifier
                            .fillMaxWidth()

                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFEEEEEE))
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                        ) {

                            Box(
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(2.dp)
                                    .background(Color.Transparent, shape = CircleShape)
                                    .padding(6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Person,
                                    contentDescription = "user",
                                    tint = Color.Transparent
                                )
                            }
                            Spacer(modifier = Modifier.weight(0.33f))
                            Text(
                                "Name",
                                Modifier
                                    .weight(1.13f)
                                    .padding(start = 16.dp)
                                    ,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                "Role",
                                Modifier.weight(0.8f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                "Phone",
                                Modifier.weight(0.9f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                "Created Date",
                                Modifier.weight(0.9f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.weight(0.3f))
                            Spacer(modifier = Modifier.weight(0.3f))
                        }

                        AddUserDialog(
                            isVisible = isAddDialogVisible,
                            onDismiss = { isAddDialogVisible = false },
                            onStartFaceRecognition = { isPdpDialogVisible = true },
                            name = name,
                            onNameChange = { name = it },
                            role = role,
                            onRoleChange = { role = it },
                            phone = phone,
                            onPhoneChange = { phone = it }
                        )
                        EditAccountDialog(
                            isVisible = isEditDialogVisible,
                            name = name,
                            onNameChange = { name = it },
                            phone = phone,
                            onPhoneChange = { phone = it },
                            role = role,
                            onRoleChange = { role = it },
                            onCancel = { isEditDialogVisible = false },
                            onApply = {
                                val usageTime = System.currentTimeMillis().toString()
                                accountIdToEdit?.let {
                                    accountViewModel.updateAccountFields(it, name, phone, role)
                                    val ManageAccount = ManageAccount(name_user = name,actoin_username = adminname,UsageTime=usageTime,Usage = "Edit Account" )
                                    manageAccountViewModel.insertManageAccount(ManageAccount)

                                }
                                isEditDialogVisible = false
                            }
                        )

                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, end = 8.dp)

                    ) {
                        items(filteredUsers) { user ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)

                            ) {
                                Spacer(modifier = Modifier.weight(0.16f))
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(Color.Gray, shape = CircleShape)
                                        .padding(6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.Person,
                                        contentDescription = "user",
                                        tint = Color.White,
                                        modifier = Modifier.size(50.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.weight(0.17f))
                                Text(
                                    user.Name,
                                    Modifier
                                        .weight(1.1f)
                                        .padding(start = 16.dp)
                                )
                                Spacer(modifier = Modifier.weight(0.1f))
                                Text(user.Role, Modifier.weight(0.8f))
                                Spacer(modifier = Modifier.weight(0.1f))
                                Text(user.Phone, Modifier.weight(0.9f))
                                Spacer(modifier = Modifier.weight(0.1f))
                                Text(formatDate(user.CreatedDate), Modifier.weight(1f))


                                IconButton(
                                    onClick = {
                                        if (!isAddDialogVisible && !isEditDialogVisible) {
                                            name = user.Name
                                            role = user.Role
                                            phone = user.Phone
                                            accountIdToEdit = user.AccountID
                                            isEditDialogVisible = true
                                            isEditMode = true
                                        }
                                    },
                                    modifier = Modifier.weight(0.3f),
                                    enabled = !isAddDialogVisible && !isEditDialogVisible
                                )
                                {
                                    Icon(
                                        Icons.Outlined.Edit,
                                        contentDescription = "Edit",
                                        tint = Color.Black
                                    )

                                }
                                IconButton(
                                    onClick = {
                                        if (!isAddDialogVisible && !isEditDialogVisible) {
                                            val usageTime = System.currentTimeMillis().toString()
                                            val ManageAccount = ManageAccount(name_user = user.Name,actoin_username = adminname,UsageTime=usageTime,Usage = "Delete Account Account" )
                                            manageAccountViewModel.insertManageAccount(ManageAccount)
                                            accountViewModel.deleteAccount(user)
                                        }
                                    },
                                    modifier = Modifier.weight(0.3f),
                                    enabled = !isAddDialogVisible && !isEditDialogVisible
                                ) {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        contentDescription = "Delete",
                                        tint = Color.Black
                                    )
                                }


                            }
                            Divider(color = Color(0xFFE8E8E8), thickness = 1.dp)
                        }

                    }
                }
            }
        }



        if (isPdpDialogVisible) {
            PdpDialog(
                onConfirm = {
                    isPdpDialogVisible = false
                    if (accountid == 1) {
                        // Master Password admin account - use password verification
                        isAdminVerificationDialogVisible = true
                    } else {
                        // Normal user account - show face verification overlay
                        showFaceVerification = true
                    }
                    isEditDialogVisible = false // Close the dialog
                },
                onCancel = {
                    isPdpDialogVisible = false
                }
            )
        }
    }
}

fun isValidPhoneNumber(phone: String): Boolean {
    val phoneRegex = "^[0-9]{10}$"
    return phone.matches(phoneRegex.toRegex())
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleDropdown(
    role: String,
    onRoleChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    val roles = listOf("Owner", "User")

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        TextField(
            value = role,
            onValueChange = {}, // Read-only
            readOnly = true,
            label = { Text("Role", color = Color.White) },
            textStyle = TextStyle(color = Color.White),
            trailingIcon = {
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = "Dropdown Icon",
                    tint = Color.White
                )
            },
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = Color.Transparent,
                focusedIndicatorColor = if (isError) Color.Red else Color.White,
                unfocusedIndicatorColor = if (isError) Color.Red else Color.White
            ),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(Color(0xFF2A3D4F))
            // ขยับ dropdown ลงมา 10dp
        ) {
            roles.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item, color = Color.White) }, // เปลี่ยนสีตัวอักษร
                    onClick = {
                        onRoleChange(item)
                        expanded = false
                    },
                    colors = MenuDefaults.itemColors( // เปลี่ยนสี background และ foreground
                        textColor = Color(0xFF2A3D4F),
                        leadingIconColor = Color(0xFF2A3D4F)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2A3D4F)) // เปลี่ยนสีพื้นหลังของ item
                        .clip(RoundedCornerShape(0.dp))
                )
            }
        }
    }
}


@Composable
fun PdpDialog(onConfirm: () -> Unit, onCancel: () -> Unit) {
    // Track if user has scrolled to bottom
    var canAccept by remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()

    // Only check scroll position when necessary
    LaunchedEffect(lazyListState.layoutInfo.visibleItemsInfo.size, lazyListState.firstVisibleItemIndex) {
        val lastVisibleItem = lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()
        val totalItems = lazyListState.layoutInfo.totalItemsCount

        // Check if last item is visible
        if (lastVisibleItem != null && lastVisibleItem.index >= totalItems - 1) {
            canAccept = true
        }
    }

    // Custom dialog implementation instead of AlertDialog
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color.White,
            modifier = Modifier
                .padding(vertical = 115.dp)
                .padding(horizontal = 416.dp)
//                .width(600.dp)
//                .height(400.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Fixed title area
                Text(
                    text = "Privacy Policy",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 25.dp)
                        .fillMaxWidth()
                )

                // Scrollable content area with fixed height
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    // Scrollable content
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp)
                            .background(Color(0xFFEEEEEE))
                    ) {
                        item {
                            Text(
                                text = "The Face Authentication Locker for Equipment Borrowing application prioritizes user privacy in accordance with the Personal Data Protection Act B.E. 2562 (PDPA).",
                                color = Color.Black,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        item { Spacer(modifier = Modifier.height(16.dp)) }

                        item {
                            Text(
                                text = "1. Data We Collect",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        item {
                            Text(
                                text = "When you use our application, we may collect your personal data, including:",
                                color = Color.Black
                            )
                        }

                        item { BulletPoint("Name: Used for identification purposes") }
                        item { BulletPoint("Phone number: Used for contact and identity verification") }
                        item { BulletPoint("Facial data: Used for facial recognition and identity verification features") }

                        item { Spacer(modifier = Modifier.height(8.dp)) }

                        item {
                            Text(
                                text = "2. Purpose of Data Usage",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        item {
                            Text(
                                text = "We will use your data for the following purposes:",
                                color = Color.Black
                            )
                        }

                        item { BulletPoint("Providing application functionalities") }
                        item { BulletPoint("Identity verification and account security") }
                        item { BulletPoint("Contacting users regarding application usage") }

                        item { Spacer(modifier = Modifier.height(8.dp)) }

                        item {
                            Text(
                                text = "3. Data Retention",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        item {
                            Text(
                                text = "Your data will be securely stored and deleted when it is no longer necessary.",
                                color = Color.Black
                            )
                        }

                        item { Spacer(modifier = Modifier.height(8.dp)) }

                        item {
                            Text(
                                text = "4. User Rights",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        item {
                            Text(
                                text = "You have the right to:",
                                color = Color.Black
                            )
                        }

                        item { BulletPoint("Request access, modification, or deletion of your personal data") }
                        item { BulletPoint("Request to suspend or object to the processing of your data") }
                        item { BulletPoint("Withdraw consent for data collection and usage") }

                        item {
                            Text(
                                text = "If you wish to exercise your rights or have any questions about this policy, please contact the application provider.",
                                color = Color.Black,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        // Add extra padding at the bottom to ensure content can be fully scrolled
                        item { Spacer(modifier = Modifier.height(50.dp)) }
                    }

                    // Fixed overlay gradient with scroll indicator
                    if (!canAccept) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(60.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.White),
                                        startY = 0f,
                                        endY = 120f
                                    )
                                )
                        ) {
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = "Scroll down to continue",
                                modifier = Modifier.align(Alignment.Center),
                                tint = Color.Gray
                            )
                        }
                    }
                }

                // Fixed buttons area
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 20.dp, vertical = 25.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onCancel,
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = Color(0xFFFFFFFF),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = 1.dp, // ความหนาของขอบ
                                color = Color.Black, // สีของขอบ
                                shape = RoundedCornerShape(8.dp) // รูปร่างขอบให้โค้งมน
                            )
                    ) {
                        Text("Cancel", color = Color.Black)
                    }

                    Spacer(modifier = Modifier.width(10.dp))
                    TextButton(
                        onClick = {
                            onConfirm()
                            onCancel()
                        },
                        enabled = canAccept,
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = if (canAccept) Color(0xFF3961AA) else Color.Gray,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = 1.dp, // ความหนาของขอบ
                                color = Color(0xFF3961AA), // สีของขอบ
                                shape = RoundedCornerShape(8.dp) // รูปร่างขอบให้โค้งมน
                            )
                    ){
                        Text("Accept", color = Color.White)
                    }
                }
            }

        }
    }
}

@Composable
private fun BulletPoint(text: String) {
    Row(
        modifier = Modifier.padding(start = 8.dp, top = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            color = Color.Black,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(end = 8.dp, top = 0.dp)
        )
        Text(
            text = text,
            color = Color.Black
        )
    }
}

@Composable
fun AddUserDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onStartFaceRecognition: () -> Unit,
    name: String,
    onNameChange: (String) -> Unit,
    role: String,
    onRoleChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit
) {
    var validationAttempted by remember { mutableStateOf(false) }
    val isNameEmpty = name.isEmpty()
    val isRoleEmpty = role.isEmpty()
    val isPhoneEmpty = phone.isEmpty()
    val isPhoneValid = isValidPhoneNumber(phone)
    val isFormNull = !isNameEmpty && !isRoleEmpty && !isPhoneEmpty

    if (isVisible) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 0.dp),
            shape = RoundedCornerShape(bottomEnd = 16.dp, bottomStart = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A3D4F))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
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

                    TextField(
                        value = name,
                        onValueChange = onNameChange,
                        label = { Text("Name", color = Color.White) },
                        textStyle = TextStyle(color = Color.White),
                        modifier = Modifier
                            .weight(1f)
                            .background(Color.Transparent),
                        colors = TextFieldDefaults.textFieldColors(
                            backgroundColor = Color.Transparent,
                            focusedIndicatorColor = if (validationAttempted && isNameEmpty) Color.Red else Color.White,
                            unfocusedIndicatorColor = if (validationAttempted && isNameEmpty) Color.Red else Color.White
                        )
                    )

                    RoleDropdown(
                        role = role,
                        onRoleChange = onRoleChange,
                        modifier = Modifier.weight(1f),
                        isError = validationAttempted && isRoleEmpty
                    )

                    TextField(
                        value = phone,
                        onValueChange = { newValue ->
                            if (newValue.length <= 10 && newValue.all { it.isDigit() }) {
                                onPhoneChange(newValue)
                            }
                        },
                        label = { Text("Phone Number", color = Color.White) },
                        textStyle = TextStyle(color = Color.White),
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.textFieldColors(
                            backgroundColor = Color.Transparent,
                            focusedIndicatorColor = if (validationAttempted && (!isPhoneValid || isPhoneEmpty)) Color.Red else Color.White,
                            unfocusedIndicatorColor = if (validationAttempted && (!isPhoneValid || isPhoneEmpty)) Color.Red else Color.White
                        )
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.padding(top = 16.dp)) {
                        Text("Cancel", color = Color.White)
                    }

                    TextButton(
                        onClick = {
                            validationAttempted = true
                            if (isFormNull && isPhoneValid) {
                                onStartFaceRecognition()
                            }
                        },
                        modifier = Modifier
                            .padding(start = 8.dp, top = 16.dp)
                            .background(Color.White, shape = RoundedCornerShape(8.dp)),
//                        enabled = isFormNull
                    ) {
                        Text("Start Face Recognition", color = Color(0xFF2A3D4F))
                    }
                }
            }
        }
    }
}

@Composable
fun EditAccountDialog(
    isVisible: Boolean,
    name: String,
    onNameChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    role: String,
    onRoleChange: (String) -> Unit,
    onCancel: () -> Unit,
    onApply: () -> Unit
) {
    var validationAttempted by remember { mutableStateOf(false) }
    val isNameEmpty = name.isEmpty()
    val isRoleEmpty = role.isEmpty()
    val isPhoneEmpty = phone.isEmpty()
    val isPhoneValid = isValidPhoneNumber(phone)
    val isFormNull = !isNameEmpty && !isRoleEmpty && !isPhoneEmpty
    if (isVisible) {
        Card(
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(bottomEnd = 16.dp, bottomStart = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A3D4F))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextField(
                        value = name,
                        onValueChange = onNameChange,
                        label = { Text("Name", color = Color.White) },
                        textStyle = TextStyle(color = Color.White),
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        colors = TextFieldDefaults.textFieldColors(
                            backgroundColor = Color.Transparent,
                            focusedIndicatorColor = if (validationAttempted && isNameEmpty) Color.Red else Color.White,
                            unfocusedIndicatorColor = if (validationAttempted && isNameEmpty) Color.Red else Color.White
                        )
                    )

                    RoleDropdown(
                        role = role,
                        onRoleChange = onRoleChange,
                        modifier = Modifier.weight(1f),
                        isError = validationAttempted && isRoleEmpty
                    )

                    TextField(
                        value = phone,
                        onValueChange = onPhoneChange,
                        label = { Text("Phone", color = Color.White) },
                        textStyle = TextStyle(color = Color.White),
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        colors = TextFieldDefaults.textFieldColors(
                            backgroundColor = Color.Transparent,
                            focusedIndicatorColor = if (validationAttempted && (!isPhoneValid || isPhoneEmpty)) Color.Red else Color.White,
                            unfocusedIndicatorColor = if (validationAttempted && (!isPhoneValid || isPhoneEmpty)) Color.Red else Color.White
                        )
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(onClick = onCancel, modifier = Modifier.padding(top = 16.dp)) {
                    Text("Cancel", color = Color.White)
                }

                TextButton(
                    onClick = {
                        validationAttempted = true
                        if (isFormNull && isPhoneValid) {
                            onApply()
                        }
                    },
                    modifier = Modifier
                        .padding(start = 8.dp, top = 16.dp)
                        .background(Color.White, shape = RoundedCornerShape(8.dp))
                ) {
                    Text("Apply", color = Color(0xFF2A3D4F))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DropdownUser(
    selectedRole: String,
    onRoleChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val roles = listOf("All Users", "User", "Owner")
    Box( modifier = Modifier.width(130.dp),contentAlignment = Alignment.Center ) {
        androidx.compose.material.ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }) {
            Box(
                modifier = Modifier
                    .width(130.dp)
                    .height(56.dp)
                    .border(2.dp, Color(0xFF8D8B8B), RoundedCornerShape(15.dp))
                    .clickable { expanded = true }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.wrapContentWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedRole,
                        style = MaterialTheme.typography.body1
                    )
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Dropdown Icon"
                    )
                }
            }
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.width(130.dp)
            ) {
                roles.forEach { role ->
                    androidx.compose.material.DropdownMenuItem(onClick = {
                        onRoleChange(role)
                        expanded = false
                    }) {
                        Text(role)
                    }
                }
            }
        }
    }
}

fun formatDate(dateString: String): String {
    val possibleFormats = listOf(
        "yyyy-MM-dd" // ถ้าไม่มีเวลา
    )

    val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH) // ใช้ Locale.ENGLISH

    for (format in possibleFormats) {
        try {
            val inputFormat = SimpleDateFormat(format, Locale.getDefault())
            val date = inputFormat.parse(dateString)
            if (date != null) return outputFormat.format(date)
        } catch (_: Exception) {
        }
    }

    return dateString // คืนค่าเดิมถ้าแปลงไม่ได้
}
