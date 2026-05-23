package com.xesc.asltv.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.xesc.asltv.ui.navigation.Screen
import com.xesc.asltv.ui.themes.AccentBlue
import com.xesc.asltv.ui.themes.AccentPurple
import com.xesc.asltv.ui.themes.DividerColor
import com.xesc.asltv.ui.themes.NavBg
import com.xesc.asltv.ui.themes.TextMuted

@Composable
fun TopNav(navController: NavController, isTV: Boolean) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val navItems = listOf(
        Triple(Screen.Channels, Icons.Default.Tv,       "CANALES"),
        Triple(Screen.Apks,     Icons.Default.Download, "APLICACIONES"),
        Triple(Screen.Settings, Icons.Default.Settings, "AJUSTES")
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(NavBg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── Logo / marca ──────────────────────────────────────────────────────
        Row(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Brush.linearGradient(listOf(AccentBlue, AccentPurple))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayCircle, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Text(
                text = "ACESTREAM",
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 1.5.sp
            )
            Text(
                text = "TV",
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = AccentBlue,
                letterSpacing = 1.5.sp
            )
        }

        // Separador
        Box(modifier = Modifier.width(1.dp).height(24.dp).background(DividerColor))

        // ── Items de navegación ───────────────────────────────────────────────
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            navItems.forEach { (screen, icon, label) ->
                val isSelected = currentRoute == screen.route
                TopNavItem(
                    label = label,
                    icon = icon,
                    isSelected = isSelected,
                    onClick = {
                        if (!isSelected) navController.navigate(screen.route) {
                            popUpTo(Screen.Channels.route)
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    }

    // Línea inferior del nav
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DividerColor))
}

@Composable
private fun TopNavItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else TextMuted,
        animationSpec = tween(200),
        label = "nav_color"
    )

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor,
            letterSpacing = 0.8.sp
        )

        // Indicador activo (línea inferior)
        Box(
            modifier = Modifier
                .width(if (isSelected) 24.dp else 0.dp)
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(if (isSelected) AccentBlue else Color.Transparent)
        )
    }
}