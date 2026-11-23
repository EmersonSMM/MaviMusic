package com.music.mavimusic.desktop

import java.lang.management.ManagementFactory
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.*

/** Decode-only measurements; not a claim about interactive GPU FPS or total app CPU. */
class DesktopMediaProfileTest {
    @Test fun measureIsolatedAudioAnd720pVideoDecoding() {
        org.junit.Assume.assumeTrue(System.getenv("MAVIMUSIC_PROFILE_MEDIA") == "1")
        val directory = Path.of("../output/desktop-review-v2").toAbsolutePath()
        val cpu = ManagementFactory.getThreadMXBean()
        val memory = ManagementFactory.getMemoryMXBean()
        val video = DesktopCanvasDecoder(maxEdge = 1280, looping = false)
        var frames = 0
        var videoWall: Long
        var videoCpu: Long
        val heapBefore = memory.heapMemoryUsage.used
        try {
            video.open(directory.resolve("profile-720p.mp4").toString()).getOrThrow()
            val pixels = ByteArray(video.width * video.height * 4)
            val started = System.nanoTime()
            val cpuStarted = cpu.currentThreadCpuTime
            while (video.nextFrame(pixels)) frames++
            videoWall = System.nanoTime() - started
            videoCpu = cpu.currentThreadCpuTime - cpuStarted
        } finally { video.close() }
        val audio = DesktopAudioDecoder()
        var samples = 0L
        var audioWall: Long
        var audioCpu: Long
        try {
            audio.open(directory.resolve("profile-audio.wav").toString(), requested = DesktopPcmFormat(44_100, 2, 4, true)).getOrThrow()
            val started = System.nanoTime()
            val cpuStarted = cpu.currentThreadCpuTime
            while (audio.readSamples() != null) samples += audio.sampleCount
            audioWall = System.nanoTime() - started
            audioCpu = cpu.currentThreadCpuTime - cpuStarted
        } finally { audio.close() }
        assertEquals(150, frames)
        assertTrue(samples > 400_000)
        val report = """
            {
              "scope": "synthetic decode only; no speakers, UI, GPU, library or user session",
              "videoFrames720p": $frames,
              "videoWallMs": ${videoWall / 1_000_000.0},
              "videoThreadCpuMs": ${videoCpu / 1_000_000.0},
              "audioSamples": $samples,
              "audioWallMs": ${audioWall / 1_000_000.0},
              "audioThreadCpuMs": ${audioCpu / 1_000_000.0},
              "heapBeforeBytes": $heapBefore,
              "heapAfterBytes": ${memory.heapMemoryUsage.used}
            }
        """.trimIndent()
        Files.writeString(directory.resolve("media-profile.json"), report)
    }
}
