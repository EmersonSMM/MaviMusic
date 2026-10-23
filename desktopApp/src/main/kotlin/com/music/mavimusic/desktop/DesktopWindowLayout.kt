package com.music.mavimusic.desktop

import androidx.compose.runtime.getValue

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@Composable
internal fun DesktopFrame(
    containerColor: Color,
    modifier: Modifier = Modifier,
    /**
     * The open page's own colours, painted across the whole window rather than only the content
     * area, so the title bar, top bar and sidebar take their tint from the page they are framing
     * instead of staying a flat slab beside it.
     */
    /** Receives true when its solid base should be left clear for the system material. */
    backdrop: @Composable (transparentBase: Boolean) -> Unit,
    topBar: @Composable (Boolean) -> Unit,
    sidebar: @Composable () -> Unit,
    bottomBar: @Composable (Boolean) -> Unit,
    /** The column on the far side of the page — the lyrics or the queue, when one is open. */
    trailing: @Composable () -> Unit = {},
    overlay: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    // Held out here rather than inside the constraints box.
    val haze = remember { HazeState() }
    // With a system material active, the window stays clear under its chrome. The app-background
    // preference decides whether the page and side column also stay clear or retain their solid
    // gray ground.
    val material by DesktopWindowBackdrop.active.collectAsState()
    val appBackground by DesktopWindowBackdrop.appBackground.collectAsState()
    val glass = material != DesktopBackdrop.OFF
    val materialBehindApp = glass && appBackground
    val maximized by DesktopWindowMode.maximized.collectAsState()
    val roundCorners = DesktopPlatform.isMac && !maximized
    CompositionLocalProvider(LocalDesktopHaze provides haze) {
        Box(
            modifier
                .fillMaxSize()
                .then(if (roundCorners) Modifier.clip(RoundedCornerShape(10.dp)) else Modifier)
                .then(if (glass) Modifier else Modifier.background(containerColor)),
        ) {
            // Both sources of the same state: the chrome blurs the backdrop behind it, and the
            // floating bottom bar blurs the page scrolling under it.
            if (!glass) Box(Modifier.fillMaxSize().hazeSource(haze)) { backdrop(false) }
            Column(Modifier.fillMaxSize()) {
                // Above everything, and outside the box the rest of the window is drawn in, because
                // that is what a title bar is.
                DesktopTitleBar()
                Box(Modifier.fillMaxWidth().weight(1f)) {
                    BoxWithConstraints(Modifier.fillMaxSize()) {
                        val compact = maxWidth < 820.dp
                        Column(Modifier.fillMaxSize()) {
                            Row(Modifier.fillMaxWidth().weight(1f)) {
                                if (!compact) sidebar()
                                Column(Modifier.weight(1f).fillMaxHeight()) {
                                    topBar(compact)
                                    Row(Modifier.fillMaxWidth().weight(1f)) {
                                        Box(
                                            Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .then(
                                                    if (glass && !materialBehindApp) {
                                                        Modifier.background(containerColor)
                                                    } else {
                                                        Modifier
                                                    },
                                                ),
                                        ) {
                                            Box(Modifier.fillMaxSize().hazeSource(haze)) {
                                                if (glass) backdrop(materialBehindApp)
                                                content(
                                                    PaddingValues(
                                                        start = 0.dp,
                                                        end = 0.dp,
                                                        bottom = if (compact) 128.dp else 84.dp,
                                                    ),
                                                )
                                            }
                                        }
                                        Box(
                                            Modifier
                                                .fillMaxHeight()
                                                .then(
                                                    if (glass && !materialBehindApp) {
                                                        Modifier.background(containerColor)
                                                    } else {
                                                        Modifier
                                                    },
                                                ),
                                        ) { trailing() }
                                    }
                                }
                            }
                            bottomBar(compact)
                        }
                    }
                    overlay()
                    DesktopDownloadFloatingWidget(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 24.dp, bottom = 96.dp),
                    )
                }
            }
        }
    }
}

/**
 * What [DesktopFrame] paints behind the window.
 *
 * [artworkUrl] is the page's lead artwork when it has one, and the mesh built from it is laid under
 * ink so a page of text stays readable over it.
 */
@Composable
internal fun DesktopPageBackdrop(artworkUrl: String?, transparentBase: Boolean, containerColor: Color = DesktopBackground) {
    Box(
        Modifier
            .fillMaxSize()
            .then(if (transparentBase) Modifier else Modifier.background(containerColor)),
    ) {
        if (artworkUrl == null) return@Box
        DesktopMesh(artworkUrl)
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.30f),
                        Color.Black.copy(alpha = 0.72f),
                        Color.Black.copy(alpha = 0.88f),
                    ),
                ),
            ),
        )
    }
}

