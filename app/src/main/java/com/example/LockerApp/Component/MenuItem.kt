package com.example.LockerApp.Component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun MenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    iconTint: Color = Color.White, // สีของไอคอน
    iconSize: Dp = 24.dp, // ขนาดของไอคอน
    backgroundColor: Color = Color.LightGray,
    selected: Boolean// สีพื้นหลังของไอคอน
) {
    val bgColor = if (selected) Color(0xFF3A4750) else Color.Transparent
    val textColor = if (selected) Color.White else Color.Black
    Row(
        modifier = Modifier
            .fillMaxWidth()

            .clickable(onClick = onClick)
            .background(bgColor, shape = RoundedCornerShape(15.dp))

            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(iconSize * 1.65f) // ให้ขนาด Box ใหญ่กว่าไอคอนนิดหน่อย
                .background(
                    backgroundColor,
                    shape = RoundedCornerShape(10.dp)
                ) // กำหนดสีพื้นหลังและขอบมน
                .padding(8.dp), // ให้มีระยะห่างระหว่างขอบกับไอคอน
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = iconTint, // กำหนดสีของไอคอน
                modifier = Modifier.size(iconSize) // กำหนดขนาดของไอคอน
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, color = textColor)
    }
}


