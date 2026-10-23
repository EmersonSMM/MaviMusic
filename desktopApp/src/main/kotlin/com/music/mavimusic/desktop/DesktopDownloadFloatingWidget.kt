package com.music.mavimusic.desktop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Floating download status card in the bottom-right corner of the window.
 * Displays active downloads, real-time progress, and smoothly fades out on completion.
 * Can be toggled on/off in Appearance / Downloads Settings.
 */
@Composable
internal fun DesktopDownloadFloatingWidget(
    modifier: Modifier = Modifier,
) {
    val enabled by DesktopAppearanceSettings.floatingDownloadStatus.collectAsState()
    if (!enabled) return

    val activeMap by DesktopDownloadQueue.active.collectAsState()
    val receipts by DesktopDownloadHistory.recent.collectAsState()
    var lastCompletionAtStart by remember { mutableStateOf<Long?>(null) }
    val activeItems = activeMap.values.toList()

    var wasActive by remember { mutableStateOf(false) }
    var showCompleted by remember { mutableStateOf(false) }
    var dismissedManually by remember { mutableStateOf(false) }

    LaunchedEffect(activeItems.isNotEmpty()) {
        if (activeItems.isNotEmpty()) {
            if (!wasActive) lastCompletionAtStart = receipts.firstOrNull()?.completedAt
            wasActive = true
            showCompleted = false
            dismissedManually = false
        } else if (wasActive) {
            // Cancelling the queue is not a completed download.
            showCompleted = receipts.firstOrNull()?.completedAt?.let { it != lastCompletionAtStart } == true
            wasActive = false
            delay(3500)
            showCompleted = false
        }
    }

    val isVisible = !dismissedManually && (activeItems.isNotEmpty() || showCompleted)

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { it / 2 },
            animationSpec = tween(360, easing = FastOutSlowInEasing),
        ) + fadeIn(tween(360)),
        exit = slideOutVertically(
            targetOffsetY = { it / 2 },
            animationSpec = tween(450, easing = FastOutSlowInEasing),
        ) + fadeOut(tween(450)),
        modifier = modifier,
    ) {
        val shape = RoundedCornerShape(14.dp)
        val activeItem = activeItems.firstOrNull { it.state is DesktopDownloadState.Running }
            ?: activeItems.firstOrNull()

        val runningFraction = (activeItem?.state as? DesktopDownloadState.Running)?.fraction ?: 0f

        Box(
            modifier = Modifier
                .widthIn(min = 280.dp, max = 340.dp)
                .shadow(16.dp, shape, clip = false)
                .clip(shape)
                .background(Color(0xE61E1E24))
                .border(1.dp, Color.White.copy(alpha = 0.16f), shape)
                .desktopBarGlass(shape)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Status icon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (showCompleted) Color(0x3330D158) else Color(0x330A84FF),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (showCompleted) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Completado",
                            tint = Color(0xFF30D158),
                            modifier = Modifier.size(22.dp),
                        )
                    } else if (activeItem?.state is DesktopDownloadState.Running) {
                        CircularProgressIndicator(
                            progress = { runningFraction },
                            color = Color(0xFF0A84FF),
                            trackColor = Color.White.copy(alpha = 0.15f),
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(24.dp),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = "Descargando",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                // Song info & details
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    if (showCompleted) {
                        Text(
                            text = "Descarga completada",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Canciones guardadas en tu biblioteca",
                            color = DesktopSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    } else {
                        val count = activeItems.size
                        Text(
                            text = (activeItem?.state as? DesktopDownloadState.Running)?.progress?.stage?.label
                                ?.let { if (count > 1) "$it ($count restantes)" else it } ?: "En cola ($count)",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = activeItem?.song?.title ?: "Preparando descarga...",
                            color = DesktopSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Spacer(Modifier.height(5.dp))

                        // Clean progress bar without Material 3 stop indicator dot
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(1.5.dp))
                                .background(Color.White.copy(alpha = 0.14f)),
                        ) {
                            val fraction = if (activeItem?.state is DesktopDownloadState.Running) {
                                runningFraction.coerceIn(0f, 1f)
                            } else {
                                0.35f
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(1.5.dp))
                                    .background(Color.White),
                            )
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))

                // Dismiss button
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .clickable { dismissedManually = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Cerrar",
                        tint = DesktopSecondary,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
        }
    }
}
