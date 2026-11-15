package com.lukasoft.lukarest.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import com.lukasoft.lukarest.ui.theme.StaffGradStart
import com.lukasoft.lukarest.ui.theme.TextMuted
import com.lukasoft.lukarest.ui.theme.White
import com.lukasoft.lukarest.ui.theme.PillBlue

data class NavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
)

@Composable
fun LukaBottomNav(
    items: List<NavItem>,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = StaffGradStart
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
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
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                LukaNavItem(item, currentRoute == item.route, onNavigate, activeColor)
            }
        }
    }
}

@Composable
internal fun RowScope.LukaNavItem(
    item: NavItem,
    selected: Boolean,
    onNavigate: (String) -> Unit,
    activeColor: Color
) {
    NavigationBarItem(
        modifier = Modifier.lukaFocusOutline(),
        selected = selected,
        onClick = { onNavigate(item.route) },
        icon = { Icon(item.icon, contentDescription = null, modifier = Modifier.size(22.dp)) },
        label = { Text(item.title, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = activeColor,
            selectedTextColor = activeColor,
            unselectedIconColor = TextMuted,
            unselectedTextColor = TextMuted,
            indicatorColor = PillBlue.bg
        )
    )
}

