package com.music.mavimusic.ui.player

import android.os.Build
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalView
import java.util.Locale

private val startUptime = SystemClock.uptimeMillis()

internal actual fun uptimeMillis(): Long = SystemClock.uptimeMillis() - startUptime

internal actual val renderEffectBlurSupported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@Composable
internal actual fun KeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(enabled) {
        view.keepScreenOn = enabled
        onDispose { view.keepScreenOn = false }
    }
}

@Composable
internal actual fun appLanguageTag(): String = Locale.getDefault().toLanguageTag()

@Composable
internal actual fun PlayerBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}

internal actual val drawerFollowsListScroll: Boolean get() = true

internal actual fun DrawScope.clipShiftedDown(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    dy: Float,
    block: DrawScope.() -> Unit,
) {
    clipRect(left = left, top = top, right = right, bottom = bottom) {
        translate(top = dy) { block() }
    }
}
