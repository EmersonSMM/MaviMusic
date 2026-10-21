package com.music.bitchord.desktop

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Visual color themes for the desktop application ground and surfaces.
 */
internal enum class DesktopThemeTone(
    val id: String,
    val title: String,
    val background: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val glass: Color,
) {
    DARK_CLASSIC("dark_classic", "Classic Dark", Color(0xFF202020), Color(0xFF141416), Color(0xFF26262A), Color(0x661C1C1E)),
    OLED_BLACK("oled_black", "OLED Black", Color(0xFF000000), Color(0xFF0A0A0C), Color(0xFF161618), Color(0x66121212)),
    MIDNIGHT_BLUE("midnight_blue", "Midnight Blue", Color(0xFF0B111E), Color(0xFF070B14), Color(0xFF141E32), Color(0x66111B2E)),
    DEEP_PURPLE("deep_purple", "Deep Velvet", Color(0xFF150D1E), Color(0xFF0B0710), Color(0xFF241533), Color(0x6620132E)),
    SLATE_GRAY("slate_gray", "Slate Modern", Color(0xFF181B20), Color(0xFF0F1115), Color(0xFF22272E), Color(0x661E232B)),
    FOREST_EMERALD("forest_emerald", "Forest Emerald", Color(0xFF0B1713), Color(0xFF060E0B), Color(0xFF142620), Color(0x6612241E));

    companion object {
        fun fromId(id: String): DesktopThemeTone = entries.firstOrNull { it.id == id } ?: DARK_CLASSIC
    }
}

/**
 * The toggles read far from the Settings sheet that sets them.
 *
 * Mirrors Android's `AppSettings` for these few: a composable deep in the player or the lyrics
 * pane can collect one without it being threaded down through every caller.
 */
internal object DesktopAppearanceSettings {

    private fun flag(key: String, default: Boolean) =
        MutableStateFlow(DesktopPersistence().boolean(key, default))

    private val _reduceAnimation = flag(KEY_REDUCE_ANIMATION, false)
    private val _reduceDynamicBlur = flag(KEY_REDUCE_DYNAMIC_BLUR, false)
    private val _hideVolumeBar = flag(KEY_HIDE_VOLUME_BAR, false)
    private val _ambientBackdrop = flag(KEY_AMBIENT_BACKDROP, false)
    private val _floatingDownloadStatus = flag(KEY_FLOATING_DOWNLOAD_STATUS, true)
    private val _themeTone = MutableStateFlow(
        DesktopThemeTone.fromId(DesktopPersistence().string(KEY_THEME_TONE, DesktopThemeTone.DARK_CLASSIC.id))
    )

    /** Freezes the main player's gradient instead of drifting. */
    val reduceAnimation: StateFlow<Boolean> = _reduceAnimation

    /** Swaps frosted glass for solid fills across the app. */
    val reduceDynamicBlur: StateFlow<Boolean> = _reduceDynamicBlur

    /** Removes the volume slider from the main player. */
    val hideVolumeBar: StateFlow<Boolean> = _hideVolumeBar

    /** Diffuses current artwork colors under the window glass chrome like Apple Music. */
    val ambientBackdrop: StateFlow<Boolean> = _ambientBackdrop

    /** Whether to show a floating download status pill at the bottom right. */
    val floatingDownloadStatus: StateFlow<Boolean> = _floatingDownloadStatus

    /** The active theme tone for backgrounds and surfaces. */
    val themeTone: StateFlow<DesktopThemeTone> = _themeTone

    fun setReduceAnimation(value: Boolean) = write(KEY_REDUCE_ANIMATION, value, _reduceAnimation)

    fun setReduceDynamicBlur(value: Boolean) = write(KEY_REDUCE_DYNAMIC_BLUR, value, _reduceDynamicBlur)

    fun setHideVolumeBar(value: Boolean) = write(KEY_HIDE_VOLUME_BAR, value, _hideVolumeBar)

    fun setAmbientBackdrop(value: Boolean) = write(KEY_AMBIENT_BACKDROP, value, _ambientBackdrop)

    fun setFloatingDownloadStatus(value: Boolean) = write(KEY_FLOATING_DOWNLOAD_STATUS, value, _floatingDownloadStatus)

    fun setThemeTone(tone: DesktopThemeTone) {
        DesktopPersistence().saveString(KEY_THEME_TONE, tone.id)
        _themeTone.value = tone
    }

    private fun write(key: String, value: Boolean, into: MutableStateFlow<Boolean>) {
        DesktopPersistence().saveBoolean(key, value)
        into.value = value
    }

    internal const val KEY_REDUCE_ANIMATION = "reduce_animation"
    internal const val KEY_REDUCE_DYNAMIC_BLUR = "reduce_dynamic_blur"
    internal const val KEY_HIDE_VOLUME_BAR = "hide_volume_bar"
    internal const val KEY_AMBIENT_BACKDROP = "ambient_backdrop"
    internal const val KEY_FLOATING_DOWNLOAD_STATUS = "floating_download_status"
    internal const val KEY_THEME_TONE = "theme_tone"
}

