package com.xesc.asltv.ui.screens.apk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xesc.asltv.data.model.Apk
import com.xesc.asltv.data.repository.ApkRepository
import com.xesc.asltv.data.repository.ChannelRepository
import com.xesc.asltv.ui.components.DownloadState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ApksViewModel @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val apkRepository: ApkRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ApksUiState())
    val uiState: StateFlow<ApksUiState> = _uiState.asStateFlow()

    init {
        loadApks()
    }

    private fun loadApks() {
        viewModelScope.launch {
            val apks = channelRepository.getActiveChannelList().first()?.apks ?: emptyList()
            _uiState.update { it.copy(apks = apks) }
        }
    }

    fun downloadApk(apk: Apk) {
        viewModelScope.launch {
            _uiState.update { it.copy(downloadStates = it.downloadStates + (apk.name to DownloadState.Downloading(0f))) }
            
            apkRepository.downloadApk(
                apk = apk,
                onProgress = { progress ->
                    _uiState.update { state ->
                        state.copy(downloadStates = state.downloadStates + (apk.name to DownloadState.Downloading(progress)))
                    }
                },
                onComplete = { file ->
                    _uiState.update { state ->
                        state.copy(downloadStates = state.downloadStates + (apk.name to DownloadState.Done(file)))
                    }
                },
                onError = { error ->
                    _uiState.update { state ->
                        state.copy(downloadStates = state.downloadStates + (apk.name to DownloadState.Error(error)))
                    }
                }
            )
        }
    }

    fun installApk(apk: Apk) {
        viewModelScope.launch {
            val downloadState = _uiState.value.downloadStates[apk.name]
            if (downloadState is DownloadState.Done) {
                // TODO: Implement APK installation
            }
        }
    }
}

data class ApksUiState(
    val apks: List<Apk> = emptyList(),
    val downloadStates: Map<String, DownloadState> = emptyMap()
)