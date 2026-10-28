package com.music.mavimusic.ui.haptics

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

actual class Haptics(private val view: View?) {
    actual fun play(haptic: Haptic) {
        val view = view ?: return
        val constant = when (haptic) {
            Haptic.Tick -> HapticFeedbackConstants.CLOCK_TICK
            Haptic.Tap -> HapticFeedbackConstants.VIRTUAL_KEY
            Haptic.Select -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.CONFIRM
            } else {
                HapticFeedbackConstants.KEYBOARD_TAP
            }
            Haptic.ToggleOn -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.CONFIRM
            } else {
                HapticFeedbackConstants.VIRTUAL_KEY
            }
            Haptic.ToggleOff -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.REJECT
            } else {
                HapticFeedbackConstants.CLOCK_TICK
            }
            Haptic.SkipNext, Haptic.SkipPrevious -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.GESTURE_START
            } else {
                HapticFeedbackConstants.KEYBOARD_TAP
            }
            Haptic.Resume -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.GESTURE_START
            } else {
                HapticFeedbackConstants.VIRTUAL_KEY
            }
            Haptic.Pause -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.GESTURE_END
            } else {
                HapticFeedbackConstants.CLOCK_TICK
            }
            Haptic.Expand -> HapticFeedbackConstants.LONG_PRESS
        }
        view.performHapticFeedback(constant)
    }
}

@Composable
actual fun rememberHaptics(): Haptics {
    val view = LocalView.current
    return remember(view) { Haptics(view) }
}
