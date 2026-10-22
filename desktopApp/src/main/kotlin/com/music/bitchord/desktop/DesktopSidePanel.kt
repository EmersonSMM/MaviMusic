package com.music.bitchord.desktop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.music.bitchord.ui.player.FullArtworkBlurBackdrop
import com.music.bitchord.ui.player.rememberFullArtworkBlurImage

/** What the column beside the page is showing. */
internal enum class DesktopSidePanel { LYRICS, QUEUE }

/**
 * Apple Music's right-hand sidebar: the lyrics or the queue in an elegant floating
 * rounded glass panel beside the page. The panel has smooth rounded corners, dark
 * ambient backdrop gradient and soft shadows, blending seamlessly into macOS.
 */
@Composable
internal fun DesktopSidePanelColumn(
    panel: DesktopSidePanel?,
    onClose: () -> Unit,
    onExpandImmersive: (() -> Unit)? = null,
    content: @Composable (DesktopSidePanel) -> Unit,
) {
    // Held through the exit, so the column slides out with what it had in it rather than empty.
    var shown by remember { mutableStateOf(panel) }
    if (panel != null) shown = panel
    AnimatedVisibility(
        visible = panel != null,
        enter = expandHorizontally(tween(SLIDE_MS, easing = FastOutSlowInEasing), expandFrom = Alignment.Start) +
            fadeIn(tween(SLIDE_MS)),
        exit = shrinkHorizontally(tween(SLIDE_MS, easing = FastOutSlowInEasing), shrinkTowards = Alignment.Start) +
            fadeOut(tween(SLIDE_MS / 2)),
    ) {
        val panelShape = RoundedCornerShape(18.dp)
        Box(
            modifier = Modifier
                .width(SIDE_PANEL_WIDTH)
                .fillMaxHeight()
                .padding(start = 6.dp, end = 12.dp, top = 8.dp, bottom = 12.dp),
        ) {
            Crossfade(
                targetState = shown ?: return@Box,
                animationSpec = tween(CROSSFADE_MS),
                label = "sidePanel",
            ) { which ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .shadow(16.dp, panelShape, spotColor = Color.Black.copy(alpha = 0.5f))
                        .clip(panelShape)
                        .background(Color(0xFF141416).copy(alpha = 0.90f))
                        .border(
                            1.dp,
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.16f),
                                    Color.White.copy(alpha = 0.04f),
                                ),
                            ),
                            panelShape,
                        ),
                ) {
                    // Apple Music authentic frosted glass with smooth artwork tint
                    Box(Modifier.fillMaxSize().desktopChromeGlass())
                    if (which == DesktopSidePanel.LYRICS) {
                        val currentSong = LocalNowPlaying.current
                        val artworkUrl = currentSong?.thumbnailUrl
                        if (artworkUrl != null) {
                            val blurImage = rememberFullArtworkBlurImage(imageUrl = artworkUrl)
                            FullArtworkBlurBackdrop(
                                image = blurImage,
                                modifier = Modifier.fillMaxSize().alpha(0.35f),
                            )
                            // Soft gradient scrim to maintain contrast and eliminate muddy color
                            Box(
                                Modifier.fillMaxSize().background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.Black.copy(alpha = 0.35f),
                                            Color.Black.copy(alpha = 0.75f),
                                        ),
                                    ),
                                ),
                            )
                        }
                    }
                    Column(Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (which == DesktopSidePanel.LYRICS) HEADER_HEIGHT else QUEUE_HEADER_HEIGHT)
                                .padding(start = 16.dp, end = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = if (which == DesktopSidePanel.LYRICS) {
                                    DesktopStrings["lyrics", "Letras"]
                                } else {
                                    DesktopStrings["queue", "Cola de reproducción"]
                                },
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                            )
                            if (which == DesktopSidePanel.LYRICS && onExpandImmersive != null) {
                                DesktopToolbarButton(
                                    onClick = onExpandImmersive,
                                    size = 28.dp,
                                ) {
                                    Icon(
                                        Icons.Rounded.OpenInFull,
                                        DesktopStrings["fullscreen_lyrics", "Pantalla completa de letras"],
                                        tint = DesktopSecondary,
                                        modifier = Modifier.size(15.dp),
                                    )
                                }
                                Spacer(Modifier.width(4.dp))
                            }
                            DesktopToolbarButton(onClick = onClose, size = 28.dp) {
                                Icon(
                                    Icons.Rounded.Close,
                                    DesktopStrings["close", "Cerrar"],
                                    tint = DesktopSecondary,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(
                                    start = if (which == DesktopSidePanel.LYRICS) 14.dp else 12.dp,
                                    end = if (which == DesktopSidePanel.LYRICS) 14.dp else 4.dp,
                                    bottom = 12.dp,
                                ),
                        ) {
                            content(which)
                        }
                    }
                }
            }
        }
    }
}

private val SIDE_PANEL_WIDTH = 380.dp
private val HEADER_HEIGHT = 46.dp
private val QUEUE_HEADER_HEIGHT = 30.dp
private const val SLIDE_MS = 260
private const val CROSSFADE_MS = 180
