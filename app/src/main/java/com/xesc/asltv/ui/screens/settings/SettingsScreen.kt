package com.xesc.asltv.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.xesc.asltv.ui.components.*
import com.xesc.asltv.ui.themes.*

@Composable
fun SettingsScreen(navController: NavHostController, isTV: Boolean) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("Configuració", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("Gestiona llistes, preferències i actualitzacions", fontSize = 13.sp, color = TextMuted)

        SectionTitle("LLISTES DE CANALS")
        if (uiState.savedLists.isEmpty()) {
            EmptyListPlaceholder()
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                uiState.savedLists.forEach { list ->
                    ListItem(
                        name = list.name, author = list.author, isActive = list.isActive,
                        lastUpdated = list.lastUpdated,
                        onSelect = { viewModel.setActiveList(list.userUrl) },
                        onDelete = { viewModel.deleteList(list.userUrl) }
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        PrimaryButton("Afegeix una llista nova", onClick = viewModel::showAddListDialog, icon = Icons.Default.Add)

        SectionTitle("TIPUS DE LLISTA")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ListModeCard("Completa", "Tots els canals disponibles", Icons.Default.GridView,
                !uiState.isLiteMode, { viewModel.setLiteMode(false) }, Modifier.weight(1f))
            ListModeCard("Bàsica", "Només canals marcats com a bàsics", Icons.Default.FilterList,
                uiState.isLiteMode, { viewModel.setLiteMode(true) }, Modifier.weight(1f))
        }

        SectionTitle("ACTUALIZACIÓ")
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryButton(
                if (uiState.isRefreshing) "Actualitzant…" else "Actualitzar llista",
                onClick = viewModel::refreshActiveList,
                icon = Icons.Default.Refresh,
                enabled = !uiState.isRefreshing,
                modifier = Modifier.weight(1f)
            )
            PrimaryButton(
                if (uiState.isCheckingUpdates) "Comprovant…" else "Cerca actualitzacions",
                onClick = viewModel::manualCheckForAppUpdate,
                icon = Icons.Default.SystemUpdate,
                enabled = !uiState.isCheckingUpdates,
                modifier = Modifier.weight(1f)
            )
            if (uiState.isRefreshing || uiState.isCheckingUpdates) {
                CircularProgressIndicator(Modifier.size(24.dp), color = AccentGold, strokeWidth = 2.dp)
            }
        }
        uiState.lastUpdateText?.let {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                Icon(Icons.Default.CheckCircle, null, tint = LiveDot, modifier = Modifier.size(14.dp))
                Text(it, fontSize = 11.sp, color = TextMuted)
            }
        }

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, DividerColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("ASLTV Versió 1.0.0", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (uiState.showAddListDialog) {
        AddListDialog(onConfirm = viewModel::addList, onDismiss = viewModel::hideAddListDialog)
    }
    
    uiState.appUpdate?.let { appUpdate ->
        println("SettingsScreen: Mostrando AppUpdateDialog para versión ${appUpdate.version}")
        AppUpdateDialog(
            newVersion = appUpdate.version,
            description = appUpdate.description,
            onInstall = {
                println("SettingsScreen: Usuario quiere instalar versión ${appUpdate.version}")
                viewModel.installUpdate(appUpdate)
            },
            onDismiss = {
                println("SettingsScreen: Usuario descartó actualización")
                viewModel.dismissAppUpdate()
            }
        )
    }

    uiState.appUpdateFile?.let { file ->
        LaunchedEffect(file) {
            com.xesc.asltv.utils.ApkInstaller.installApk(context, file)
        }
    }

    if (uiState.updateDownloadProgress != null) {
        AlertDialog(
            onDismissRequest = { },
            containerColor = DialogBg,
            title = { Text("Baixant actualització", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LinearProgressIndicator(
                        progress = { uiState.updateDownloadProgress ?: 0f },
                        modifier = Modifier.fillMaxWidth(),
                        color = AccentGold
                    )
                    Text("${((uiState.updateDownloadProgress ?: 0f) * 100).toInt()}%", color = TextSecondary)
                }
            },
            confirmButton = { }
        )
    }

}

@Composable
private fun EmptyListPlaceholder() {
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceCard).padding(28.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.PlaylistAdd, null, tint = TextMuted, modifier = Modifier.size(32.dp))
            Text("No hi ha llistes afegides", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
            Text("Afegeix una llista per començar a veure canals", fontSize = 12.sp, color = TextMuted)
        }
    }
}