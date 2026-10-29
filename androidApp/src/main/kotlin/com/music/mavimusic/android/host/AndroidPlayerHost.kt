package com.music.mavimusic.android.host

import android.content.Context
import android.media.AudioManager
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.music.mavimusic.data.canvas.CanvasArtwork
import com.music.mavimusic.data.lyrics.LyricLine
import com.music.mavimusic.data.lyrics.LyricsSource
import com.music.mavimusic.data.model.Song
import com.music.mavimusic.data.settings.AudioQuality
import com.music.mavimusic.data.settings.LastPlayerScreen
import com.music.mavimusic.data.settings.MixBlend
import com.music.mavimusic.data.settings.SmartAnalysis
import com.music.mavimusic.data.settings.TransitionWindow
import com.music.mavimusic.ui.player.AudioOutputDevice
import com.music.mavimusic.ui.player.AudioOutputKind
import com.music.mavimusic.ui.player.CanvasVideoSpec
import com.music.mavimusic.ui.player.LyricsRomanizationResult
import com.music.mavimusic.ui.player.LyricsTranslationResult
import com.music.mavimusic.ui.player.OutputFormatUi
import com.music.mavimusic.ui.player.PartyUi
import com.music.mavimusic.ui.player.PlayerHost
import com.music.mavimusic.ui.player.PlayerSettingsSource
import com.music.mavimusic.ui.player.SystemVolume
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AndroidPlayerSettings : PlayerSettingsSource {
    override val animatedCanvas = MutableStateFlow(false)
    override val audioQualityCellular = MutableStateFlow(AudioQuality.HIGH)
    override val audioQualityWifi = MutableStateFlow(AudioQuality.HIGH)
    override val canvasOverCellular = MutableStateFlow(false)
    override val fullBleedArtwork = MutableStateFlow(true)
    override val hideSongStatus = MutableStateFlow(false)
    override val showQueueHistory = MutableStateFlow(false)
    override val hideVolumeBar = MutableStateFlow(false)
    override val hideStatusBar = MutableStateFlow(false)
    override val lastPlayerScreen = MutableStateFlow(LastPlayerScreen.MAIN)
    override val legacyMeshGradient = MutableStateFlow(false)
    override val lyricsBlur = MutableStateFlow(true)
    override val lyricsOffsetMs = MutableStateFlow(0)
    override val lyricsSourceOrder = MutableStateFlow<List<LyricsSource>>(LyricsSource.offered)
    override val meteredConnection = MutableStateFlow<Boolean?>(false)
    override val preferUsbDac = MutableStateFlow(false)
    override val prioritizeSpotifyCanvas = MutableStateFlow(false)
    override val reduceAnimation = MutableStateFlow(false)
    override val reduceDynamicBlur = MutableStateFlow(false)
    override val showNerdStats = MutableStateFlow(false)
    override val smartAnalysis = MutableStateFlow(SmartAnalysis())
    override val smartFadeEnabled = MutableStateFlow(true)
    override val smartMixInProgress = MutableStateFlow(false)
    override val smartMixBlend = MutableStateFlow<MixBlend?>(null)
    override val smartTransitionWindow = MutableStateFlow<TransitionWindow?>(null)
    override val spotifyCanvasAutoHide = MutableStateFlow(false)
    override val syncedLyrics = MutableStateFlow(true)
    override val translationLanguage = MutableStateFlow("es")
    override val versionAlignmentInProgress = MutableStateFlow(false)

    override fun setLastPlayerScreen(value: LastPlayerScreen) {
        lastPlayerScreen.value = value
    }

    override fun setLyricsOffsetMs(value: Int) {
        lyricsOffsetMs.value = value
    }
}

class AndroidPlayerHost(private val context: Context) : PlayerHost {
    override val settings = AndroidPlayerSettings()

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val _volumeLevel = MutableStateFlow(readSystemVolume())

    private fun readSystemVolume(): Float {
        val am = audioManager ?: return 1f
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat()
        if (max <= 0f) return 1f
        val cur = am.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
        return (cur / max).coerceIn(0f, 1f)
    }

    override val volume: SystemVolume = object : SystemVolume {
        override val level: StateFlow<Float> = _volumeLevel.asStateFlow()

        override fun set(level: Float) {
            val am = audioManager ?: return
            val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val target = (level.coerceIn(0f, 1f) * max).toInt()
            am.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
            _volumeLevel.value = level
        }

        override fun refresh() {
            _volumeLevel.value = readSystemVolume()
        }
    }

    override fun cachedCanvas(song: Song): CanvasArtwork? = null

    override suspend fun canvasFor(song: Song): CanvasArtwork? = null

    @Composable
    override fun CanvasVideo(spec: CanvasVideoSpec, modifier: Modifier) = Unit

    @Composable
    override fun rememberRemoteArtworkUrl(song: Song?): String? = song?.thumbnailUrl

    @Composable
    override fun rememberAudioOutputs(): List<AudioOutputDevice> = listOf(
        AudioOutputDevice(
            id = 0,
            name = "Altavoz del teléfono",
            kind = AudioOutputKind.PHONE,
            isActive = true
        )
    )

    @Composable
    override fun rememberOutputPicker(onOpen: () -> Unit): () -> Unit = onOpen

    override fun selectAudioOutput(id: Int?) = Unit

    override val outputFormat: StateFlow<OutputFormatUi> =
        MutableStateFlow(OutputFormatUi(summary = "Formato según la fuente", carriesHiRes = false))

    override val party: StateFlow<PartyUi> = MutableStateFlow(PartyUi())

    override suspend fun translateLyrics(
        trackId: String,
        lines: List<LyricLine>,
        targetLanguageTag: String,
    ): LyricsTranslationResult = LyricsTranslationResult.Unavailable

    override suspend fun romanizeLyrics(
        trackId: String,
        lines: List<LyricLine>,
        targetLanguageTag: String,
    ): LyricsRomanizationResult = LyricsRomanizationResult.Unavailable

    override fun showMessage(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    @Composable
    override fun AudioPipelineDialog(hazeState: HazeState, isPlaying: Boolean, onDismiss: () -> Unit) = Unit
}
