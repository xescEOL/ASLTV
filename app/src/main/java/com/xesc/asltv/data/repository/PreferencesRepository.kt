package com.xesc.asltv.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("settings")

@Singleton
class PreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val LITE_MODE = booleanPreferencesKey("lite_mode")
    }

    val isLiteMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[LITE_MODE] ?: false
    }

    suspend fun setLiteMode(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[LITE_MODE] = enabled }
    }
}