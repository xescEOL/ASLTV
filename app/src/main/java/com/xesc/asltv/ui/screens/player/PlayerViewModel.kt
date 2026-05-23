package com.xesc.asltv.ui.screens.player

import android.app.Application
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import okhttp3.OkHttpClient
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.Request
import java.io.IOException

data class AudioTrackInfo(
    val groupIndex: Int,
    val trackIndex: Int,
    val label: String,
    val isSelected: Boolean,
    val format: androidx.media3.common.Format?
)

data class PlaylistChannel(
    val name: String,
    val url: String,
    val logo: String? = null
)

@HiltViewModel
@OptIn(UnstableApi::class)
class PlayerViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val _player = MutableStateFlow<ExoPlayer?>(null)
    val player: StateFlow<ExoPlayer?> = _player.asStateFlow()

    private val _audioTracks = MutableStateFlow<List<AudioTrackInfo>>(emptyList())
    val audioTracks: StateFlow<List<AudioTrackInfo>> = _audioTracks.asStateFlow()

    private val _playlistChannels = MutableStateFlow<List<PlaylistChannel>>(emptyList())
    val playlistChannels: StateFlow<List<PlaylistChannel>> = _playlistChannels.asStateFlow()

    private val _currentChannelIndex = MutableStateFlow(-1)
    val currentChannelIndex: StateFlow<Int> = _currentChannelIndex.asStateFlow()

    private val _isAudioOnly = MutableStateFlow(false)
    val isAudioOnly: StateFlow<Boolean> = _isAudioOnly.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError: StateFlow<String?> = _playbackError.asStateFlow()

    private var exoPlayer: ExoPlayer? = null
    private lateinit var mediaSourceFactory: DefaultMediaSourceFactory
    private lateinit var okHttpClient: OkHttpClient
    private var lastUrl: String? = null
    private var watchdogJob: Job? = null
    private var lastPosition = -1L
    private var stallCount = 0

    init {
        // ... (rest of init)
        // CONFIGURACIÓ CRÍTICA: Permetem que el vídeo no s'esperi a l'àudio
        val renderersFactory = DefaultRenderersFactory(application)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            .setEnableDecoderFallback(true)

        val extractorsFactory = DefaultExtractorsFactory()
            .setTsExtractorFlags(DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES)

        // Configurar un OkHttpClient que ignora errors de SSL (Chain validation failed)
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())
        
        this.okHttpClient = OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val requestBuilder = originalRequest.newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "*/*")
                
                val request = requestBuilder.build()
                android.util.Log.d("PLAYER_OKHTTP", "Solicitant: ${request.url}")
                
                try {
                    val response = chain.proceed(request)
                    android.util.Log.d("PLAYER_OKHTTP", "Resposta: ${response.code}, Content-Type: ${response.header("Content-Type")}")
                    response
                } catch (e: Exception) {
                    android.util.Log.e("PLAYER_OKHTTP", "Error en la petició OkHttp: ${e.message}")
                    throw e
                }
            }
            .build()

        val httpDataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
        
        // Forcem l'ús d'OkHttp per a tot el que sigui HTTP/HTTPS
        val dataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(application, httpDataSourceFactory)

        this.mediaSourceFactory = DefaultMediaSourceFactory(application, extractorsFactory)
            .setDataSourceFactory(dataSourceFactory)
        
        // Referències explícites per evitar que R8 les elimini o que no es carreguin al DEX
        try {
            Class.forName("androidx.media3.exoplayer.hls.HlsMediaSource\$Factory")
            android.util.Log.d("PlayerViewModel", "Media3 Extensions vinculades correctament")
        } catch (e: Exception) {
            android.util.Log.e("PlayerViewModel", "ERROR: No s'han trobat les extensions de Media3", e)
        }

        // Augmentem el buffer per a streamings inestables
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                60000,  // Min buffer
                120000, // Max buffer
                2500,   // Buffer for playback (més baix per començar abans)
                5000    // Buffer for rebuffering
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        exoPlayer = ExoPlayer.Builder(application, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .build()
            .apply {
                playWhenReady = true

                addListener(object : Player.Listener {
                    override fun onTracksChanged(tracks: Tracks) {
                        updateAudioTracks(tracks)
                        _isAudioOnly.value = tracks.groups.any { it.type == C.TRACK_TYPE_AUDIO } &&
                                            !tracks.groups.any { it.type == C.TRACK_TYPE_VIDEO }
                    }
                    
                    override fun onPlaybackStateChanged(state: Int) {
                        _isLoading.value = state == Player.STATE_BUFFERING
                        
                        if (state == Player.STATE_READY) {
                            _playbackError.value = null
                            startWatchdog() // Iniciem watchdog quan estigui llest
                        } else if (state == Player.STATE_ENDED) {
                            // Si acaba prematurament (comú en ràdios), intentem reconnectar
                            retryPlayback()
                        }
                    }

                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        _isLoading.value = false
                        android.util.Log.e("PlayerViewModel", "Error de reproducció: ${error.message}")
                        _playbackError.value = "Error: ${error.message}. Reconnectant..."
                        retryPlayback()
                    }
                })
            }
        _player.value = exoPlayer
    }

    private fun startWatchdog() {
        watchdogJob?.cancel()
        watchdogJob = viewModelScope.launch(Dispatchers.Main) {
            while (true) {
                delay(5000)
                val player = exoPlayer ?: break
                
                if (player.isPlaying) {
                    val currentPos = player.currentPosition
                    if (currentPos == lastPosition && currentPos != 0L) {
                        stallCount++
                        if (stallCount >= 2) { // 10 segons congelat
                            android.util.Log.w("PlayerViewModel", "Watchdog: Stall detectat, reconnectant...")
                            retryPlayback()
                            break
                        }
                    } else {
                        stallCount = 0
                        lastPosition = currentPos
                    }
                } else if (player.playbackState == Player.STATE_BUFFERING) {
                    stallCount++
                    if (stallCount >= 4) { // 20 segons buffering
                        android.util.Log.w("PlayerViewModel", "Watchdog: Buffering massa llarg, reconnectant...")
                        retryPlayback()
                        break
                    }
                }
            }
        }
    }

    private fun retryPlayback() {
        lastUrl?.let { url ->
            viewModelScope.launch(Dispatchers.Main) {
                delay(2000) // Esperar una mica abans de reintentar
                playUrl(url)
            }
        }
    }

    fun preparePlayer(url: String) {
        val currentPlayer = exoPlayer
        if (currentPlayer != null && url == lastUrl && 
            currentPlayer.playbackState != Player.STATE_IDLE && 
            currentPlayer.playbackState != Player.STATE_ENDED) {
            android.util.Log.d("PlayerViewModel", "preparePlayer: La mateixa URL ja s'està reproduint, ignorem.")
            return
        }

        // Si és AceStream, no fem el check d'OkHttp perquè sol fallar fins que el motor està llest
        if (url.startsWith("acestream://", ignoreCase = true) || 
            (url.length == 40 && url.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' })) {
            playUrl(url)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _playbackError.value = null
            
            try {
                val request = Request.Builder().url(url).build()
                val response = okHttpClient.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    launch(Dispatchers.Main) {
                        _playbackError.value = "Error al carregar: ${response.code}"
                        _isLoading.value = false
                    }
                    response.close()
                    return@launch
                }

                val contentType = response.header("Content-Type") ?: ""
                val isLikelyPlaylist = contentType.contains("mpegurl", ignoreCase = true) || 
                                       contentType.contains("x-mpegurl", ignoreCase = true) ||
                                       url.contains(".m3u", ignoreCase = true)

                if (isLikelyPlaylist) {
                    // Peek a bit to see if it's an IPTV list or a single HLS stream
                    val peek = response.peekBody(1024 * 16).string()
                    if (peek.contains("#EXTINF") && (peek.contains("tvg-") || !peek.contains("#EXT-X-TARGETDURATION"))) {
                        // IPTV Playlist - we need the whole thing
                        val body = response.body?.string() ?: ""
                        response.close()
                        val channels = parseIptvPlaylist(body)
                        launch(Dispatchers.Main) {
                            _playlistChannels.value = channels
                            if (channels.isNotEmpty()) {
                                selectPlaylistChannel(0)
                            } else {
                                _playbackError.value = "La llista no conté canals vàlids"
                                _isLoading.value = false
                            }
                        }
                    } else {
                        // HLS Stream
                        response.close()
                        launch(Dispatchers.Main) {
                            _playlistChannels.value = emptyList()
                            _currentChannelIndex.value = -1
                            playUrl(url)
                        }
                    }
                } else {
                    // Direct stream (MP4, MP3, AceStream Engine HTTP, etc)
                    response.close()
                    launch(Dispatchers.Main) {
                        _playlistChannels.value = emptyList()
                        _currentChannelIndex.value = -1
                        playUrl(url)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("PlayerViewModel", "Error en preparePlayer: ${e.message}")
                launch(Dispatchers.Main) {
                    _playbackError.value = "Error: ${e.message}"
                    _isLoading.value = false
                }
            }
        }
    }

    private fun parseIptvPlaylist(content: String): List<PlaylistChannel> {
        val channelsMap = LinkedHashMap<String, PlaylistChannel>()
        val lines = content.lines()
        var currentName = ""
        var currentLogo: String? = null

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#EXTINF:")) {
                currentName = trimmed.substringAfter(",").trim()
                if (currentName.isEmpty()) currentName = "Canal sense nom"
                
                currentLogo = if (trimmed.contains("tvg-logo=\"")) {
                    trimmed.substringAfter("tvg-logo=\"").substringBefore("\"")
                } else null
            } else if (trimmed.startsWith("http")) {
                // If the name repeats, the last occurrence will overwrite previous ones in the map
                channelsMap[currentName] = PlaylistChannel(currentName, trimmed, currentLogo)
                currentName = ""
                currentLogo = null
            }
        }
        return channelsMap.values.toList()
    }

    fun selectPlaylistChannel(index: Int) {
        val channels = _playlistChannels.value
        if (index in channels.indices) {
            _currentChannelIndex.value = index
            playUrl(channels[index].url)
        }
    }

    private fun playUrl(url: String) {
        // Conversió d'AceStream a URL d'AceServe local si cal
        val finalUrl = if (url.startsWith("acestream://", ignoreCase = true)) {
            val hash = url.substringAfter("acestream://")
            "http://127.0.0.1:6878/ace/getstream?id=$hash"
        } else if (url.length == 40 && url.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) {
            "http://127.0.0.1:6878/ace/getstream?id=$url"
        } else {
            url
        }

        lastUrl = url // Guardem l'original per reintents
        lastPosition = -1L
        stallCount = 0
        viewModelScope.launch(Dispatchers.Main) {
            exoPlayer?.let { player ->
                android.util.Log.d("PLAYER_OKHTTP", "Reproduint: $finalUrl (original: $url)")
                _isLoading.value = true
                
                player.stop()
                player.clearMediaItems()
                
                val mediaItem = MediaItem.Builder()
                    .setUri(finalUrl)
                    .build()
                
                val mediaSource = mediaSourceFactory.createMediaSource(mediaItem)
                player.setMediaSource(mediaSource)
                
                player.prepare()
                player.play()
            }
        }
    }

    private fun updateAudioTracks(tracks: Tracks) {
        val audioTracksList = mutableListOf<AudioTrackInfo>()
        val player = exoPlayer ?: return

        val audioDisabled = player.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_AUDIO)
        audioTracksList.add(AudioTrackInfo(-1, -1, "Sense àudio", audioDisabled, null))

        tracks.groups.forEachIndexed { groupIndex, group ->
            if (group.type == C.TRACK_TYPE_AUDIO) {
                for (trackIndex in 0 until group.length) {
                    val format = group.getTrackFormat(trackIndex)
                    val label = format.label ?: format.language ?: "Àudio ${audioTracksList.size}"
                    audioTracksList.add(
                        AudioTrackInfo(groupIndex, trackIndex, label, !audioDisabled && group.isTrackSelected(trackIndex), format)
                    )
                }
            }
        }
        _audioTracks.value = audioTracksList
    }

    fun selectAudioTrack(track: AudioTrackInfo) {
        exoPlayer?.let { player ->
            val builder = player.trackSelectionParameters.buildUpon()

            if (track.groupIndex == -1) {
                builder.setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
            } else {
                val trackGroups = player.currentTracks.groups
                if (track.groupIndex < trackGroups.size) {
                    val group = trackGroups[track.groupIndex]
                    builder.setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                        .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, track.trackIndex))
                }
            }

            // Apliquem els paràmetres
            player.trackSelectionParameters = builder.build()

            // Per evitar la congelació al canviar d'àudio:
            // No fem pausa ni seek manual, ExoPlayer ja s'encarrega de sincronitzar.
            // Si es congela, sol ser per un problema de renderització que es soluciona
            // mantenint el playWhenReady i deixant que el buffer es reompli.
        }
    }

    fun releasePlayer() {
        watchdogJob?.cancel()
        exoPlayer?.release()
        exoPlayer = null
        _player.value = null
    }

    override fun onCleared() {
        super.onCleared()
        releasePlayer()
    }
}