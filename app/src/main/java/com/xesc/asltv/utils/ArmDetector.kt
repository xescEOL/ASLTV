package com.xesc.asltv.utils

import android.os.Build

object ArmDetector {
    /** Retorna 8 (arm64-v8a), 7 (armeabi-v7a), o 0 (otro/desconocido) */
    fun getArmVersion(): Int {
        val abis = Build.SUPPORTED_ABIS
        return when {
            abis.contains("arm64-v8a") -> 8
            abis.contains("armeabi-v7a") -> 7
            else -> 0
        }
    }

    /** Filtra APKs compatibles con este dispositivo */
    fun filterCompatibleApks(apks: List<com.xesc.asltv.data.model.Apk>): List<com.xesc.asltv.data.model.Apk> {
        val deviceArm = getArmVersion()
        // Agrupa por nombre y selecciona el más compatible
        return apks.groupBy { it.name }.map { (_, versions) ->
            // Prefiere: versión exacta para el ARM del dispositivo → universal (null)
            versions.firstOrNull { it.armv == deviceArm }
                ?: versions.firstOrNull { it.armv == null }
                ?: versions.first()
        }
    }
}