package com.xesc.asltv.ui.screens.apk

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.navigation.NavHostController
import com.xesc.asltv.ui.components.*
import com.xesc.asltv.ui.themes.*
import com.xesc.asltv.utils.ApkInstaller

@Composable
fun ApksScreen(navController: NavHostController, isTV: Boolean) {
    val viewModel: ApksViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Text("Aplicacions", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(
            "Descarrega i instal·la els APK necessaris per reproduir canals",
            fontSize = 13.sp, color = TextMuted,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier.padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SecondaryButton(
                text = "Desinstal·lar AceStream Apk",
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_DELETE).apply {
                            // Afegim el prefix package:
                            data = Uri.parse("package:org.acestream.node.web")
                            // Aquest flag és vital si el context no és l'activitat directament
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // Si falla, almenys veuràs el motiu al Logcat
                        android.util.Log.e("APK_SCREEN", "Error al desinstal·lar: ${e.message}")
                    }
                },
                icon = Icons.Default.Delete
            )
            SecondaryButton(
                text = "Desinstal·lar AceStream PlayStore",
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_DELETE).apply {
                            // Afegim el prefix package:
                            data = Uri.parse("package:org.acestream.node")
                            // Aquest flag és vital si el context no és l'activitat directament
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // Si falla, almenys veuràs el motiu al Logcat
                        android.util.Log.e("APK_SCREEN", "Error al desinstal·lar: ${e.message}")
                    }
                },
                icon = Icons.Default.Delete
            )
            SecondaryButton(
                text = "Desinstal·lar AceServe",
                onClick = {
                    try {
                    val intent = Intent(Intent.ACTION_DELETE)
                    intent.data = Uri.parse("package:org.free.aceserve")
                    context.startActivity(intent)
                } catch (e: Exception) {
                // Si falla, almenys veuràs el motiu al Logcat
                android.util.Log.e("APK_SCREEN", "Error al desinstal·lar: ${e.message}")
            }
                },
                icon = Icons.Default.Delete
            )
        }

        if (uiState.apks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hi ha APK disponibles a la llista activa", color = TextMuted, fontSize = 14.sp)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(280.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.apks) { apk ->
                    ApkCard(
                        apk = apk,
                        downloadState = uiState.downloadStates[apk.name],
                        onDownload = { viewModel.downloadApk(apk) },
                        onInstall = {
                            val state = uiState.downloadStates[apk.name]
                            if (state is DownloadState.Done) ApkInstaller.installApk(context, state.file)
                        }
                    )
                }
            }
        }
    }
}