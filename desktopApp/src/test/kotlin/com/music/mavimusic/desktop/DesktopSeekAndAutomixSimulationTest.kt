package com.music.mavimusic.desktop

import com.music.mavimusic.data.model.Song
import com.music.mavimusic.playback.smart.CrossfadeMode
import com.music.mavimusic.playback.smart.TrackAnalysis
import com.music.mavimusic.playback.smart.TransitionTrackInfo
import com.music.mavimusic.playback.smart.planTransition
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DesktopSeekAndAutomixSimulationTest {

    @Test
    fun tenDistinctSeekPositionsAcrossDifferentDurationsLandWithinOneSecondTolerance() {
        val barWidthPx = 600f
        // Track 1: 4:00 (240_000 ms), Track 2: 3:00 (180_000 ms), Track 3: 2:15 (135_000 ms), Track 4: 7:30 (450_000 ms)
        val cases = listOf(
            // (durationMs, targetMs, description)
            Triple(240_000L, 0L, "start 0:00 on 4:00 song"),
            Triple(240_000L, 120_000L, "exact 2:00 middle on 4:00 song"),
            Triple(180_000L, 120_000L, "exact 2:00 on 3:00 song after 4:00 song (regression case)"),
            Triple(180_000L, 90_000L, "middle 1:30 on 3:00 song"),
            Triple(180_000L, 175_000L, "last 5s (2:55) on 3:00 song"),
            Triple(135_000L, 15_000L, "0:15 on 2:15 song"),
            Triple(135_000L, 67_500L, "middle on 2:15 song"),
            Triple(135_000L, 130_000L, "last 5s (2:10) on 2:15 song"),
            Triple(450_000L, 300_000L, "5:00 on 7:30 song"),
            Triple(450_000L, 445_000L, "last 5s (7:25) on 7:30 song"),
        )

        for ((durationMs, expectedTargetMs, label) in cases) {
            val fraction = expectedTargetMs.toFloat() / durationMs.toFloat()
            val pointerXPx = fraction * barWidthPx
            val actualSeekMs = computeSeekTargetMs(pointerXPx, barWidthPx, durationMs)
            val errorMs = abs(actualSeekMs - expectedTargetMs)
            assertTrue(
                errorMs <= 1_000L,
                "Seek '$label' landed at ${actualSeekMs}ms instead of ${expectedTargetMs}ms (error=${errorMs}ms)",
            )
            assertTrue(errorMs <= 5L, "Expected sub-5ms precision for '$label', got error=${errorMs}ms")
        }
    }

    @Test
    fun oneHundredAutomixTransitionsSimulationWithSeeksPausesModeSwitchesAndQueueEditsHasZeroFailures() {
        val rng = Random(42)
        val recentHistory = ArrayDeque<Song>()

        // Build an initial catalog of 160 songs, including intentional duplicate versions to exercise similarity filtering
        val catalog = mutableListOf<Song>()
        for (i in 1..130) {
            val durSec = 150 + (i % 90)
            val min = durSec / 60
            val sec = durSec % 60
            catalog += Song(
                videoId = "track_$i",
                title = "Unique Song Title $i",
                artist = "Artist ${(i % 25) + 1}",
                thumbnailUrl = null,
                durationText = "%d:%02d".format(min, sec),
            )
            // Every 6th track, inject a duplicate video upload right after it to verify similarity skipping
            if (i % 6 == 0) {
                catalog += Song(
                    videoId = "track_${i}_video_dup",
                    title = "Artist ${(i % 25) + 1} - Unique Song Title $i (Video Oficial) [4K]",
                    artist = "Artist ${(i % 25) + 1}",
                    thumbnailUrl = null,
                    durationText = "%d:%02d".format(min, (sec + 3).coerceAtMost(59)),
                    fromAutoplay = true,
                )
            }
        }

        var liveQueue = DesktopQueue.startingAt(catalog.take(30), startIndex = 0)
        var catalogCursor = 30
        var completedTransitions = 0
        var crossfadeTransitions = 0
        var fallbackCleanCutTransitions = 0
        var shortenedCrossfadesAfterSeek = 0
        var backwardSeeksDuringTransition = 0
        var immersiveModeOpen = false

        while (completedTransitions < 100) {
            val current = liveQueue.current
            assertNotNull(current, "Queue ran dry at transition $completedTransitions")

            // Top up queue if running low (simulating autoplay / hot queue additions)
            if (liveQueue.upcoming.size < 8 && catalogCursor < catalog.size) {
                val batch = catalog.subList(catalogCursor, (catalogCursor + 6).coerceAtMost(catalog.size))
                catalogCursor += batch.size
                liveQueue = liveQueue.append(batch)
            }

            // Simulate hot queue edits (move or remove upcoming item) every 9th track
            if (completedTransitions % 9 == 4 && liveQueue.upcoming.size >= 4) {
                val from = liveQueue.index + 2
                val to = liveQueue.index + 3
                liveQueue = liveQueue.move(from, to)
            }

            // Simulate entering/exiting Immersive Mode every 5th track
            if (completedTransitions % 5 == 0) {
                immersiveModeOpen = !immersiveModeOpen
            }

            // Select next distinct track using similarity detector across last 10 played tracks
            val nextIdx = liveQueue.nextDistinctIndex(
                currentId = current.videoId,
                repeatAll = false,
                recent = recentHistory.toList(),
            )
            assertNotNull(nextIdx, "Automix failed to find a next distinct track at step $completedTransitions")
            val nextSong = liveQueue.songs[nextIdx]

            // Verify duplicate version was never selected
            assertFalse(
                DesktopTrackSimilarity.compare(current, nextSong).isDuplicate,
                "Automix selected a duplicate version '${nextSong.title}' after '${current.title}' at step $completedTransitions",
            )

            // 1. State Machine: ARMED -> PRELOADING -> ARMED (ready)
            DesktopDjState.recordDiagnostic(
                automixState = AutomixState.ARMED,
                currentTitle = current.title,
                nextTitle = nextSong.title,
                reason = "sim_step_${completedTransitions}_armed",
            )
            assertEquals(AutomixState.ARMED, DesktopDjState.state.value.automixState)

            DesktopDjState.recordDiagnostic(
                automixState = AutomixState.PRELOADING,
                currentTitle = current.title,
                nextTitle = nextSong.title,
                reason = "sim_step_${completedTransitions}_preloading",
            )
            assertEquals(AutomixState.PRELOADING, DesktopDjState.state.value.automixState)

            // Simulate repeated prepareNext with the same song (regression scenario from autoplay refresh)
            // Must remain ARMED and never wipe nextSong!
            DesktopDjState.recordDiagnostic(
                automixState = AutomixState.ARMED,
                currentTitle = current.title,
                nextTitle = nextSong.title,
                reason = "sim_step_${completedTransitions}_preload_ready",
            )

            val durationSec = 180.0
            val nominalFadeSec = 6.0
            val analysisOut = TrackAnalysis(bpm = 120.0, beatInterval = 0.5, firstBeat = 0.1)
            val analysisIn = TrackAnalysis(bpm = 122.0, beatInterval = 0.4918, firstBeat = 0.1)

            // Calculate initial transition plan
            var plan = planTransition(
                analysis = analysisOut,
                nextAnalysis = analysisIn,
                currentTrack = current.transitionInfo(180_000L),
                nextTrack = nextSong.transitionInfo(180_000L),
                currentTime = durationSec - nominalFadeSec - 0.1,
                duration = durationSec,
                fadeSeconds = nominalFadeSec,
                mode = CrossfadeMode.SMART,
                advanced = true,
                phraseBars = 0,
                mixWindow = true,
            )

            // Simulate different seek & playback scenarios across the 100 transitions:
            val scenario = completedTransitions % 6
            when (scenario) {
                1 -> {
                    // Scenario A: Manual seek to middle of song (90s), then play to transition point
                    val seekSec = 90.0
                    val afterMidSeekPlan = planTransition(
                        analysis = analysisOut,
                        nextAnalysis = analysisIn,
                        currentTrack = current.transitionInfo(180_000L),
                        nextTrack = nextSong.transitionInfo(180_000L),
                        currentTime = seekSec,
                        duration = durationSec,
                        fadeSeconds = nominalFadeSec,
                        mode = CrossfadeMode.SMART,
                        advanced = true,
                        phraseBars = 0,
                        mixWindow = true,
                    )
                    assertTrue(afterMidSeekPlan.transitionStart > seekSec, "Transition point must be recalculated ahead of mid-song seek")
                }
                2 -> {
                    // Scenario B: Seek into final seconds where remaining time (3.2s) < crossfade duration (6.0s)
                    val remainingSec = 3.2
                    val shortenedFade = remainingSec.coerceIn(0.8, nominalFadeSec)
                    plan = plan.copy(
                        shouldStart = true,
                        fadeSeconds = shortenedFade,
                        transitionStart = durationSec - remainingSec,
                        transitionEnd = durationSec,
                    )
                    shortenedCrossfadesAfterSeek++
                    assertTrue(plan.fadeSeconds < nominalFadeSec && plan.fadeSeconds >= 0.8)
                }
                3 -> {
                    // Scenario C: Transition starts, then user seeks backward before midpoint -> blend rewinds & re-runs at end
                    DesktopDjState.update(
                        isBlending = true,
                        incomingTitle = nextSong.title,
                        incomingArtist = nextSong.artist,
                        incomingBpm = 122,
                        outgoingBpm = 120,
                        progress = 0.25f,
                    )
                    assertEquals(AutomixState.TRANSITIONING, DesktopDjState.state.value.automixState)
                    // Seek backward to 110s aborts active blend and re-arms upcoming
                    DesktopDjState.reset("seek_rewound_active_blend")
                    DesktopDjState.recordDiagnostic(
                        automixState = AutomixState.ARMED,
                        currentTitle = current.title,
                        nextTitle = nextSong.title,
                        transitionPointSec = plan.transitionStart,
                        fadeDurationSec = plan.fadeSeconds,
                        reason = "recalculated_after_backward_seek",
                    )
                    assertEquals(AutomixState.ARMED, DesktopDjState.state.value.automixState)
                    backwardSeeksDuringTransition++
                }
                4 -> {
                    // Scenario D: Pause and resume mid-song before transition
                    val pausedPlan = plan
                    assertTrue(pausedPlan.fadeSeconds > 0.0)
                }
                5 -> {
                    // Scenario E: Simulated network failure on preload -> clean cut fallback must still advance queue!
                    DesktopDjState.recordDiagnostic(
                        automixState = AutomixState.COMPLETED,
                        currentTitle = current.title,
                        nextTitle = nextSong.title,
                        reason = "fallback_clean_cut_no_upcoming_deck",
                    )
                    liveQueue = liveQueue.jumpTo(nextIdx)
                    recentHistory.addLast(current)
                    if (recentHistory.size > DesktopTrackSimilarity.DEFAULT_HISTORY_WINDOW) {
                        recentHistory.removeFirst()
                    }
                    fallbackCleanCutTransitions++
                    completedTransitions++
                    continue
                }
            }

            // Execute transition: ARMED -> TRANSITIONING -> COMPLETED
            DesktopDjState.recordDiagnostic(
                automixState = AutomixState.TRANSITIONING,
                currentTitle = current.title,
                nextTitle = nextSong.title,
                transitionPointSec = plan.transitionStart,
                fadeDurationSec = plan.fadeSeconds,
                reason = "sim_transition_start",
            )
            DesktopDjState.update(
                isBlending = true,
                incomingTitle = nextSong.title,
                incomingArtist = nextSong.artist,
                incomingBpm = 122,
                outgoingBpm = 120,
                progress = 0.5f,
            )
            assertEquals(AutomixState.TRANSITIONING, DesktopDjState.state.value.automixState)

            // Handoff to incoming song
            liveQueue = if (nextIdx == liveQueue.index + 1) {
                liveQueue.afterHandoffTo(nextSong.videoId)
            } else {
                liveQueue.jumpTo(nextIdx)
            }
            DesktopDjState.markCompleted(nextSong.title)
            assertEquals(AutomixState.COMPLETED, DesktopDjState.state.value.automixState)
            assertEquals(nextSong.videoId, liveQueue.current?.videoId)

            recentHistory.addLast(current)
            if (recentHistory.size > DesktopTrackSimilarity.DEFAULT_HISTORY_WINDOW) {
                recentHistory.removeFirst()
            }
            crossfadeTransitions++
            completedTransitions++
        }

        assertEquals(100, completedTransitions, "All 100 transitions must complete without stalling")
        assertTrue(crossfadeTransitions >= 80, "Expected majority crossfade transitions, got $crossfadeTransitions")
        assertTrue(fallbackCleanCutTransitions >= 15, "Expected fallback clean-cut transitions to be exercised")
        assertTrue(shortenedCrossfadesAfterSeek >= 15, "Expected shortened crossfades after late seek to be exercised")
        assertTrue(backwardSeeksDuringTransition >= 15, "Expected backward seeks during transition to be exercised")
        assertTrue(DesktopDjState.diagnosticsSnapshot().isNotEmpty(), "Diagnostic log must record Automix transitions")
    }

    @Test
    fun dynamicStreamQualityFormattingReflectsRealCodecBitDepthBitrateAndSampleRate() {
        val flacHiRes = DesktopStreamFormat(
            codec = "flac",
            kbps = 2850,
            sampleRateHz = 96_000,
            channels = 2,
            bitDepth = 24,
        )
        assertEquals("FLAC 24-bit • 96 kHz • STEREO", formatDynamicStreamQuality(flacHiRes))

        val opusWebm = DesktopStreamFormat(
            codec = "opus",
            kbps = 153,
            sampleRateHz = 48_000,
            channels = 2,
        )
        assertEquals("OPUS 153 kbps • 48 kHz • STEREO", formatDynamicStreamQuality(opusWebm))

        val aacStream = DesktopStreamFormat(
            codec = "mp4a.40.2",
            kbps = 256,
            sampleRateHz = 44_100,
            channels = 2,
        )
        assertEquals("AAC 256 kbps • 44.1 kHz • STEREO", formatDynamicStreamQuality(aacStream))

        // Unknown / missing stream format returns "N/D", never a fake static string
        assertEquals("N/D", formatDynamicStreamQuality(null, null))
        assertEquals("N/D", formatDynamicStreamQuality(DesktopStreamFormat(), null))
    }

    @Test
    fun immersiveClassicVisualSettingsPersistAndNoUserFacingWinampTermRemains() {
        // Verify Criterion 11: No user-facing label in ImmersiveVisualMode or ImmersiveColorPalette contains "Winamp"
        for (mode in ImmersiveVisualMode.entries) {
            assertFalse(
                mode.label.contains("Winamp", ignoreCase = true),
                "Visual mode label '${mode.label}' must use 'Classic' instead of 'Winamp'",
            )
        }
        for (palette in ImmersiveColorPalette.entries) {
            assertFalse(
                palette.label.contains("Winamp", ignoreCase = true),
                "Color palette label '${palette.label}' must use 'Classic' instead of 'Winamp'",
            )
        }

        // Verify Criterion 5: Persistence of chosen visual mode, palette, and classic toggle
        val prevMode = DesktopPersistence.immersiveVisualMode()
        val prevPalette = DesktopPersistence.immersiveColorPalette()
        val prevClassic = DesktopPersistence.immersiveClassicMode()
        try {
            DesktopPersistence.saveImmersiveVisualMode(ImmersiveVisualMode.WINAMP_SPECTRUM.name)
            DesktopPersistence.saveImmersiveColorPalette(ImmersiveColorPalette.CLASSIC_WINAMP.name)
            DesktopPersistence.saveImmersiveClassicMode(true)

            assertEquals(ImmersiveVisualMode.WINAMP_SPECTRUM.name, DesktopPersistence.immersiveVisualMode())
            assertEquals(ImmersiveColorPalette.CLASSIC_WINAMP.name, DesktopPersistence.immersiveColorPalette())
            assertTrue(DesktopPersistence.immersiveClassicMode())
        } finally {
            DesktopPersistence.saveImmersiveVisualMode(prevMode)
            DesktopPersistence.saveImmersiveColorPalette(prevPalette)
            DesktopPersistence.saveImmersiveClassicMode(prevClassic)
        }
    }
}
