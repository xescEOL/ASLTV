package com.xesc.asltv.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

object AceStreamHelper {

    private const val ACESTREAM_PACKAGE = "org.acestream.media"

    fun isAceStreamInstalled(context: Context): Boolean =
        try {
            context.packageManager.getPackageInfo(ACESTREAM_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) { false }

    /** Formatea la URL de AceStream correctamente */
    fun formatUrl(raw: String): String =
        if (raw.startsWith("acestream://")) raw
        else "acestream://$raw"

    /** Abre AceStream con el hash/url dado */
    fun openChannel(context: Context, url: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(formatUrl(url))).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) { false }
    }
}