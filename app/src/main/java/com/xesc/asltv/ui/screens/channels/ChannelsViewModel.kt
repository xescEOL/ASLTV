package com.xesc.asltv.ui.screens.channels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xesc.asltv.data.local.entity.EpgProgramEntity
import com.xesc.asltv.data.model.AppUpdate
import com.xesc.asltv.data.model.ChannelList
import com.xesc.asltv.data.model.Group
import com.xesc.asltv.data.model.MirrorGroup
import com.xesc.asltv.data.model.Station
import com.xesc.asltv.data.repository.ChannelRepository
import com.xesc.asltv.data.repository.EpgRepository
import com.xesc.asltv.data.repository.PreferencesRepository
import com.xesc.asltv.utils.ApkInstaller
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import javax.inject.Inject

data class ChannelNavigation(
    val url: String,
    val name: String?,
    val logo: String? = null,
    val timestamp: Long = System.nanoTime()
)

@HiltViewModel
class ChannelsViewModel @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val epgRepository: EpgRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChannelsUiState())
    val uiState: StateFlow<ChannelsUiState> = _uiState.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<ChannelNavigation>(extraBufferCapacity = 1)
    val navigationEvents: SharedFlow<ChannelNavigation> = _navigationEvents.asSharedFlow()

    private var epgSyncJob: Job? = null
    private var epgDataJob: Job? = null
    private var focusedEpgJob: Job? = null
    private var pendingFocusJob: Job? = null

    init {
        // Carga inicial de datos de canales
        viewModelScope.launch {
            combine(
                channelRepository.getActiveChannelList(),
                preferencesRepository.isLiteMode
            ) { list, isLite ->
                Pair(list, isLite)
            }.collect { (list, isLite) ->
                val groups = buildGroups(list, isLite)
                val bannerImage = list?.bannerImage
                val epgBaseUrl = list?.epgUrl

                _uiState.update { current ->
                    // Si el grupo seleccionado ya no existe en la nueva lista, lo limpiamos
                    val stillValid = current.selectedGroup != null &&
                            groups.any { it.first.name == current.selectedGroup }
                    val selectedGroup = if (stillValid) current.selectedGroup else null
                    val channels = if (selectedGroup != null)
                        groups.firstOrNull { it.first.name == selectedGroup }?.second ?: emptyList()
                    else emptyList()
                    
                    val focusedChannel = if (selectedGroup == null) null 
                                         else if (stillValid) current.focusedChannel ?: channels.firstOrNull()
                                         else channels.firstOrNull()

                    current.copy(
                        groups = groups,
                        selectedGroup = selectedGroup,
                        channels = channels,
                        bannerImage = bannerImage,
                        focusedChannel = focusedChannel
                    )
                }

                // Disparar sincronización de EPG en segundo plano sin bloquear el flow de canales
                if (!epgBaseUrl.isNullOrBlank()) {
                    startEpgSync(epgBaseUrl, groups)
                } else {
                    epgRepository.updateSyncState(false)
                }
                
                // Si ya hay canales, cargar su info básica de EPG (lo que se ve en las tarjetas)
                val currentChannels = _uiState.value.channels
                if (currentChannels.isNotEmpty()) {
                    loadEpgForChannels(currentChannels)
                }
            }
        }
        
        checkForAppUpdate()
    }

    private fun startEpgSync(baseUrl: String, groups: List<Pair<Group, List<MirrorGroup>>>) {
        epgSyncJob?.cancel()
        epgSyncJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val epgFiles = groups.flatMap { it.second }
                    .mapNotNull { it.epgfile }
                    .filter { it.isNotBlank() }
                    .distinct()
                
                val prioritized = epgFiles.filter { it.contains("ES") || it.contains("spain") } +
                                 epgFiles.filter { !it.contains("ES") && !it.contains("spain") }

                if (prioritized.isEmpty()) {
                    epgRepository.updateSyncState(false)
                    return@launch
                }

                epgRepository.updateSyncState(true, 0f)

                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(epgLoadingStatus = "Iniciant EPG...", epgError = null) }
                }

                prioritized.forEachIndexed { index, file ->
                    val progress = (index.toFloat() / prioritized.size.toFloat())
                    epgRepository.updateSyncState(true, progress)

                    withContext(Dispatchers.Main) {
                        _uiState.update { it.copy(epgLoadingStatus = "EPG ${index + 1}/${prioritized.size}") }
                    }
                    
                    // Intentar cargar el archivo
                    runCatching {
                        withTimeout(90000L) {
                            epgRepository.refreshEpg(baseUrl, file).getOrNull()
                        }
                    }

                    // Cada vez que un archivo se carga, refrescamos la info de los canales visibles
                    // para que el usuario vea progreso
                    val currentChannels = _uiState.value.channels
                    if (currentChannels.isNotEmpty()) {
                        loadEpgForChannels(currentChannels)
                    }

                    // También refrescar el sidebar del canal enfocado si es necesario
                    _uiState.value.focusedChannel?.let { 
                        loadFocusedChannelEpg(it)
                    }
                }

                epgRepository.updateSyncState(false, 1f)

                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(epgLoadingStatus = "EPG Actualitzat", epgError = null) }
                }
            } catch (e: Exception) {
                Log.e("ChannelsViewModel", "EPG Sync error", e)
                epgRepository.updateSyncState(false)
            }
        }
    }

    /** Carga la info EPG básica (Actual/Siguiente) para las tarjetas de la lista */
    private fun loadEpgForChannels(channels: List<MirrorGroup>) {
        epgDataJob?.cancel()
        epgDataJob = viewModelScope.launch(Dispatchers.IO) {
            val epgToChannelNames = mutableMapOf<String, MutableList<Pair<String, Int>>>()
            channels.forEach { ch ->
                ch.epgId?.let { epgId ->
                    epgToChannelNames.getOrPut(epgId) { mutableListOf() }.add(ch.name to ch.epgHora)
                }
            }
            
            val epgIds = epgToChannelNames.keys.toList()
            if (epgIds.isEmpty()) return@launch

            val now = System.currentTimeMillis()
            // Buscamos programas en las próximas 6 horas (más margen para offsets)
            val programs = epgRepository.getProgramsForChannels(epgIds, now - 12 * 3600000L, now + 12 * 3600000L)
            val programsByEpgId = programs.groupBy { it.channelId }
            
            withContext(Dispatchers.Main) {
                _uiState.update { state ->
                    val newEpgData = state.epgData.toMutableMap()
                    
                    epgToChannelNames.forEach { (epgId, nameAndOffsets) ->
                        val basePrograms = programsByEpgId[epgId] ?: emptyList()
                        
                        nameAndOffsets.forEach { (name, offset) ->
                            // Si el usuario pone +3, restamos 3h a la programación para que 
                            // lo que empieza a las 18:00 (XML) se vea a las 15:00 (local).
                            val shiftedPrograms = basePrograms.map { it.withOffset(-offset) }
                            val current = shiftedPrograms.firstOrNull { it.startTime <= now && it.endTime > now }
                            val upcoming = shiftedPrograms.filter { it.startTime >= (current?.endTime ?: now) }
                            
                            val existing = newEpgData[name]
                            if (existing == null || existing.upcoming.size <= 2) {
                                newEpgData[name] = EpgInfo(current, upcoming.take(2))
                            }
                        }
                    }
                    state.copy(epgData = newEpgData)
                }
            }
        }
    }

    /** Carga el EPG completo (Sidebar) para el canal enfocado */
    private fun loadFocusedChannelEpg(channel: MirrorGroup) {
        focusedEpgJob?.cancel()
        focusedEpgJob = viewModelScope.launch(Dispatchers.IO) {
            val epgId = channel.epgId ?: return@launch
            val offset = channel.epgHora
            // El repositorio ya aplica el offset invertido (-offset)
            val info = epgRepository.getChannelEpgInfo(epgId, offset)
            
            withContext(Dispatchers.Main) {
                _uiState.update { state ->
                    val newEpgData = state.epgData.toMutableMap()
                    newEpgData[channel.name] = EpgInfo(info.current, info.upcoming)
                    state.copy(epgData = newEpgData)
                }
            }
        }
    }

    // ── Interacciones ─────────────────────────────────────────────────────────

    fun selectGroup(groupName: String) {
        val groups = _uiState.value.groups
        val groupData = groups.firstOrNull { it.first.name == groupName } ?: return
        val channels = groupData.second
        
        _uiState.update { it.copy(
            selectedGroup = groupName,
            channels = channels,
            focusedChannel = channels.firstOrNull()
        ) }

        // Al cambiar de grupo, cargamos su EPG
        loadEpgForChannels(channels)
        channels.firstOrNull()?.let { loadFocusedChannelEpg(it) }
    }

    fun clearGroup() {
        _uiState.update { it.copy(selectedGroup = null, channels = emptyList(), focusedChannel = null) }
    }

    fun selectChannel(mirrorGroup: MirrorGroup?) {
        if (mirrorGroup == null) return
        if (_uiState.value.focusedChannel?.name == mirrorGroup.name) return

        _uiState.update { it.copy(focusedChannel = mirrorGroup) }
        
        // Cargar EPG del sidebar con un pequeño delay para no saturar si el usuario navega rápido
        pendingFocusJob?.cancel()
        pendingFocusJob = viewModelScope.launch {
            delay(150)
            loadFocusedChannelEpg(mirrorGroup)
        }
    }

    fun onChannelClicked(mirrorGroup: MirrorGroup) {
        selectChannel(mirrorGroup)
        if (mirrorGroup.hasMirrors) {
            _uiState.update { it.copy(showMirrorDialog = true, currentMirrors = mirrorGroup.stations) }
        } else {
            playChannel(mirrorGroup.primaryUrl, mirrorGroup.name, mirrorGroup.image)
        }
    }

    fun playChannel(url: String, name: String? = null, logo: String? = null) {
        viewModelScope.launch {
            _navigationEvents.emit(ChannelNavigation(url, name, logo))
        }
    }

    fun dismissMirrors() {
        _uiState.update { it.copy(showMirrorDialog = false) }
    }

    private fun buildGroups(list: ChannelList?, isLite: Boolean): List<Pair<Group, List<MirrorGroup>>> {
        return list?.groups?.mapNotNull { group ->
            val mirrorGroups = group.stations
                .filter { if (isLite) it.lite else true }
                .groupBy { it.name.trim() }
                .values
                .map { MirrorGroup(it) }
            if (mirrorGroups.isEmpty()) null else group to mirrorGroups
        } ?: emptyList()
    }

    private fun checkForAppUpdate() {
        viewModelScope.launch {
            try {
                val appUpdate = channelRepository.checkForAppUpdate()
                if (appUpdate != null) {
                    _uiState.update { it.copy(appUpdate = appUpdate) }
                }
            } catch (e: Exception) {
                Log.e("ChannelsViewModel", "Error checking app update", e)
            }
        }
    }

    fun dismissAppUpdate() {
        _uiState.update { it.copy(appUpdate = null) }
    }

    fun installAppUpdate(context: android.content.Context, appUpdate: AppUpdate) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isDownloadingUpdate = true, downloadProgress = 0f) }
                ApkInstaller.downloadAndInstallApk(
                    context = context,
                    url = appUpdate.url,
                    version = appUpdate.version,
                    onProgress = { progress ->
                        _uiState.update { it.copy(downloadProgress = progress) }
                    }
                )
                _uiState.update { it.copy(isDownloadingUpdate = false, downloadProgress = 0f) }
                dismissAppUpdate()
            } catch (e: Exception) {
                _uiState.update { it.copy(isDownloadingUpdate = false, downloadProgress = 0f) }
                Log.e("ChannelsViewModel", "Error installing app update", e)
            }
        }
    }
}

data class EpgInfo(
    val current: EpgProgramEntity?,
    val upcoming: List<EpgProgramEntity>
)

data class ChannelsUiState(
    val groups: List<Pair<Group, List<MirrorGroup>>> = emptyList(),
    val selectedGroup: String? = null,
    val channels: List<MirrorGroup> = emptyList(),
    val bannerImage: String? = null,
    val epgData: Map<String, EpgInfo> = emptyMap(),
    val focusedChannel: MirrorGroup? = null,
    val showMirrorDialog: Boolean = false,
    val currentMirrors: List<Station> = emptyList(),
    val appUpdate: AppUpdate? = null,
    val epgLoadingStatus: String = "Cargando EPG...",
    val epgError: String? = null,
    val isDownloadingUpdate: Boolean = false,
    val downloadProgress: Float = 0f
)
