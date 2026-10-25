package com.music.mavimusic.desktop

import androidx.compose.ui.graphics.Color
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopGpuVisualizersTest {

    @Test
    fun testGpuSettingsTogglesAndFpsClamping() {
        val initialEnabled = DesktopGpuSettings.isEnabled()
        val initialFps = DesktopGpuSettings.targetFps.value
        try {
            DesktopGpuSettings.setEnabled(true)
            assertTrue(DesktopGpuSettings.isEnabled())

            DesktopGpuSettings.setTargetFps(30)
            assertEquals(30, DesktopGpuSettings.targetFps.value)

            DesktopGpuSettings.setTargetFps(60)
            assertEquals(60, DesktopGpuSettings.targetFps.value)

            DesktopGpuSettings.setTargetFps(120)
            assertEquals(120, DesktopGpuSettings.targetFps.value)

            // Test clamping
            DesktopGpuSettings.setTargetFps(20)
            assertEquals(30, DesktopGpuSettings.targetFps.value)

            DesktopGpuSettings.setTargetFps(75)
            assertEquals(60, DesktopGpuSettings.targetFps.value)

            DesktopGpuSettings.setTargetFps(144)
            assertEquals(120, DesktopGpuSettings.targetFps.value)

            DesktopGpuSettings.setEnabled(false)
            assertFalse(DesktopGpuSettings.isEnabled())
        } finally {
            DesktopGpuSettings.setEnabled(initialEnabled)
            DesktopGpuSettings.setTargetFps(initialFps)
        }
    }

    @Test
    fun testCompileAllGpuShadersThroughEngine() {
        val initialEnabled = DesktopGpuSettings.isEnabled()
        try {
            DesktopGpuSettings.setEnabled(true)

            // Neon Tunnel
            val tunnelShader = DesktopGpuVisualizerEngine.createShader(
                style = GpuVisualizerStyle.NEON_TUNNEL,
                width = 1920f,
                height = 1080f,
                timeSec = 2.5f,
                bass = 0.8f,
                mids = 0.4f,
                treble = 0.2f,
                primary = Color.Cyan,
                secondary = Color.Magenta,
                accent = Color.Yellow,
            )
            assertNotNull(tunnelShader, "Neon Tunnel GPU shader must compile and instantiate")
            tunnelShader.close()

            // Water Surface
            val waterShader = DesktopGpuVisualizerEngine.createShader(
                style = GpuVisualizerStyle.WATER_SURFACE,
                width = 1920f,
                height = 1080f,
                timeSec = 3.0f,
                bass = 0.6f,
                mids = 0.5f,
                treble = 0.7f,
                primary = Color.Blue,
                secondary = Color.Cyan,
                accent = Color.White,
            )
            assertNotNull(waterShader, "Water Surface GPU shader must compile and instantiate")
            waterShader.close()

            // Cosmic Particles
            val cosmicShader = DesktopGpuVisualizerEngine.createShader(
                style = GpuVisualizerStyle.COSMIC_PARTICLES,
                width = 1920f,
                height = 1080f,
                timeSec = 1.2f,
                bass = 0.9f,
                mids = 0.7f,
                treble = 0.8f,
                primary = Color(0xFF7C4DFF),
                secondary = Color(0xFFFF007F),
                accent = Color(0xFFFFD600),
            )
            assertNotNull(cosmicShader, "Cosmic Particles GPU shader must compile and instantiate")
            cosmicShader.close()
        } finally {
            DesktopGpuVisualizerEngine.release()
            DesktopGpuSettings.setEnabled(initialEnabled)
        }
    }

    @Test
    fun testGracefulFallbackWhenDisabledOrInvalidDimensions() {
        val initialEnabled = DesktopGpuSettings.isEnabled()
        try {
            DesktopGpuSettings.setEnabled(false)

            // When disabled, createShader returns null to trigger instant 2D fallback
            val shader = DesktopGpuVisualizerEngine.createShader(
                style = GpuVisualizerStyle.NEON_TUNNEL,
                width = 1920f,
                height = 1080f,
                timeSec = 1f,
                bass = 0.5f,
                mids = 0.5f,
                treble = 0.5f,
                primary = Color.White,
                secondary = Color.Gray,
                accent = Color.Yellow,
            )
            assertNull(shader, "Shader creation must return null when disabled to trigger 2D fallback")

            // Zero or negative dimension returns null
            DesktopGpuSettings.setEnabled(true)
            val zeroShader = DesktopGpuVisualizerEngine.createShader(
                style = GpuVisualizerStyle.NEON_TUNNEL,
                width = 0f,
                height = 0f,
                timeSec = 1f,
                bass = 0.5f,
                mids = 0.5f,
                treble = 0.5f,
                primary = Color.White,
                secondary = Color.Gray,
                accent = Color.Yellow,
            )
            assertNull(zeroShader, "Shader creation must return null on 0x0 dimensions")
        } finally {
            DesktopGpuVisualizerEngine.release()
            DesktopGpuSettings.setEnabled(initialEnabled)
        }
    }

    @Test
    fun testImmersiveVisualModeGpuFlag() {
        assertTrue(ImmersiveVisualMode.GPU_NEON_TUNNEL.isGpuShader)
        assertTrue(ImmersiveVisualMode.GPU_WATER_SURFACE.isGpuShader)
        assertTrue(ImmersiveVisualMode.GPU_COSMIC_PARTICLES.isGpuShader)

        assertFalse(ImmersiveVisualMode.WINAMP_WAVES.isGpuShader)
        assertFalse(ImmersiveVisualMode.WINAMP_SPECTRUM.isGpuShader)
        assertFalse(ImmersiveVisualMode.WINAMP_MILKDROP.isGpuShader)
        assertFalse(ImmersiveVisualMode.APPLE_LYRICS.isGpuShader)
    }
}
