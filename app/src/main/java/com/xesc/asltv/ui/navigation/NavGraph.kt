package com.xesc.asltv.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.xesc.asltv.ui.screens.apk.ApksScreen
import com.xesc.asltv.ui.screens.channels.ChannelsScreen
import com.xesc.asltv.ui.screens.player.PlayerScreen
import com.xesc.asltv.ui.screens.settings.SettingsScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument

@Composable
fun NavGraph(navController: NavHostController, isTV: Boolean) {
    NavHost(navController, startDestination = Screen.Channels.route) {
        composable(Screen.Channels.route) {
            ChannelsScreen(navController = navController, isTV = isTV)
        }
        composable(Screen.Apks.route) {
            ApksScreen(navController = navController, isTV = isTV)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(navController = navController, isTV = isTV)
        }
        composable(
            route = Screen.Player.route,
            arguments = listOf(
                navArgument("url") { type = NavType.StringType },
                navArgument("name") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("logo") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { backStackEntry ->
            val url = backStackEntry.arguments?.getString("url") ?: ""
            val name = backStackEntry.arguments?.getString("name")
            val logo = backStackEntry.arguments?.getString("logo")
            PlayerScreen(
                url = url,
                initialName = name,
                initialLogo = logo,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
