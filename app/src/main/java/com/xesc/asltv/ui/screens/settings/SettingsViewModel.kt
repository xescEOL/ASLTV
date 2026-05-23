package com.xesc.asltv.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xesc.asltv.data.model.Apk
import com.xesc.asltv.data.model.AppUpdate
import com.xesc.asltv.data.model.SavedListInfo
import com.xesc.asltv.data.repository.ApkRepository
import com.xesc.asltv.data.repository.ChannelRepository
import com.xesc.asltv.data.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val preferencesRepository: PreferencesRepository,
    private val apkRepository: ApkRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        println("SettingsViewModel: INITIALIZED")
        loadSavedLists()
        loadPreferences()
        //checkForAppUpdateFromPrefs()
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            val isLite = preferencesRepository.isLiteMode.first()
            _uiState.update { it.copy(isLiteMode = isLite) }
        }
    }

    private fun loadSavedLists() {
        viewModelScope.launch {
            channelRepository.getAllSavedLists().collect { entities ->
                val lists = entities.map { entity ->
                    SavedListInfo(
                        userUrl      = entity.userUrl,
                        name         = entity.name,
                        author       = entity.author,
                        isActive     = entity.isActive,
                        lastUpdated  = entity.lastUpdated,
                        groupCount   = 0,
                        channelCount = 0,
                        epgUrl       = entity.epgUrl
                    )
                }
                _uiState.update { it.copy(savedLists = lists) }
            }
        }
    }

    fun setActiveList(userUrl: String) {
        viewModelScope.launch {
            channelRepository.setActiveList(userUrl)
        }
    }

    fun deleteList(userUrl: String) {
        viewModelScope.launch {
            channelRepository.deleteList(userUrl)
        }
    }

    fun showAddListDialog() {
        _uiState.update { it.copy(showAddListDialog = true) }
    }

    fun hideAddListDialog() {
        _uiState.update { it.copy(showAddListDialog = false) }
    }

    fun setLiteMode(isLite: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setLiteMode(isLite)
            _uiState.update { it.copy(isLiteMode = isLite) }
        }
    }

    fun refreshActiveList() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            val activeList = _uiState.value.savedLists.firstOrNull { it.isActive }
            if (activeList != null) {
                channelRepository.refreshList(activeList.userUrl, force = true)
                val fmt = SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault())
                _uiState.update { it.copy(lastUpdateText = "Actualitzat: ${fmt.format(Date())}") }
                
                // Check for app update after refresh
                checkForAppUpdate()
            }
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun manualCheckForAppUpdate() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingUpdates = true) }
            try {
                val appUpdate = channelRepository.checkForAppUpdate()
                if (appUpdate != null) {
                    _uiState.update { it.copy(appUpdate = appUpdate) }
                } else {
                    _uiState.update { it.copy(errorMessage = "Ja tens l'última versió instal·lada") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error al comprovar actualitzacions: ${e.message}") }
            }
            _uiState.update { it.copy(isCheckingUpdates = false) }
        }
    }

    fun installUpdate(appUpdate: AppUpdate) {
        viewModelScope.launch {
            val apk = Apk(
                name = "ASLTV Update",
                url = appUpdate.url,
                version = appUpdate.version,
                description = appUpdate.description
            )
            
            _uiState.update { it.copy(updateDownloadProgress = 0f) }
            
            apkRepository.downloadApk(
                apk = apk,
                onProgress = { progress ->
                    _uiState.update { it.copy(updateDownloadProgress = progress) }
                },
                onComplete = { file ->
                    _uiState.update { it.copy(updateDownloadProgress = null, appUpdateFile = file) }
                },
                onError = { error ->
                    _uiState.update { it.copy(updateDownloadProgress = null, errorMessage = "Error en descarregar l'actualització: $error") }
                }
            )
        }
    }

    private fun checkForAppUpdate() {
        viewModelScope.launch {
            println("SettingsViewModel: Iniciando checkForAppUpdate")
            val appUpdate = channelRepository.checkForAppUpdate()
            println("SettingsViewModel: AppUpdate result: ${appUpdate?.version}")
            if (appUpdate != null) {
                println("SettingsViewModel: Actualizando UI state con appUpdate")
                _uiState.update { it.copy(appUpdate = appUpdate) }
            } else {
                println("SettingsViewModel: No hay actualización disponible")
            }
        }
    }

    fun dismissAppUpdate() {
        _uiState.update { it.copy(appUpdate = null) }
    }

    fun addList(url: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            
            // Add https:// prefix if not present
            val normalizedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                "https://$url"
            } else {
                url
            }
            
            val result = channelRepository.addList(normalizedUrl)
            
            result.onSuccess {
                hideAddListDialog()
            }.onFailure { e ->
                _uiState.update { it.copy(errorMessage = "Error: ${e.message ?: "Desconocido"}") }
            }
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

data class SettingsUiState(
    val savedLists: List<SavedListInfo> = emptyList(),
    val isLiteMode: Boolean = false,
    val isRefreshing: Boolean = false,
    val isCheckingUpdates: Boolean = false,
    val updateDownloadProgress: Float? = null,
    val appUpdateFile: java.io.File? = null,
    val lastUpdateText: String? = null,
    val showAddListDialog: Boolean = false,
    val errorMessage: String? = null,
    val appUpdate: AppUpdate? = null
)