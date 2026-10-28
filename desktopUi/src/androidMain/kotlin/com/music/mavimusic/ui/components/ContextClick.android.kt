package com.music.mavimusic.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalFoundationApi::class)
actual fun Modifier.contextClick(onClick: (() -> Unit)?): Modifier =
    if (onClick == null) {
        this
    } else {
        combinedClickable(
            onLongClick = onClick,
            onClick = {}
        )
    }
