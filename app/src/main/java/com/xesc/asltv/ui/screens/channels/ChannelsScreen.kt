package com.xesc.asltv.ui.screens.channels

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.xesc.asltv.ui.navigation.Screen
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.xesc.asltv.data.local.entity.EpgProgramEntity
import com.xesc.asltv.data.model.Group
import com.xesc.asltv.data.model.MirrorGroup
import com.xesc.asltv.ui.components.AppUpdateDialog
import com.xesc.asltv.ui.components.MirrorDialog
import com.xesc.asltv.ui.themes.*
import com.xesc.asltv.utils.FlagHelper
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChannelsScreen(navController: NavHostController, isTV: Boolean) {
    val viewModel: ChannelsViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { navEvent ->
            val url = navEvent.url
            val name = navEvent.name
            val logo = navEvent.logo
            
            // Naveguem al reproductor integrat per a TOTES les fonts (inclòs AceStream)
            // per assegurar que es visualitzi la nostra UI (TopBar, Logo, etc.)
            navController.navigate(Screen.Player.createRoute(url, name, logo)) {
                // Evitem acumular pantalles del reproductor al backstack
                popUpTo(Screen.Channels.route) {
                    saveState = true
                }
                launchSingleTop = true
            }
        }
    }

    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    BackHandler {
        if (uiState.selectedGroup != null) {
            viewModel.clearGroup()
        } else {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastBackPressTime < 2000) {
                (context as? android.app.Activity)?.finish()
            } else {
                lastBackPressTime = currentTime
                Toast.makeText(context, "Prem de nou per sortir", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(BackgroundDark)) {
        AnimatedContent(
            targetState = uiState.selectedGroup,
            transitionSpec = { fadeIn(tween(280)) togetherWith fadeOut(tween(200)) },
            label = "screen_transition"
        ) { selectedGroup ->
            if (selectedGroup == null) {
                HomeView(
                    groups = uiState.groups,
                    bannerImage = uiState.bannerImage,
                    onGroupSelected = viewModel::selectGroup
                )
            } else {
                ChannelsView(
                    groupName = selectedGroup,
                    groups = uiState.groups,
                    channels = uiState.channels,
                    epgData = uiState.epgData,
                    focusedChannel = uiState.focusedChannel,
                    onBack = viewModel::clearGroup,
                    onChannelClick = viewModel::onChannelClicked,
                    onChannelFocused = viewModel::selectChannel,
                    onGroupSelected = viewModel::selectGroup
                )
            }
        }
    }

    if (uiState.showMirrorDialog) {
        MirrorDialog(
            mirrorGroup = com.xesc.asltv.data.model.MirrorGroup(uiState.currentMirrors),
            onSelect = { url, name -> 
                viewModel.dismissMirrors()
                val logo = uiState.focusedChannel?.image
                viewModel.playChannel(url, name, logo)
            },
            onDismiss = viewModel::dismissMirrors
        )
    }

    // App Update Dialog
    uiState.appUpdate?.let { appUpdate ->
        AppUpdateDialog(
            newVersion = appUpdate.version,
            description = appUpdate.description,
            onInstall = {
                viewModel.installAppUpdate(context, appUpdate)
            },
            onDismiss = {
                viewModel.dismissAppUpdate()
            }
        )
    }
}

@Composable
private fun HomeView(
    groups: List<Pair<Group, List<MirrorGroup>>>,
    bannerImage: String?,
    onGroupSelected: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (!bannerImage.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                AsyncImage(
                    model = bannerImage,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, BackgroundDark)
                            )
                        )
                )
            }
        }

        Text(
            text = "CATEGORIES",
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            color = NavIconInactive,
            letterSpacing = 2.5.sp,
            modifier = Modifier.padding(
                start = 24.dp,
                end = 24.dp,
                top = if (bannerImage.isNullOrBlank()) 20.dp else 8.dp,
                bottom = 8.dp
            )
        )

        if (groups.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.PlaylistAdd,
                        null,
                        tint = NavIconInactive,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        "No hi ha llistes actives",
                        color = TextSecondary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Vés a Paràmetres → Afegeix una llista per començar",
                        color = NavIconInactive,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            val chunkedGroups = remember(groups) { groups.chunked(2) }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(chunkedGroups) { rowGroups ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        rowGroups.forEach { pair ->
                            Box(modifier = Modifier.weight(1f)) {
                                CategoryBanner(
                                    group = pair.first,
                                    channelCount = pair.second.size,
                                    onClick = { onGroupSelected(pair.first.name) }
                                )
                            }
                        }
                        if (rowGroups.size < 2) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryBanner(
    group: Group,
    channelCount: Int,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val borderWidth by animateDpAsState(
        targetValue = if (isFocused) 2.dp else 0.dp,
        label = "border"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(135.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .clip(RoundedCornerShape(14.dp))
            .then(
                if (isFocused)
                    Modifier.border(borderWidth, AccentGold, RoundedCornerShape(14.dp))
                else Modifier
            )
            .clickable(onClick = onClick)
    ) {
        if (!group.image.isNullOrBlank()) {
            AsyncImage(
                model = group.image,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF1C1A14), Color(0xFF2A2416))
                        )
                    )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to Color.Black.copy(alpha = 0.88f),
                        0.5f to Color.Black.copy(alpha = 0.55f),
                        0.85f to Color.Black.copy(alpha = 0.10f),
                        1f to Color.Transparent
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(horizontal = 26.dp)
        ) {
            Text(
                text = group.name.uppercase(),
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isFocused) AccentGold else Color.White,
                letterSpacing = 0.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = "$channelCount canals",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f),
                fontWeight = FontWeight.Normal
            )
        }

        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color.White.copy(alpha = if (isFocused) 1f else 0.3f),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 22.dp)
                .size(32.dp)
        )
    }
}

@Composable
private fun ChannelsView(
    groupName: String,
    groups: List<Pair<Group, List<MirrorGroup>>>,
    channels: List<MirrorGroup>,
    epgData: Map<String, EpgInfo>,
    focusedChannel: MirrorGroup?,
    onBack: () -> Unit,
    onChannelClick: (MirrorGroup) -> Unit,
    onChannelFocused: (MirrorGroup?) -> Unit,
    onGroupSelected: (String) -> Unit
) {
    val firstItemFocusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    var hasRequestedInitialFocus by remember(groupName) { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(BackgroundDark)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceCard)
                        .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = groupName.uppercase(),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(groups, key = { it.first.name }) { (group, _) ->
                    val isSelected = group.name == groupName
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) AccentGold
                                else SurfaceCard
                            )
                            .border(
                                1.dp,
                                if (isSelected) AccentGold else CardBorder,
                                RoundedCornerShape(20.dp)
                            )
                            .clickable { onGroupSelected(group.name) }
                            .padding(horizontal = 16.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = group.name,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.Black else TextSecondary
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(DividerColor)
            )
        }

        Row(modifier = Modifier
            .fillMaxWidth()
            .weight(1f)) {
            if (channels.isEmpty()) {
                Box(Modifier
                    .weight(0.65f)
                    .fillMaxHeight(), contentAlignment = Alignment.Center) {
                    Text(
                        "No hi ha canals en aquesta categoria",
                        color = NavIconInactive,
                        fontSize = 14.sp
                    )
                }
            } else {
                val chunkedChannels = remember(channels) { channels.chunked(3) }
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 20.dp, end = 20.dp,
                        top = 16.dp, bottom = 20.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .weight(0.65f)
                        .fillMaxHeight()
                ) {
                    items(chunkedChannels) { rowChannels ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowChannels.forEach { mirrorGroup ->
                                val isFirstItem = channels.indexOf(mirrorGroup) == 0
                                Box(modifier = Modifier.weight(1f)) {
                                    ChannelCard(
                                        mirrorGroup = mirrorGroup,
                                        epgInfo = epgData[mirrorGroup.name],
                                        onClick = { onChannelClick(mirrorGroup) },
                                        onFocused = { onChannelFocused(mirrorGroup) },
                                        modifier = if (isFirstItem) {
                                            Modifier.focusRequester(firstItemFocusRequester)
                                        } else Modifier
                                    )
                                }
                            }
                            repeat(3 - rowChannels.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                LaunchedEffect(channels) {
                    if (channels.isNotEmpty() && !hasRequestedInitialFocus) {
                        try {
                            firstItemFocusRequester.requestFocus()
                            hasRequestedInitialFocus = true
                        } catch (_: Exception) {}
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(0.35f)
                    .fillMaxHeight()
                    .background(SurfaceCard.copy(alpha = 0.3f))
                    .drawWithContent {
                        drawContent()
                        drawLine(
                            color = DividerColor,
                            start = androidx.compose.ui.geometry.Offset(0f, 0f),
                            end = androidx.compose.ui.geometry.Offset(0f, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
            ) {
                ChannelEpgSidebar(focusedChannel, epgData[focusedChannel?.name])
            }
        }
    }
}

@Composable
private fun ChannelEpgSidebar(
    focusedChannel: MirrorGroup?,
    epgInfo: EpgInfo?
) {
    val scrollState = rememberLazyListState()
    
    // Reset scroll when channel changes
    LaunchedEffect(focusedChannel?.name) {
        scrollState.scrollToItem(0)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (focusedChannel == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Selecciona un canal per veure’n la programació.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(20.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {

            Text(
                "PROGRAMACIÓ ${focusedChannel.name}",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = AccentGold,
                letterSpacing = 1.5.sp
            )

            Spacer(Modifier.height(12.dp))

            if (epgInfo == null || (epgInfo.current == null && epgInfo.upcoming.isEmpty())) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No hi ha informació de programació disponible.",
                        color = TextSecondary.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    state = scrollState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    epgInfo.current?.let {
                        item {
                            EpgSidebarItem(it, isCurrent = true)
                        }
                    }
                    items(epgInfo.upcoming.take(20)) {
                        EpgSidebarItem(it, isCurrent = false)
                    }
                    item {
                        Spacer(Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun EpgSidebarItem(program: EpgProgramEntity, isCurrent: Boolean) {
    val timeFormat = remember {
        SimpleDateFormat("HH:mm", Locale.getDefault())
    }

    val startTime = timeFormat.format(Date(program.startTime))
    val endTime = timeFormat.format(Date(program.endTime))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (isCurrent) Modifier
                    .background(AccentGold.copy(alpha = 0.1f))
                    .border(1.dp, AccentGold.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .padding(10.dp)
                else Modifier.padding(horizontal = 4.dp)
            )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(AccentGold)
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = "$startTime - $endTime",
                fontSize = 11.sp,
                color = if (isCurrent) AccentGold else TextSecondary,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
            )
        }

        Spacer(Modifier.height(2.dp))

        Text(
            text = program.title,
            fontSize = 14.sp,
            color = if (isCurrent) Color.White else Color.White.copy(alpha = 0.85f),
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        if (isCurrent && program.description.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = program.description,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 15.sp
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChannelCard(
    mirrorGroup: MirrorGroup,
    epgInfo: EpgInfo?,
    onClick: () -> Unit,
    onFocused: () -> Unit,
    modifier: Modifier = Modifier
) {
    val flag = FlagHelper.extractFlag(mirrorGroup.name)
    val displayName = FlagHelper.nameWithoutFlag(mirrorGroup.name)
    var isFocused by remember { mutableStateOf(false) }

    val shadowElev by animateDpAsState(
        targetValue = if (isFocused) 20.dp else 2.dp,
        label = "shadow"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .onFocusChanged {
                isFocused = it.isFocused
                if (it.isFocused) {
                    onFocused()
                }
            }
            .shadow(
                elevation = shadowElev,
                shape = RoundedCornerShape(16.dp),
                ambientColor = if (isFocused) AccentGold.copy(0.4f) else Color.Black,
                spotColor = if (isFocused) AccentGold.copy(0.3f) else Color.Black
            )
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) AccentGold else CardBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            SubcomposeAsyncImage(
                model = mirrorGroup.image,
                contentDescription = displayName,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.72f)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                loading = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = AccentGold.copy(0.4f),
                            strokeWidth = 2.dp
                        )
                    }
                },
                error = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            displayName.take(3).uppercase(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = NavIconInactive
                        )
                    }
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0xFF0C0C0C), Color(0xFF080808))
                        )
                    )
                    .padding(horizontal = 10.dp, vertical = 9.dp)
            ) {
                Text(
                    text = displayName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                epgInfo?.current?.let { current ->
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(current.title)
                            }
                        },
                        fontSize = 9.sp,
                        color = AccentGold.copy(0.80f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee()
                    )
                }
            }

            if (mirrorGroup.hasMirrors) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(AccentGold)
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        "${mirrorGroup.stations.size}",
                        fontSize = 9.sp,
                        color = Color.Black,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            if (flag != null) {
                Text(
                    text = flag,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(7.dp)
                )
            }
        }
    }
}
