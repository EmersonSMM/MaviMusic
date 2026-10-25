package com.music.mavimusic.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopAirPlayTest {

    @Test
    fun testSettingsAndLatencyClamping() {
        val initialEnabled = DesktopAirPlaySettings.isFeatureEnabled()
        val initialLatency = DesktopAirPlaySettings.latencyMs.value

        try {
            DesktopAirPlaySettings.setFeatureEnabled(true)
            assertTrue(DesktopAirPlaySettings.isFeatureEnabled())

            DesktopAirPlaySettings.setLatencyMs(2000)
            assertEquals(2000, DesktopAirPlaySettings.latencyMs.value)

            // Clamp below 0
            DesktopAirPlaySettings.setLatencyMs(-500)
            assertEquals(0, DesktopAirPlaySettings.latencyMs.value)

            // Clamp above 3000
            DesktopAirPlaySettings.setLatencyMs(5000)
            assertEquals(3000, DesktopAirPlaySettings.latencyMs.value)

            DesktopAirPlaySettings.setLatencyMs(1500)
            assertEquals(1500, DesktopAirPlaySettings.latencyMs.value)

            DesktopAirPlaySettings.setFeatureEnabled(false)
            assertFalse(DesktopAirPlaySettings.isFeatureEnabled())
        } finally {
            DesktopAirPlaySettings.setFeatureEnabled(initialEnabled)
            DesktopAirPlaySettings.setLatencyMs(initialLatency)
        }
    }

    @Test
    fun testDiscoveredDevicesUpdate() {
        val devices = listOf(
            DesktopAirPlayDevice(id = "HomePod Sala", name = "HomePod Sala", type = "HomePod"),
            DesktopAirPlayDevice(id = "Apple TV 4K", name = "Apple TV 4K", type = "Apple TV"),
        )

        DesktopAirPlaySettings.updateDiscovered(devices)
        assertEquals(2, DesktopAirPlaySettings.discoveredDevices.value.size)
        assertEquals("HomePod Sala", DesktopAirPlaySettings.discoveredDevices.value[0].name)
        assertEquals("Apple TV", DesktopAirPlaySettings.discoveredDevices.value[1].type)

        DesktopAirPlaySettings.updateDiscovered(emptyList())
        assertTrue(DesktopAirPlaySettings.discoveredDevices.value.isEmpty())
    }

    @Test
    fun testAirPlayActiveDetection() {
        val initialEnabled = DesktopAirPlaySettings.isFeatureEnabled()
        val initialDevice = DesktopAudioDevices.selected.value

        try {
            // When feature is off, isAirPlayActive is always false
            DesktopAirPlaySettings.setFeatureEnabled(false)
            DesktopAudioDevices.select("AirPlay Speaker")
            assertFalse(DesktopAirPlaySettings.isAirPlayActive())

            // When feature is on, detects AirPlay names
            DesktopAirPlaySettings.setFeatureEnabled(true)
            DesktopAudioDevices.select("HomePod Mini")
            assertTrue(DesktopAirPlaySettings.isAirPlayActive())

            DesktopAudioDevices.select("Apple TV Salon")
            assertTrue(DesktopAirPlaySettings.isAirPlayActive())

            DesktopAudioDevices.select("External Headphones")
            assertFalse(DesktopAirPlaySettings.isAirPlayActive())
        } finally {
            DesktopAudioDevices.select(initialDevice)
            DesktopAirPlaySettings.setFeatureEnabled(initialEnabled)
        }
    }

    @Test
    fun testLatencyFramesCalculation() {
        val latencyMs = 2000
        val sampleRate = 44100.0
        val expectedDelayFrames = (latencyMs * sampleRate / 1000.0).toLong()
        assertEquals(88200L, expectedDelayFrames)

        // At 48 kHz
        val sampleRate48k = 48000.0
        val expectedDelayFrames48k = (latencyMs * sampleRate48k / 1000.0).toLong()
        assertEquals(96000L, expectedDelayFrames48k)
    }
}
