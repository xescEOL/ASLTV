package com.xesc.asltv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.GetApp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.xesc.asltv.R
import com.xesc.asltv.ui.MainViewModel
import com.xesc.asltv.ui.navigation.Screen
import com.xesc.asltv.ui.themes.AccentGoldDim
import com.xesc.asltv.ui.themes.AccentGold
import com.xesc.asltv.ui.themes.BackgroundDark
import com.xesc.asltv.ui.themes.NavIconInactive

@Composable
fun SideNav(navController: NavHostController, isTV: Boolean) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    val viewModel: MainViewModel = hiltViewModel()
    val isSyncing by viewModel.isEpgSyncing.collectAsState()
    val progress by viewModel.epgSyncProgress.collectAsState()

    val items = listOf(
        NavigationItem("Canals", Screen.Channels.route, Icons.Default.Home),
        NavigationItem("Aplicacións", Screen.Apks.route, Icons.Default.GetApp),
        NavigationItem("Paràmetres", Screen.Settings.route, Icons.Default.Settings)
    )

    Column(
        modifier = Modifier
            .width(if (isTV) 130.dp else 180.dp)
            .fillMaxHeight()
            .background(BackgroundDark),
        horizontalAlignment = Alignment.Start
    ) {
        // ── Logo ──────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF111111), BackgroundDark)
                    )
                ),
            contentAlignment = Alignment.CenterStart
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Logo",
                modifier = Modifier.padding(horizontal = 20.dp).height(40.dp),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )
        }


        Spacer(Modifier.height(16.dp))

        // ── Items de navegación ───────────────────────────────────────────────
        items.forEach { item ->
            val selected = currentRoute == item.route
            NavigationIconButton(
                item = item,
                selected = selected,
                isTV = isTV,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }
                }
            )
        }

        Spacer(Modifier.weight(1f))

        // ── Cargando EPG ──────────────────────────────────────────────────────
        if (isSyncing) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Cargant la programació",
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp)),
                    color = Color(0xFFD4AF37),
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
            }
        }

        // ── Footer ────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "Aplicació desenvolupada per XESC",
                fontSize = 8.sp,
                color = NavIconInactive,
                lineHeight = 12.sp
            )
        }
    }
}

@Composable
fun NavigationIconButton(
    item: NavigationItem,
    selected: Boolean,
    isTV: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Barra indicadora lateral
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(42.dp)
                .background(
                    if (selected) Color(0xFFD4AF37) else Color.Transparent
                )
        )

        Spacer(Modifier.width(7.dp))

        Icon(
            imageVector = item.icon,
            contentDescription = item.title,
            tint = if (selected) Color(0xFFD4AF37) else NavIconInactive,
            modifier = Modifier.size(20.dp)
        )

        Spacer(Modifier.width(6.dp))

        Text(
            text = item.title,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Color(0xFFD4AF37) else NavIconInactive,
            maxLines = 1
        )
    }
}

data class NavigationItem(val title: String, val route: String, val icon: ImageVector)
