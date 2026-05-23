package com.xesc.asltv.ui.screens.player

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.xesc.asltv.MainActivity
import com.xesc.asltv.ui.themes.AccentGold
import kotlinx.coroutines.delay

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    url: String,
    initialName: String? = null,
    initialLogo: String? = null,
    onBack: () -> Unit
) {
    val viewModel: PlayerViewModel = hiltViewModel()
    val player by viewModel.player.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val playbackError by viewModel.playbackError.collectAsState()
    val playlistChannels by viewModel.playlistChannels.collectAsState()
    val currentChannelIndex by viewModel.currentChannelIndex.collectAsState()
    val isAudioOnly by viewModel.isAudioOnly.collectAsState()

    var showAudioTracks by remember { mutableStateOf(false) }
    var showPlaylist by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    var interactionCount by remember { mutableIntStateOf(0) }

    val backButtonFocusRequester = remember { FocusRequester() }
    val playlistFocusRequester = remember { FocusRequester() }

    // Lògica per obtenir el canal actual i les seves dades
    val currentChannel = remember(currentChannelIndex, playlistChannels) {
        if (currentChannelIndex != -1 && currentChannelIndex < playlistChannels.size) {
            playlistChannels[currentChannelIndex]
        } else null
    }

    val currentChannelName = remember(currentChannel, initialName, player) {
        currentChannel?.name ?: run {
            try {
                initialName?.let { java.net.URLDecoder.decode(it, "UTF-8") }
            } catch (_: Exception) {
                initialName
            }
        } ?: player?.currentMediaItem?.localConfiguration?.uri?.lastPathSegment ?: ""
    }

    val currentChannelLogo = remember(currentChannel, initialLogo) {
        currentChannel?.logo ?: run {
            try {
                initialLogo?.let { java.net.URLDecoder.decode(it, "UTF-8") }
            } catch (_: Exception) {
                initialLogo
            }
        }
    }

    BackHandler(showPlaylist || showAudioTracks) {
        if (showPlaylist) {
            showPlaylist = false
            controlsVisible = true
        } else if (showAudioTracks) {
            showAudioTracks = false
            controlsVisible = true
        }
    }

    LaunchedEffect(url) {
        viewModel.preparePlayer(url)
    }

    LaunchedEffect(controlsVisible, showAudioTracks, showPlaylist, interactionCount) {
        if (controlsVisible && !showAudioTracks && !showPlaylist) {
            delay(5000)
            controlsVisible = false
        }
    }

    LaunchedEffect(controlsVisible) {
        if (controlsVisible && !showAudioTracks && !showPlaylist) {
            delay(200)
            try {
                backButtonFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(showPlaylist) {
        if (showPlaylist) {
            delay(200)
            try {
                playlistFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(playlistChannels) {
        if (playlistChannels.isNotEmpty() && !showPlaylist) {
            showPlaylist = true
            controlsVisible = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.releasePlayer()
            MainActivity.isPlayerFullScreen = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onFocusChanged {
                if (it.isFocused && !showAudioTracks && !showPlaylist) {
                    controlsVisible = true
                    try { backButtonFocusRequester.requestFocus() } catch (_: Exception) {}
                }
            }
            .onKeyEvent {
                if (!controlsVisible && !showPlaylist) controlsVisible = true
                interactionCount++
                false
            }
            .focusable()
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = player
                            this.keepScreenOn = true
                            useController = false
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    update = { view ->
                        view.player = player
                    },
                    modifier = Modifier.fillMaxSize()
                )

                if (isAudioOnly) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (currentChannelLogo != null) {
                                AsyncImage(
                                    model = currentChannelLogo,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(180.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.White.copy(alpha = 0.05f))
                                        .padding(12.dp),
                                    contentScale = ContentScale.Fit
                                )
                                Spacer(modifier = Modifier.height(32.dp))
                            }

                            Text(
                                text = "Reproduint $currentChannelName",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 40.dp)
                            )
                        }
                    }
                }

                // Overlay de carga y errores
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (isLoading) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = AccentGold)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Carregant canal...",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    playbackError?.let { error ->
                        var showError by remember(error) { mutableStateOf(false) }
                        LaunchedEffect(error) {
                            delay(5000)
                            showError = true
                        }
                        
                        if (showError) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Text(
                                    text = error,
                                    color = Color.Red,
                                    modifier = Modifier.padding(16.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            controlsVisible = !controlsVisible
                            interactionCount++
                        }
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = (controlsVisible || showAudioTracks) && !showPlaylist,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Barra superior: Enrere + Info Canal + Botons Acció
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 40.dp, vertical = 30.dp)
                                    .align(Alignment.TopCenter),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 1. Botó Enrere
                                PlayerIconButton(
                                    onClick = onBack,
                                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    modifier = Modifier.focusRequester(backButtonFocusRequester)
                                )

                                Spacer(modifier = Modifier.width(24.dp))

                                // 2. Logotip i Nom del Canal (Centre)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (currentChannelLogo != null) {
                                        AsyncImage(
                                            model = currentChannelLogo,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(45.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color.White.copy(alpha = 0.15f))
                                                .padding(4.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                    }

                                    Text(
                                        text = currentChannelName,
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .weight(1f, fill = false)
                                            .graphicsLayer(alpha = 0.9f)
                                    )
                                }

                                // 3. Botons d'acció (Dreta)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (playlistChannels.isNotEmpty()) {
                                        PlayerIconButton(
                                            onClick = {
                                                showPlaylist = true
                                                controlsVisible = false
                                                interactionCount++
                                            },
                                            icon = Icons.AutoMirrored.Filled.List,
                                            contentDescription = "Llista de canals"
                                        )
                                    }

                                    PlayerIconButton(
                                        onClick = {
                                            MainActivity.isPlayerFullScreen = !MainActivity.isPlayerFullScreen
                                            controlsVisible = true
                                            interactionCount++
                                        },
                                        icon = if (MainActivity.isPlayerFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                        contentDescription = "Pantalla completa"
                                    )

                                    PlayerIconButton(
                                        onClick = {
                                            showAudioTracks = !showAudioTracks
                                            interactionCount++
                                        },
                                        icon = Icons.Default.AudioFile,
                                        contentDescription = "Pistes d'àudio"
                                    )
                                }
                            }

                            if (showAudioTracks) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.BottomCenter)
                                        .background(Color.Black.copy(alpha = 0.85f))
                                        .padding(vertical = 32.dp)
                                ) {
                                    AudioTrackSelector(
                                        viewModel = viewModel,
                                        onSelect = {
                                            showAudioTracks = false
                                            controlsVisible = true
                                            interactionCount++
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (showPlaylist && playlistChannels.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .width(300.dp)
                        .fillMaxHeight()
                        .background(Color.Black.copy(alpha = 0.9f))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "LLISTA DE CANALS",
                                color = AccentGold,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { showPlaylist = false; controlsVisible = true }) {
                                Icon(Icons.Default.Close, null, tint = Color.White)
                            }
                        }
                        
                        Spacer(Modifier.height(16.dp))

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .onKeyEvent { keyEvent ->
                                    if (keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT) {
                                        showPlaylist = false
                                        controlsVisible = true
                                        true
                                    } else {
                                        false
                                    }
                                },
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(playlistChannels) { index, channel ->
                                val isSelected = index == currentChannelIndex
                                val interactionSource = remember { MutableInteractionSource() }
                                val isFocused by interactionSource.collectIsFocusedAsState()

                                Surface(
                                    onClick = {
                                        viewModel.selectPlaylistChannel(index)
                                        // No cerramos la lista para permitir navegación rápida
                                    },
                                    interactionSource = interactionSource,
                                    shape = RoundedCornerShape(8.dp),
                                    color = when {
                                        isFocused -> Color.White
                                        isSelected -> AccentGold.copy(alpha = 0.2f)
                                        else -> Color.Transparent
                                    },
                                    border = if (isSelected) BorderStroke(1.dp, AccentGold) else null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .then(if (index == 0) Modifier.focusRequester(playlistFocusRequester) else Modifier)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (channel.logo != null) {
                                            AsyncImage(
                                                model = channel.logo,
                                                contentDescription = null,
                                                modifier = Modifier.size(30.dp),
                                                contentScale = ContentScale.Fit
                                            )
                                            Spacer(Modifier.width(12.dp))
                                        }
                                        Text(
                                            text = channel.name,
                                            color = if (isFocused) Color.Black else Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (isFocused) 1.2f else 1f, label = "iconScale")

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = CircleShape,
        color = if (isFocused) Color.White else Color.Black.copy(0.4f),
        border = if (isFocused) BorderStroke(2.dp, AccentGold) else null,
        modifier = modifier
            .size(48.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (isFocused) Color.Black else Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun AudioTrackSelector(
    viewModel: PlayerViewModel,
    onSelect: () -> Unit
) {
    val tracks by viewModel.audioTracks.collectAsState()
    val firstItemFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (tracks.isNotEmpty()) {
            delay(200)
            firstItemFocusRequester.requestFocus()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            "SELECCIONAR PISTA D'ÀUDIO",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = AccentGold,
            letterSpacing = 2.sp
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            itemsIndexed(tracks) { index, track ->
                val isSelected = track.isSelected
                val interactionSource = remember { MutableInteractionSource() }
                val isFocused by interactionSource.collectIsFocusedAsState()
                val scale by animateFloatAsState(if (isFocused) 1.1f else 1f, label = "trackScale")

                Surface(
                    onClick = {
                        viewModel.selectAudioTrack(track)
                        onSelect()
                    },
                    interactionSource = interactionSource,
                    shape = RoundedCornerShape(12.dp),
                    color = when {
                        isFocused -> Color.White
                        isSelected -> AccentGold
                        else -> Color.White.copy(alpha = 0.1f)
                    },
                    border = if (isFocused) BorderStroke(3.dp, AccentGold) else null,
                    modifier = Modifier
                        .height(56.dp)
                        .graphicsLayer(scaleX = scale, scaleY = scale)
                        .then(if (index == 0) Modifier.focusRequester(firstItemFocusRequester) else Modifier)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = track.label,
                            color = if (isFocused || isSelected) Color.Black else Color.White,
                            fontSize = 15.sp,
                            fontWeight = if (isFocused || isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}