import android.service.autofill.UserData
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.LockerApp.model.Account
import com.example.LockerApp.model.AccountDao
import com.example.LockerApp.viewmodel.AccountViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun ParticipantScreen(accountViewModel: AccountViewModel,navController: NavController) {
    var isEditDialogVisible by remember { mutableStateOf(false) }
    var isEditMode by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var accountIdToEdit by remember { mutableStateOf<Long?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    val userDetails by accountViewModel.userDetails.observeAsState(emptyList())
    val filteredUsers = userDetails.filter {
        it.Name.contains(searchQuery, ignoreCase = true) ||
                it.Role.contains(searchQuery, ignoreCase = true) ||
                it.Phone.contains(searchQuery, ignoreCase = true)
    }
    val userCount = filteredUsers.size

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp)
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
                    text = "Participant", // ข้อความที่ต้องการแสดง
                    fontSize = 24.sp, // ขนาดตัวอักษร
                    fontWeight = FontWeight.Bold, // ทำให้ตัวอักษรหนา
                    color = Color.Black // สีข้อความ
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
                        imageVector = Icons.Default.Person,
                        contentDescription = "User Icon",
                        tint = Color(0xFF6200EA),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$userCount users",
                        fontSize = 20.sp
                    )
                }

                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search") },
                    modifier = Modifier
                        .width(200.dp)
                        .padding(8.dp)
                )

                IconButton(
                    onClick = {
                        name = ""
                        role = ""
                        phone = ""
                        isEditDialogVisible = true
                        isEditMode = false
                        accountIdToEdit = null
                    }
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add User",
                        tint = Color(0xFF6200EA)
                    )
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
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()

                    ) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFEEEEEE))
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(
                                    "Name",
                                    Modifier.weight(1f).padding(start = 16.dp),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    "Role",
                                    Modifier.weight(1f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    "Phone",
                                    Modifier.weight(1f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    "Created Date",
                                    Modifier.weight(1f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.weight(0.5f))
                                Spacer(modifier = Modifier.weight(0.5f))
                            }
                        }

                        items(filteredUsers) { user ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(user.Name, Modifier.weight(1f).padding(start = 16.dp))
                                Text(user.Role, Modifier.weight(1f))
                                Text(user.Phone, Modifier.weight(1f))
                                Text(user.CreatedDate, Modifier.weight(1f))
                                IconButton(onClick = {
                                    name = user.Name
                                    role = user.Role
                                    phone = user.Phone
                                    accountIdToEdit = user.AccountID
                                    isEditDialogVisible = true
                                    isEditMode = true
                                }, modifier = Modifier.weight(0.5f)) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        tint = Color.Blue
                                    )
                                }
                                IconButton(onClick = {
                                    accountViewModel.deleteAccount(user)
                                }, modifier = Modifier.weight(0.5f)) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = Color.Red
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ฟอร์มการกรอกข้อมูลที่อยู่ด้านล่าง
        if (isEditDialogVisible) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2A3D4F))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(
                            onClick = {navController.navigate("face_detection")},
                            modifier = Modifier.align(Alignment.CenterVertically) // ทำให้ปุ่มอยู่ในแนวตั้งกลาง
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star, // ใช้ไอคอนกล้อง
                                contentDescription = "Capture Photo",
                                tint = Color.White
                            )
                        }

                        // ช่องกรอกชื่อ
                        TextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Name", color = Color.White) },
                            textStyle = TextStyle(color = Color.White),
                            modifier = Modifier.weight(1f) // กำหนดให้ TextField ขยายให้เต็มพื้นที่ที่เหลือ
                        )

                        // ช่องกรอกตำแหน่ง
                        TextField(
                            value = role,
                            onValueChange = { role = it },
                            label = { Text("Role", color = Color.White) },
                            textStyle = TextStyle(color = Color.White),
                            modifier = Modifier.weight(1f)
                        )

                        // ช่องกรอกหมายเลขโทรศัพท์
                        TextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Phone", color = Color.White) },
                            textStyle = TextStyle(color = Color.White),
                            modifier = Modifier.weight(1f)
                        )
                    }

                }

                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(onClick = {
                                isEditDialogVisible = false
                            }, modifier = Modifier.padding(top = 16.dp)) {
                                Text("Cancel", color = Color.White)
                            }

                            TextButton(
                                onClick = {
                                    if (isEditMode && accountIdToEdit != null) {
                                        val embeddingString = ","
                                        val updatedUser = Account(
                                            AccountID = accountIdToEdit!!,
                                            Name = name,
                                            Phone = phone,
                                            Role = role,
                                            embedding = embeddingString,
                                            CreatedDate = userDetails.firstOrNull { it.AccountID == accountIdToEdit }?.CreatedDate
                                                ?: currentDate
                                        )
                                        accountViewModel.updateAccount(updatedUser)
                                    } else {
                                        val embeddingString = ","
                                        val newUser = Account(
                                            Name = name,
                                            Phone = phone,
                                            Role = role,
                                            embedding = embeddingString,
                                            CreatedDate = currentDate
                                        )
                                        accountViewModel.insertAccount(newUser)
                                    }
                                    isEditDialogVisible = false
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
        }

