package com.music.mavimusic.desktop

import kotlin.test.*

class DesktopPlaybackRecoveryTest {
    @Test fun retriesAreBoundedAndNewListeningIntentGetsItsOwnBudget() {
        val recovery = DesktopPlaybackRecovery()
        assertEquals(1_000L, recovery.nextDelay("song/1", true, "timeout", 20_000))
        assertEquals(3_000L, recovery.nextDelay("song/1", true, "timeout", 20_000))
        assertNull(recovery.nextDelay("song/1", true, "timeout", 20_000))
        assertEquals(1_000L, recovery.nextDelay("song/2", true, "timeout", 20_000))
    }
    @Test fun localFilesAndMissingAudioOutputDoNotTriggerNetworkRecovery() {
        assertNull(DesktopPlaybackRecovery().nextDelay("local", false, "bad file", 0))
        assertNull(DesktopPlaybackRecovery().nextDelay("remote", true, "No audio output: device missing", 0))
    }
    @Test fun successfulProgressRearmsRecoveryButAnotherImmediateFailureDoesNot() {
        val recovery = DesktopPlaybackRecovery()
        recovery.nextDelay("song", true, "timeout", 10_000)
        recovery.nextDelay("song", true, "timeout", 10_000)
        assertNull(recovery.nextDelay("song", true, "timeout", 11_000))
        assertEquals(1_000L, recovery.nextDelay("song", true, "timeout", 41_000))
    }
    @Test fun genuineEofAndShortBufferLatencyAreNotNetworkFailures() {
        assertFalse(DesktopPlaybackRecovery.prematureEnd(true, 178_000, 180_000))
        assertFalse(DesktopPlaybackRecovery.prematureEnd(false, 20_000, 180_000))
        assertFalse(DesktopPlaybackRecovery.prematureEnd(true, 20_000, 0))
        assertTrue(DesktopPlaybackRecovery.prematureEnd(true, 20_000, 180_000))
    }
    @Test fun pausedAndHiddenVideoWaitForAnExplicitRenderReason() {
        assertFalse(VideoRenderGate(false, true, false, 1).shouldRender(true, 0))
        assertFalse(VideoRenderGate(true, false, false, 1).shouldRender(false, 1))
        assertFalse(VideoRenderGate(true, true, true, 1).shouldRender(true, 0))
        assertTrue(VideoRenderGate(true, false, false, 2).shouldRender(false, 1))
        assertTrue(VideoRenderGate(true, true, false, 1).shouldRender(false, 1))
    }
    @Test fun videoAtEofDoesNotReseekUntilUserRequestsASeek() {
        assertFalse(shouldSeekVideo(lastSeek = 3, currentSeeks = 3, atEnd = true, decoderPositionMs = 178_000, targetMs = 180_500))
        assertTrue(shouldSeekVideo(lastSeek = 3, currentSeeks = 4, atEnd = true, decoderPositionMs = 178_000, targetMs = 20_000))
        assertTrue(shouldSeekVideo(lastSeek = 3, currentSeeks = 3, atEnd = false, decoderPositionMs = 10_000, targetMs = 12_000))
    }
}
