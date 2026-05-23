package com.xesc.asltv.ui.navigation

sealed class Screen(val route: String) {
    object Channels : Screen("channels")
    object Apks     : Screen("apks")
    object Settings : Screen("settings")
    object Player   : Screen("player/{url}?name={name}&logo={logo}") {
        fun createRoute(url: String, name: String? = null, logo: String? = null) : String {
            val encodedUrl = java.net.URLEncoder.encode(url, "UTF-8")
            var route = "player/$encodedUrl"
            val queryParams = mutableListOf<String>()
            
            name?.let { queryParams.add("name=${java.net.URLEncoder.encode(it, "UTF-8")}") }
            logo?.let { queryParams.add("logo=${java.net.URLEncoder.encode(it, "UTF-8")}") }
            
            if (queryParams.isNotEmpty()) {
                route += "?${queryParams.joinToString("&")}"
            }
            return route
        }
    }
}
