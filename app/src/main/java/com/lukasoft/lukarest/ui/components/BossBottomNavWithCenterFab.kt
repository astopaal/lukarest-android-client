package com.lukasoft.lukarest.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.FloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lukasoft.lukarest.ui.theme.BossGradStart
import com.lukasoft.lukarest.ui.theme.BossGradient
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
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Navigasyon Bar Gövdesi
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
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
                LukaNavItem(item, currentRoute == item.route, onNavigate, BossGradStart)
            }

            // Ortadaki FAB için boşluk bırak
            Spacer(modifier = Modifier.width(64.dp))

            // Sağ İki Sekme (Görevler, Talepler)
            rightItems.forEach { item ->
                LukaNavItem(item, currentRoute == item.route, onNavigate, BossGradStart)
            }
        }

        // Ortada Öne Çıkan Büyük QR Butonu (Floating Center Button)
        FloatingActionButton(
            onClick = onCenterQrClick,
            shape = CircleShape,
            containerColor = Color.Transparent,
            contentColor = White,
            modifier = Modifier
                .offset(y = (-14).dp)
                .size(58.dp)
                .lukaFocusOutline(CircleShape)
                .shadow(
                    elevation = 10.dp,
                    shape = CircleShape,
                    ambientColor = Color(0x400B2956),
                    spotColor = Color(0x500B2956)
                )
                .clip(CircleShape)
                .background(BossGradient)
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
