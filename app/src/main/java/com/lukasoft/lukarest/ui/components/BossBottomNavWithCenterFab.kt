package com.lukasoft.lukarest.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukasoft.lukarest.ui.theme.BossGradStart
import com.lukasoft.lukarest.ui.theme.BossGradient
import com.lukasoft.lukarest.ui.theme.TextMuted
import com.lukasoft.lukarest.ui.theme.White

@Composable
fun BossBottomNavWithCenterFab(
    leftItems: List<NavItem>,
    rightItems: List<NavItem>,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onCenterQrClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Navigasyon Bar Gövdesi
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(32.dp),
                    ambientColor = Color(0x180F1F38),
                    spotColor = Color(0x200F1F38)
                )
                .clip(RoundedCornerShape(32.dp))
                .background(White)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sol İki Sekme (Panel, Vardiyalar)
            leftItems.forEach { item ->
                NavTabItem(item = item, isSelected = currentRoute == item.route, onNavigate = onNavigate, modifier = Modifier.weight(1f))
            }

            // Ortadaki FAB için boşluk bırak
            Spacer(modifier = Modifier.width(64.dp))

            // Sağ İki Sekme (Görevler, Talepler)
            rightItems.forEach { item ->
                NavTabItem(item = item, isSelected = currentRoute == item.route, onNavigate = onNavigate, modifier = Modifier.weight(1f))
            }
        }

        // Ortada Öne Çıkan Büyük QR Butonu (Floating Center Button)
        Box(
            modifier = Modifier
                .offset(y = (-14).dp)
                .size(58.dp)
                .shadow(
                    elevation = 10.dp,
                    shape = CircleShape,
                    ambientColor = Color(0x400B2956),
                    spotColor = Color(0x500B2956)
                )
                .clip(CircleShape)
                .background(BossGradient)
                .clickable { onCenterQrClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = "QR Okut",
                tint = White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun NavTabItem(
    item: NavItem,
    isSelected: Boolean,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onNavigate(item.route) }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.title,
            tint = if (isSelected) BossGradStart else TextMuted,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = item.title,
            color = if (isSelected) BossGradStart else TextMuted,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

