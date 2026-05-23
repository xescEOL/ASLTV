package com.xesc.asltv

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.xesc.asltv.data.repository.ChannelRepository
import com.xesc.asltv.ui.navigation.NavGraph
import com.xesc.asltv.ui.components.SideNav
import com.xesc.asltv.ui.themes.AceStreamTVTheme
import com.xesc.asltv.utils.DeviceDetector
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.lifecycleScope
import com.xesc.asltv.ui.navigation.Screen
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var channelRepository: ChannelRepository

    private var navController: NavController? = null

    companion object {
        var isPlayerFullScreen by mutableStateOf(false)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val isTV = DeviceDetector.isAndroidTV(this)
        
        // Refrescar todas las listas al iniciar la aplicación
        lifecycleScope.launch {
            try {
                val lists = channelRepository.getAllSavedLists().first()
                lists.forEach { list ->
                    channelRepository.refreshList(list.userUrl, force = true)
                }
                
                // Check for app update after refreshing lists
                val appUpdate = channelRepository.checkForAppUpdate()
                if (appUpdate != null) {
                    val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                    prefs.edit()
                        .putString("app_update_version", appUpdate.version)
                        .putString("app_update_description", appUpdate.description)
                        .putString("app_update_url", appUpdate.url)
                        .apply()
                }
            } catch (e: Exception) {
                println("Error refreshing lists on startup: ${e.message}")
            }
        }
        
        setContent {
            AceStreamTVTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val currentNavController = rememberNavController()
                    navController = currentNavController

                    // Manejar el Intent inicial si existe
                    LaunchedEffect(Unit) {
                        intent?.let { handleIntent(it) }
                    }

                    // Reloj en tiempo real
                    var currentTime by remember { mutableStateOf(getFormattedTime()) }
                    LaunchedEffect(Unit) {
                        while (true) {
                            delay(1000)
                            currentTime = getFormattedTime()
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            if (!isPlayerFullScreen) {
                                SideNav(navController = currentNavController, isTV = isTV)
                            }
                            NavGraph(navController = currentNavController, isTV = isTV)
                        }

                        if (!isPlayerFullScreen) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 14.dp, end = 18.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.55f))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = currentTime,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val action = intent.action
        val data: Uri? = intent.data
        if (Intent.ACTION_VIEW == action && data != null) {
            val url = data.toString()
            navController?.navigate(Screen.Player.createRoute(url)) {
                popUpTo(Screen.Channels.route) {
                    inclusive = false
                }
                launchSingleTop = true
            }
        }
    }

    private fun getFormattedTime(): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
}
