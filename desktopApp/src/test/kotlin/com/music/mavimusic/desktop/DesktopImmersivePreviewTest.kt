package com.music.mavimusic.desktop

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.Density
import com.music.mavimusic.data.model.Song
import com.music.mavimusic.playback.PlaybackPosition
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.SwingUtilities
import kotlin.test.*

/** Isolated offscreen screen, never launches MaviMusic or reads its real profile. */
class DesktopImmersivePreviewTest {
    @OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
    @Test fun videoHasAudioVideoSwitchAndNoVisualizerMenu() {
        org.junit.Assume.assumeTrue(System.getenv("MAVIMUSIC_REVIEW_VISUAL") == "1")
        val directory = Path.of("../output/desktop-review-v2").toAbsolutePath()
        Files.createDirectories(directory)
        val clip = Files.createTempFile("mavi-preview-", ".mp4")
        javaClass.getResourceAsStream("/video/sync-test.mp4")!!.use { input -> Files.newOutputStream(clip).use { input.copyTo(it) } }
        val song = Song("preview", "Prueba de videoclip", "MaviMusic", null)
        val asset = DesktopMusicVideo(song, song.copy(isVideo = true), clip.toString(), emptyMap(), false, 0)
        var video by mutableStateOf<DesktopMusicVideo?>(asset)
        var enabled by mutableStateOf(true)
        var scene: ImageComposeScene? = null
        val position = PlaybackPosition().apply { report(0) }
        fun nodes(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::nodes)
        fun allNodes() = scene!!.semanticsOwners.flatMap { nodes(it.rootSemanticsNode) }
        fun text(node: SemanticsNode): List<String> = node.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text }
        fun texts() = allNodes().flatMap(::text)
        try {
            SwingUtilities.invokeAndWait {
                scene = ImageComposeScene(1280, 800, Density(1f)) {
                    MaterialTheme(colorScheme = darkColorScheme()) {
                        DesktopImmersiveLyricsScreen(
                            song = song, musicVideo = video, automaticMusicVideo = enabled,
                            onAutomaticMusicVideoChange = { enabled = it; video = if (it) asset else null },
                            isPlaying = false, isLoading = false, position = position, durationMs = 3_000,
                            volume = 0.5f, onVolumeChange = {}, onSeek = {}, onPlayPause = {}, onPrevious = {}, onNext = {},
                            shuffle = false, onShuffleChange = {}, repeatMode = DesktopRepeatMode.OFF, onRepeatModeChange = {},
                            lyrics = null, lyricsSource = null, lyricsProviderStates = emptyMap(), onSelectLyricsProvider = {},
                            lyricsUnavailable = true, volumeNormalizationActive = false, onClose = {},
                        )
                    }
                }
            }
            repeat(10) { index ->
                SwingUtilities.invokeAndWait { scene!!.render(index * 33_000_000L).close() }
                Thread.sleep(30)
            }
            SwingUtilities.invokeAndWait {
                val labels = texts()
                assertTrue("Audio" in labels)
                assertTrue("Vídeo" in labels)
                assertFalse(ImmersiveVisualMode.entries.any { it.label in labels }, "Video must hide all visualizer selectors")
                scene!!.render(400_000_000L).use { image -> Files.write(directory.resolve("inmersion-video.png"), image.encodeToData()!!.bytes) }
                val audio = allNodes().first { node ->
                    node.config.getOrNull(SemanticsActions.OnClick) != null &&
                        nodes(node).flatMap(::text).contains("Audio") && node.config.getOrNull(SemanticsProperties.Selected) != null
                }
                assertTrue(audio.config[SemanticsActions.OnClick].action!!.invoke())
                scene!!.render(500_000_000L).close()
                assertFalse(enabled)
                assertNull(video)
                assertTrue(ImmersiveVisualMode.entries.any { it.label in texts() }, "Audio restores visualizer selector")
                scene!!.render(600_000_000L).use { image -> Files.write(directory.resolve("inmersion-audio.png"), image.encodeToData()!!.bytes) }
            }
        } finally {
            SwingUtilities.invokeAndWait { scene?.close() }
            Files.deleteIfExists(clip)
        }
    }
    @OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
    @Test fun suggestedShelfHasWorkingRefreshAndCollectionMenu() {
        org.junit.Assume.assumeTrue(System.getenv("MAVIMUSIC_REVIEW_VISUAL") == "1")
        com.music.mavimusic.ui.AppUi.install(object : com.music.mavimusic.ui.AppUiHost {
            override val swipeToPlayNext = kotlinx.coroutines.flow.MutableStateFlow(false)
            override val homeRecentsViewType = kotlinx.coroutines.flow.MutableStateFlow(com.music.mavimusic.data.settings.LibraryViewType.GRID)
            override val pinnedPlaylists = kotlinx.coroutines.flow.MutableStateFlow(emptyList<String>())
            override val librarySort = kotlinx.coroutines.flow.MutableStateFlow(com.music.mavimusic.data.settings.LibrarySort.DEFAULT)
            override fun setHomeRecentsViewType(value: com.music.mavimusic.data.settings.LibraryViewType) { homeRecentsViewType.value = value }
            @Composable override fun downloadedIds(): Set<String> = emptySet()
        })
        val owner = object : androidx.lifecycle.LifecycleOwner {
            override val lifecycle = androidx.lifecycle.LifecycleRegistry.createUnsafe(this).apply { currentState = androidx.lifecycle.Lifecycle.State.RESUMED }
        }
        var refreshed = false
        var opened = false
        var scene: ImageComposeScene? = null
        fun nodes(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::nodes)
        val shelf = com.music.mavimusic.data.model.HomeShelf("Música sugerida", listOf(
            com.music.mavimusic.data.model.ShelfItem("Playlist de prueba", "Playlist · Artista", null, null, "VLpreview"),
            com.music.mavimusic.data.model.ShelfItem("Álbum de prueba", "Álbum · Artista", null, null, "MPREb_preview"),
        ))
        try {
            SwingUtilities.invokeAndWait {
                scene = ImageComposeScene(1280, 800, Density(1f)) {
                    MaterialTheme(colorScheme = darkColorScheme()) {
                        androidx.compose.material3.Surface {
                            CompositionLocalProvider(
                                androidx.lifecycle.compose.LocalLifecycleOwner provides owner,
                                com.music.mavimusic.ui.screens.LocalHomeShelfActions provides com.music.mavimusic.ui.screens.HomeShelfActions({ true }, { refreshed = true }),
                            ) {
                                com.music.mavimusic.ui.screens.HomeScreen(
                                    state = com.music.mavimusic.data.model.UiState.Success(listOf(shelf)),
                                    listState = androidx.compose.foundation.lazy.rememberLazyListState(),
                                    onItemClick = { _, _ -> }, onItemLongPress = { opened = true },
                                    onRetry = {}, refreshing = false, onRefresh = {},
                                    pullState = androidx.compose.material3.pulltorefresh.rememberPullToRefreshState(),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(), title = null, leadHero = false,
                                )
                            }
                        }
                    }
                }
                scene!!.render(0L).close()
                val all = scene!!.semanticsOwners.flatMap { nodes(it.rootSemanticsNode) }
                val refresh = all.asReversed().first { node ->
                    node.config.getOrNull(SemanticsActions.OnClick) != null && nodes(node).any { child -> child.config.getOrNull(SemanticsProperties.Text).orEmpty().any { it.text == "Actualizar sugerencias" } }
                }
                refresh.config[SemanticsActions.OnClick].action!!.invoke()
                assertTrue(refreshed)
                val menu = all.asReversed().first { node ->
                    node.config.getOrNull(SemanticsActions.OnClick) != null && nodes(node).any { child -> child.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().any { it.equals("More", true) || it.equals("Más", true) } }
                }
                menu.config[SemanticsActions.OnClick].action!!.invoke()
                assertTrue(opened)
                scene!!.render(100_000_000L).use { image ->
                    val file = Path.of("../output/desktop-review-v2/inicio-sugerencias.png").toAbsolutePath()
                    Files.createDirectories(file.parent)
                    Files.write(file, image.encodeToData()!!.bytes)
                }
            }
        } finally { SwingUtilities.invokeAndWait { scene?.close() } }
    }

}
