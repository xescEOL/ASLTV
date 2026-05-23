package com.xesc.asltv.ui.components



import androidx.compose.animation.*

import androidx.compose.animation.core.*

import androidx.compose.foundation.*

import androidx.compose.foundation.layout.*

import androidx.compose.runtime.mutableStateOf

import androidx.compose.runtime.remember

import androidx.compose.runtime.getValue

import androidx.compose.runtime.setValue

import androidx.compose.foundation.shape.CircleShape

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.foundation.interaction.MutableInteractionSource

import androidx.compose.foundation.interaction.collectIsFocusedAsState

import androidx.compose.foundation.interaction.collectIsPressedAsState

import androidx.compose.ui.graphics.graphicsLayer

import androidx.compose.foundation.interaction.collectIsFocusedAsState

import androidx.compose.foundation.interaction.collectIsPressedAsState

import androidx.compose.ui.graphics.graphicsLayer

import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.filled.*

import androidx.compose.material.icons.outlined.*

import androidx.compose.material3.*

import androidx.compose.runtime.*

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.draw.clip

import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.graphics.vector.ImageVector

import androidx.compose.ui.layout.ContentScale

import androidx.compose.ui.res.painterResource

import androidx.compose.ui.text.font.FontWeight

import com.xesc.asltv.R

import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.ui.unit.dp

import androidx.compose.ui.unit.sp

import androidx.navigation.NavController

import androidx.navigation.compose.currentBackStackEntryAsState

import coil.compose.AsyncImage

import com.xesc.asltv.data.local.entity.EpgProgramEntity

import com.xesc.asltv.data.model.*

import com.xesc.asltv.ui.navigation.Screen

import com.xesc.asltv.ui.themes.*

import com.xesc.asltv.utils.FlagHelper

import java.text.SimpleDateFormat

import java.util.*



// ══════════════════════════════════════════════════════════════════════════════

// SIDE NAV — logo + nav items (reloj movido a MainActivity)

// ══════════════════════════════════════════════════════════════════════════════



@Composable

fun SideNav(navController: NavController, isTV: Boolean) {

    val navBackStackEntry by navController.currentBackStackEntryAsState()

    val currentRoute = navBackStackEntry?.destination?.route



    val navItems = listOf(

        Triple(Screen.Channels, Icons.Default.Tv,       "Canales"),

        Triple(Screen.Apks,     Icons.Default.Download, "Aplicaciones"),

        Triple(Screen.Settings, Icons.Default.Settings, "Ajustes")

    )



    Column(

        modifier = Modifier

            .width(if (isTV) 200.dp else 180.dp)

            .fillMaxHeight()

            .background(SidebarBg),

        horizontalAlignment = Alignment.Start

    ) {

        // ── Logo ──────────────────────────────────────────────────────────────

        Box(

            modifier = Modifier

                .fillMaxWidth()

                .height(96.dp)

                .background(

                    Brush.verticalGradient(

                        listOf(Color(0xFF111111), SidebarBg)

                    )

                ),

            contentAlignment = Alignment.CenterStart

        ) {

            Image(

                painter = painterResource(id = R.drawable.logo),

                contentDescription = "Logo",

                modifier = Modifier.padding(horizontal = 24.dp).height(40.dp),

                contentScale = ContentScale.Fit

            )

        }



        // Separador

        Box(

            modifier = Modifier

                .fillMaxWidth()

                .height(1.dp)

                .background(DividerColor)

        )



        Spacer(Modifier.height(16.dp))



        // ── Items de navegación ───────────────────────────────────────────────

        navItems.forEach { (screen, icon, label) ->

            val isSelected = currentRoute == screen.route

            SideNavItem(

                icon = icon,

                label = label,

                isSelected = isSelected,

                onClick = {

                    if (!isSelected) navController.navigate(screen.route) {

                        popUpTo(Screen.Channels.route)

                        launchSingleTop = true

                    }

                }

            )

        }

    }

}



@Composable

private fun SideNavItem(

    icon: androidx.compose.ui.graphics.vector.ImageVector,

    label: String,

    isSelected: Boolean,

    onClick: () -> Unit

) {

    val textColor by animateColorAsState(

        targetValue = if (isSelected) Color.Black else TextSecondary,

        animationSpec = tween(200),

        label = "nav_text"

    )

    val iconColor by animateColorAsState(

        targetValue = if (isSelected) AccentGold else NavIconInactive,

        animationSpec = tween(200),

        label = "nav_icon"

    )



    Row(

        modifier = Modifier

            .fillMaxWidth()

            .clickable(onClick = onClick)

            .padding(vertical = 2.dp),

        verticalAlignment = Alignment.CenterVertically

    ) {

        // Barra indicadora lateral

        Box(

            modifier = Modifier

                .width(3.dp)

                .height(42.dp)

                .background(

                    if (isSelected) AccentGold else Color.Transparent

                )

        )



        Spacer(Modifier.width(18.dp))



        Icon(

            imageVector = icon,

            contentDescription = label,

            tint = iconColor,

            modifier = Modifier.size(20.dp)

        )



        Spacer(Modifier.width(14.dp))



        Text(

            text = label,

            fontSize = 15.sp,

            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,

            color = textColor,

            maxLines = 1

        )

    }

}



// ══════════════════════════════════════════════════════════════════════════════

// MIRROR DIALOG

// ══════════════════════════════════════════════════════════════════════════════



@Composable

fun MirrorDialog(
    mirrorGroup: MirrorGroup,
    onSelect: (String, String) -> Unit, // URL, Name
    onDismiss: () -> Unit
) {

    val channelName = FlagHelper.nameWithoutFlag(mirrorGroup.name)



    AlertDialog(

        onDismissRequest = onDismiss,

        containerColor = DialogBg,

        shape = RoundedCornerShape(20.dp),

        title = {

            Column {

                Text(

                    text = "SELECCIONAR SENYAL",

                    fontSize = 10.sp,

                    fontWeight = FontWeight.Bold,

                    color = TextSecondary,

                    letterSpacing = 1.5.sp

                )

                Text(

                    text = channelName,

                    fontSize = 18.sp,

                    fontWeight = FontWeight.Bold,

                    color = Color.White

                )

            }

        },

        text = {

            Column(

                modifier = Modifier

                    .fillMaxWidth()

                    .heightIn(max = 400.dp)

                    .verticalScroll(rememberScrollState()),

                verticalArrangement = Arrangement.spacedBy(8.dp)

            ) {

                mirrorGroup.stations.forEachIndexed { index, station ->

                    val normalized = if (station.url.startsWith("acestream://"))

                        station.url else "acestream://${station.url}"



                    Row(

                        modifier = Modifier

                            .fillMaxWidth()

                            .clip(RoundedCornerShape(10.dp))

                            .background(SurfaceCard)

                            .border(1.dp, CardBorder, RoundedCornerShape(10.dp))

                            .clickable { 
                                val signalName = if (mirrorGroup.stations.size > 1) {
                                    "$channelName | Senyal ${index + 1}"
                                } else {
                                    channelName
                                }
                                onSelect(normalized, signalName) 
                            }

                            .padding(14.dp),

                        verticalAlignment = Alignment.CenterVertically,

                        horizontalArrangement = Arrangement.spacedBy(14.dp)

                    ) {

                        // Número de señal

                        Box(

                            modifier = Modifier

                                .size(34.dp)

                                .clip(CircleShape)

                                .background(

                                    Brush.linearGradient(listOf(AccentGold, AccentGoldDim))

                                ),

                            contentAlignment = Alignment.Center

                        ) {

                            Text(

                                "${index + 1}",

                                color = Color.Black,

                                fontWeight = FontWeight.Bold,

                                fontSize = 14.sp

                            )

                        }

                        Column(modifier = Modifier.weight(1f)) {

                            Text(

                                "Senyal ${index + 1}",

                                color = Color.White,

                                fontWeight = FontWeight.SemiBold,

                                fontSize = 13.sp

                            )

                            Text(

                                normalized.removePrefix("acestream://").take(34) + "…",

                                color = TextSecondary,

                                fontSize = 10.sp,

                                maxLines = 1

                            )

                        }

                        Icon(Icons.Default.PlayArrow, null, tint = AccentGold, modifier = Modifier.size(20.dp))

                    }

                }

            }

        },

        confirmButton = {},

        dismissButton = {

            TextButton(onClick = onDismiss) {

                Text("Cancelar", color = TextSecondary)

            }

        }

    )

}



// ══════════════════════════════════════════════════════════════════════════════

// EPG BAR

// ══════════════════════════════════════════════════════════════════════════════



@Composable

fun EpgBar(

    currentProgram: EpgProgramEntity?,

    nextProgram: EpgProgramEntity?,

    modifier: Modifier = Modifier

) {

    if (currentProgram == null && nextProgram == null) return



    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }



    Row(

        modifier = modifier

            .fillMaxWidth()

            .background(

                Brush.verticalGradient(listOf(Color.Transparent, Color(0xF5080808)))

            )

            .padding(horizontal = 20.dp, vertical = 14.dp),

        verticalAlignment = Alignment.CenterVertically,

        horizontalArrangement = Arrangement.spacedBy(20.dp)

    ) {

        currentProgram?.let { prog ->

            Row(

                verticalAlignment = Alignment.CenterVertically,

                horizontalArrangement = Arrangement.spacedBy(8.dp)

            ) {

                Box(

                    modifier = Modifier

                        .clip(RoundedCornerShape(4.dp))

                        .background(LiveDot)

                        .padding(horizontal = 6.dp, vertical = 2.dp)

                ) {

                    Text(

                        "ARA",

                        fontSize = 8.sp,

                        color = Color.White,

                        fontWeight = FontWeight.ExtraBold,

                        letterSpacing = 0.5.sp

                    )

                }

                Column {

                    Text(

                        prog.title,

                        fontSize = 13.sp,

                        color = Color.White,

                        fontWeight = FontWeight.SemiBold,

                        maxLines = 1

                    )

                    Text(

                        "${timeFmt.format(Date(prog.startTime))} – ${timeFmt.format(Date(prog.endTime))}",

                        fontSize = 10.sp,

                        color = TextSecondary

                    )

                }

            }

        }



        nextProgram?.let { prog ->

            Box(

                modifier = Modifier

                    .size(width = 1.dp, height = 32.dp)

                    .background(DividerColor)

            )

            Row(

                verticalAlignment = Alignment.CenterVertically,

                horizontalArrangement = Arrangement.spacedBy(8.dp)

            ) {

                Icon(

                    Icons.Default.SkipNext,

                    null,

                    tint = TextSecondary,

                    modifier = Modifier.size(14.dp)

                )

                Column {

                    Text(

                        prog.title,

                        fontSize = 13.sp,

                        color = TextSecondary,

                        fontWeight = FontWeight.Medium,

                        maxLines = 1

                    )

                    Text(

                        timeFmt.format(Date(prog.startTime)),

                        fontSize = 10.sp,

                        color = NavIconInactive

                    )

                }

            }

        }

    }

}



// ══════════════════════════════════════════════════════════════════════════════

// SETTINGS COMPONENTS

// ══════════════════════════════════════════════════════════════════════════════



@Composable

fun SectionTitle(title: String) {

    Text(

        text = title,

        fontSize = 10.sp,

        fontWeight = FontWeight.ExtraBold,

        color = AccentGold,

        letterSpacing = 2.sp,

        modifier = Modifier.padding(top = 24.dp, bottom = 10.dp)

    )

}



@Composable

public fun ListModeCard(title: String, subtitle: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier) {

    Card(

        onClick = onClick, modifier = modifier, shape = RoundedCornerShape(12.dp),

        colors = CardDefaults.cardColors(containerColor = if (isSelected) AccentGold.copy(0.1f) else SurfaceCard),

        border = androidx.compose.foundation.BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) AccentGold else DividerColor)

    ) {

        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {

                Icon(icon, null, tint = if (isSelected) AccentGold else TextMuted, modifier = Modifier.size(20.dp))

                if (isSelected) Icon(Icons.Default.CheckCircle, null, tint = AccentGold, modifier = Modifier.size(18.dp))

            }

            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (isSelected) Color.White else TextPrimary)

            Text(subtitle, fontSize = 11.sp, color = TextMuted, lineHeight = 14.sp)

        }

    }

}



@Composable

fun ListItem(

    name: String,

    author: String,

    isActive: Boolean,

    lastUpdated: Long,

    epgUrl: String? = null,

    onSelect: () -> Unit,

    onDelete: () -> Unit

) {

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (isFocused) 1.03f else 1f, label = "cardScale")


    Row(

        modifier = Modifier.fillMaxWidth(),

        verticalAlignment = Alignment.CenterVertically,

        horizontalArrangement = Arrangement.spacedBy(8.dp)

    ) {

        // ── Card principal ────────────────────────────────────────────────────

        Card(

            modifier = Modifier

                .weight(1f)
                .graphicsLayer(scaleX = scale, scaleY = scale)
                .border(

                    width = if (isFocused) 2.5.dp else if (isActive) 1.5.dp else 1.dp,

                    color = if (isFocused) Color.White else if (isActive) AccentGold.copy(0.7f) else CardBorder,

                    shape = RoundedCornerShape(14.dp)

                )

                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onSelect
                ),

            shape = RoundedCornerShape(14.dp),

            colors = CardDefaults.cardColors(

                containerColor = when {
                    isFocused -> SurfaceElevated
                    isActive -> AccentGold.copy(0.08f)
                    else -> SurfaceCard
                }

            )

        ) {

            Row(

                modifier = Modifier.padding(16.dp),

                verticalAlignment = Alignment.CenterVertically,

                horizontalArrangement = Arrangement.spacedBy(14.dp)

            ) {

                Box(

                    modifier = Modifier

                        .size(42.dp)

                        .clip(RoundedCornerShape(10.dp))

                        .background(if (isActive) AccentGold.copy(0.15f) else SurfaceElevated),

                    contentAlignment = Alignment.Center

                ) {

                    Icon(

                        imageVector = if (isActive) Icons.Default.PlayCircle else Icons.Outlined.RadioButtonUnchecked,

                        contentDescription = null,

                        tint = if (isActive) AccentGold else NavIconInactive,

                        modifier = Modifier.size(22.dp)

                    )

                }

                Column(modifier = Modifier.weight(1f)) {

                    Text(

                        name,

                        fontSize = 14.sp,

                        fontWeight = FontWeight.SemiBold,

                        color = if (isActive) Color.White else TextPrimary

                    )

                    Text("por $author", fontSize = 11.sp, color = TextSecondary)

                    Text(

                        "Actualizado: ${dateFormat.format(Date(lastUpdated))}",

                        fontSize = 10.sp,

                        color = NavIconInactive

                    )

                    epgUrl?.let {

                        Text(

                            "EPG: $it",

                            fontSize = 9.sp,

                            color = AccentGold.copy(0.7f),

                            maxLines = 1

                        )

                    }

                }

                if (isActive) {

                    Box(

                        modifier = Modifier

                            .clip(RoundedCornerShape(6.dp))

                            .background(AccentGold)

                            .padding(horizontal = 10.dp, vertical = 4.dp)

                    ) {

                        Text(

                            "ACTIVA",

                            fontSize = 9.sp,

                            color = Color.Black,

                            fontWeight = FontWeight.ExtraBold,

                            letterSpacing = 0.5.sp

                        )

                    }

                }

            }

        }



        // ── Botón eliminar (fuera de la card, accesible con D-pad en TV) ─────

        val deleteInteractionSource = remember { MutableInteractionSource() }
        val isDeleteFocused by deleteInteractionSource.collectIsFocusedAsState()
        val deleteScale by animateFloatAsState(if (isDeleteFocused) 1.1f else 1f, label = "deleteScale")

        IconButton(

            onClick = { showDeleteConfirm = true },
            interactionSource = deleteInteractionSource,

            modifier = Modifier

                .size(48.dp)
                .graphicsLayer(scaleX = deleteScale, scaleY = deleteScale)
                .clip(RoundedCornerShape(12.dp))

                .background(if (isDeleteFocused) Color.White else SurfaceCard)

                .border(
                    width = if (isDeleteFocused) 2.dp else 1.dp,
                    color = if (isDeleteFocused) ErrorRed else CardBorder,
                    shape = RoundedCornerShape(12.dp)
                )

        ) {

            Icon(

                Icons.Default.DeleteOutline,

                contentDescription = "Eliminar lista",

                tint = if (isDeleteFocused) ErrorRed else ErrorRed.copy(0.75f),

                modifier = Modifier.size(20.dp)

            )

        }

    }



    if (showDeleteConfirm) {

        AlertDialog(

            onDismissRequest = { showDeleteConfirm = false },

            containerColor = DialogBg,

            shape = RoundedCornerShape(20.dp),

            title = {

                Text("Eliminar llista", color = Color.White, fontWeight = FontWeight.Bold)

            },

            text = {

                Text(

                    "Segur que vols eliminar \"$name\"? Aquesta acció no es pot desfer.",

                    color = TextSecondary,

                    fontSize = 13.sp

                )

            },

            confirmButton = {

                Button(

                    onClick = { showDeleteConfirm = false; onDelete() },

                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),

                    shape = RoundedCornerShape(10.dp)

                ) {

                    Text("Eliminar", color = Color.White, fontWeight = FontWeight.SemiBold)

                }

            },

            dismissButton = {

                SecondaryButton(text = "Cancelar", onClick = { showDeleteConfirm = false })

            }

        )

    }

}



@Composable

fun PrimaryButton(

    text: String,

    onClick: () -> Unit,

    modifier: Modifier = Modifier,

    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,

    enabled: Boolean = true,

    isSelected: Boolean = false

) {

    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "buttonScale"
    )

    Button(

        onClick = onClick,

        enabled = enabled,
        interactionSource = interactionSource,
        modifier = modifier.graphicsLayer(scaleX = scale, scaleY = scale),

        shape = RoundedCornerShape(10.dp),

        colors = ButtonDefaults.buttonColors(

            containerColor = when {
                !enabled -> SurfaceElevated
                isFocused -> Color.White // Blanco brillante al enfocar
                isSelected -> Color(0xFFFF6B35)
                isPressed -> Color(0xFFFFD700)
                else -> AccentGold
            },

            contentColor = Color.Black,

            disabledContainerColor = SurfaceElevated,

            disabledContentColor = NavIconInactive

        ),

        border = if (isFocused) BorderStroke(3.dp, AccentGold) else null,

        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 13.dp)

    ) {

        if (icon != null) {

            Icon(icon, null, modifier = Modifier.size(16.dp), tint = Color.Black)

            Spacer(Modifier.width(8.dp))

        }

        Text(text, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)

    }

}



@Composable

fun SecondaryButton(

    text: String,

    onClick: () -> Unit,

    modifier: Modifier = Modifier,

    icon: androidx.compose.ui.graphics.vector.ImageVector? = null

) {

    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (isFocused) 1.05f else 1f, label = "secScale")

    OutlinedButton(

        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier.graphicsLayer(scaleX = scale, scaleY = scale),

        shape = RoundedCornerShape(10.dp),

        border = BorderStroke(if (isFocused) 2.dp else 1.dp, if (isFocused) Color.White else CardBorder),

        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isFocused) Color.White.copy(0.1f) else Color.Transparent,
            contentColor = if (isFocused) Color.White else TextPrimary
        ),

        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 13.dp)

    ) {

        if (icon != null) {

            Icon(icon, null, modifier = Modifier.size(16.dp), tint = TextPrimary)

            Spacer(Modifier.width(8.dp))

        }

        Text(text, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = TextPrimary)

    }

}



@Composable

fun AddListDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {

    var url by remember { mutableStateOf("") }

    AlertDialog(

        onDismissRequest = onDismiss,

        containerColor = DialogBg,

        shape = RoundedCornerShape(20.dp),

        title = {

            Text("Afegir llista de canals", color = Color.White, fontWeight = FontWeight.Bold)

        },

        text = {

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                Text("Introdueix la URL del fitxer JSON:", fontSize = 13.sp, color = TextSecondary)

                OutlinedTextField(

                    value = url,

                    onValueChange = { url = it },

                    placeholder = {

                        Text(

                            "exemple.com/llista.json",

                            color = NavIconInactive,

                            fontSize = 13.sp

                        )

                    },

                    modifier = Modifier.fillMaxWidth(),

                    singleLine = true,

                    shape = RoundedCornerShape(10.dp),

                    colors = OutlinedTextFieldDefaults.colors(

                        focusedBorderColor = AccentGold,

                        unfocusedBorderColor = CardBorder,

                        focusedTextColor = Color.White,

                        unfocusedTextColor = Color.White,

                        cursorColor = AccentGold

                    )

                )

            }

        },

        confirmButton = {

            PrimaryButton(

                "Afegir",

                onClick = { if (url.isNotBlank()) onConfirm(url.trim()) },

                enabled = url.isNotBlank()

            )

        },

        dismissButton = { SecondaryButton("Cancelar", onClick = onDismiss) }

    )

}



@Composable

fun AppUpdateDialog(

    newVersion: String,

    description: String?,

    onInstall: () -> Unit,

    onDismiss: () -> Unit

) {

    AlertDialog(

        onDismissRequest = onDismiss,

        containerColor = DialogBg,

        shape = RoundedCornerShape(20.dp),

        title = {

            Column {

                Text(

                    text = "ACTUALIZACIÓ DISPONIBLE",

                    fontSize = 10.sp,

                    fontWeight = FontWeight.Bold,

                    color = AccentGold,

                    letterSpacing = 1.5.sp

                )

                Text(

                    text = "Nova versió: $newVersion",

                    fontSize = 18.sp,

                    fontWeight = FontWeight.Bold,

                    color = Color.White

                )

            }

        },

        text = {

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                Text(

                    "Hi ha una versió més recent de l'aplicació disponible.",

                    fontSize = 13.sp,

                    color = TextSecondary

                )

                description?.let {

                    Text(

                        it,

                        fontSize = 12.sp,

                        color = TextPrimary,

                        lineHeight = 16.sp

                    )

                }

            }

        },

        confirmButton = {

            PrimaryButton(

                "Instal·la ara",

                onClick = {

                    onInstall()

                    onDismiss()

                }

            )

        },

        dismissButton = {

            SecondaryButton("En un altre moment", onClick = onDismiss)

        }

    )

}



// ══════════════════════════════════════════════════════════════════════════════

// APK CARD

// ══════════════════════════════════════════════════════════════════════════════



@Composable

fun ApkCard(

    apk: Apk,

    downloadState: DownloadState? = null,

    onDownload: () -> Unit = {},

    onInstall: () -> Unit = {}

) {

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(downloadState) {
        if (downloadState is DownloadState.Done) {
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    Card(

        modifier = Modifier.fillMaxWidth(),

        shape = RoundedCornerShape(16.dp),

        colors = CardDefaults.cardColors(containerColor = SurfaceCard),

        border = BorderStroke(1.dp, CardBorder)

    ) {

        Column(

            modifier = Modifier.padding(18.dp),

            verticalArrangement = Arrangement.spacedBy(14.dp)

        ) {

            Row(

                verticalAlignment = Alignment.CenterVertically,

                horizontalArrangement = Arrangement.spacedBy(14.dp)

            ) {

                Box(

                    modifier = Modifier

                        .size(54.dp)

                        .clip(RoundedCornerShape(14.dp))

                        .background(SurfaceElevated)

                        .border(1.dp, CardBorder, RoundedCornerShape(14.dp))

                ) {

                    if (apk.image != null) {

                        AsyncImage(

                            model = apk.image,

                            contentDescription = apk.name,

                            modifier = Modifier.fillMaxSize().padding(8.dp)

                        )

                    } else {

                        Icon(

                            Icons.Default.Android,

                            null,

                            tint = AccentGold,

                            modifier = Modifier.align(Alignment.Center).size(28.dp)

                        )

                    }

                }

                Column(modifier = Modifier.weight(1f)) {

                    Text(

                        apk.name,

                        fontSize = 15.sp,

                        fontWeight = FontWeight.SemiBold,

                        color = Color.White

                    )

                    Row(

                        horizontalArrangement = Arrangement.spacedBy(6.dp),

                        verticalAlignment = Alignment.CenterVertically

                    ) {

                        apk.version?.let {

                            Badge(containerColor = SurfaceElevated) {

                                Text("v$it", fontSize = 10.sp, color = TextSecondary)

                            }

                        }

                        apk.armv?.let {

                            Badge(containerColor = AccentGold.copy(0.15f)) {

                                Text("ARMv$it", fontSize = 10.sp, color = AccentGold)

                            }

                        }

                    }

                }

            }



            apk.description?.let {

                Text(it, fontSize = 12.sp, color = TextSecondary, lineHeight = 17.sp)

            }



            when (val state = downloadState) {

                is DownloadState.Downloading -> {

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {

                        Row(

                            modifier = Modifier.fillMaxWidth(),

                            horizontalArrangement = Arrangement.SpaceBetween

                        ) {

                            Text("Baixant…", fontSize = 11.sp, color = AccentGold)

                            Text("${(state.progress * 100).toInt()}%", fontSize = 11.sp, color = AccentGold)

                        }

                        LinearProgressIndicator(

                            progress = { state.progress },

                            modifier = Modifier

                                .fillMaxWidth()

                                .clip(CircleShape)

                                .height(4.dp),

                            color = AccentGold,

                            trackColor = SurfaceElevated

                        )

                    }

                }

                is DownloadState.Error -> {

                    Text(

                        "Error: ${state.message}",

                        fontSize = 11.sp,

                        color = ErrorRed

                    )

                }

                else -> {}

            }



            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                if (downloadState !is DownloadState.Done) {

                    PrimaryButton(

                        text = if (downloadState is DownloadState.Downloading) "Baixant…" else "Descarregar",

                        onClick = onDownload,

                        enabled = downloadState == null || downloadState is DownloadState.Error,

                        icon = Icons.Default.Download,

                        modifier = Modifier.weight(1f).focusRequester(focusRequester)

                    )

                }

                if (downloadState is DownloadState.Done) {

                    PrimaryButton(

                        "Instal·lar",

                        onClick = onInstall,

                        icon = Icons.Default.InstallMobile,

                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),

                        isSelected = true // APK is downloaded, so install button is selected

                    )

                }

            }

        }

    }

}



// ══════════════════════════════════════════════════════════════════════════════

// DOWNLOAD STATE

// ══════════════════════════════════════════════════════════════════════════════



sealed class DownloadState {

    object Idle : DownloadState()

    data class Downloading(val progress: Float) : DownloadState()

    data class Done(val file: java.io.File) : DownloadState()

    data class Error(val message: String) : DownloadState()

}