package com.music.mavimusic.desktop.remote

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.music.mavimusic.desktop.DesktopPersistence
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import java.net.Inet4Address
import java.net.NetworkInterface

/** Network interface discovery, pairing URLs and native QR code generator. */
internal object DesktopRemoteDiscovery {
    private const val DEFAULT_PORT = 8765
    private const val KEY_PORT = "remote_server_port"
    private const val KEY_ENABLED = "remote_server_enabled"
    private const val KEY_ALLOW_BACKGROUND = "remote_background_allowed"

    private val persistence = DesktopPersistence()

    private val _enabled = MutableStateFlow(persistence.boolean(KEY_ENABLED, false))
    val enabled = _enabled.asStateFlow()

    private val _port = MutableStateFlow(persistence.int(KEY_PORT, DEFAULT_PORT).coerceIn(1024, 65535))
    val port = _port.asStateFlow()

    private val _allowInBackground = MutableStateFlow(persistence.boolean(KEY_ALLOW_BACKGROUND, true))
    val allowInBackground = _allowInBackground.asStateFlow()

    private val _activePort = MutableStateFlow(DEFAULT_PORT)
    val activePort = _activePort.asStateFlow()

    fun isEnabled(): Boolean = _enabled.value

    fun setEnabled(value: Boolean) {
        _enabled.value = value
        persistence.saveBoolean(KEY_ENABLED, value)
    }

    fun setPort(value: Int) {
        val clamped = value.coerceIn(1024, 65535)
        _port.value = clamped
        persistence.saveInt(KEY_PORT, clamped)
    }

    fun setAllowInBackground(value: Boolean) {
        _allowInBackground.value = value
        persistence.saveBoolean(KEY_ALLOW_BACKGROUND, value)
    }

    fun setActivePort(port: Int) {
        _activePort.value = port
    }

    /** Finds the most appropriate IPv4 LAN address for Wi-Fi / local network. */
    fun findLocalIp(): String {
        return runCatching {
            val interfaces = NetworkInterface.getNetworkInterfaces().toList()
            // Prefer Wi-Fi / Ethernet non-virtual interfaces
            val candidates = mutableListOf<Inet4Address>()
            for (iface in interfaces) {
                if (!iface.isUp || iface.isLoopback || iface.isVirtual) continue
                val name = iface.name.lowercase()
                // Skip Docker, bridge, vmware interfaces
                if (name.contains("docker") || name.contains("br-") || name.contains("vbox") || name.contains("vmnet")) continue

                for (addr in iface.inetAddresses) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress && addr.isSiteLocalAddress) {
                        candidates.add(addr)
                    }
                }
            }
            // Prefer en0 (Wi-Fi on Mac) or wlan / eth / standard LAN addresses
            candidates.firstOrNull {
                it.hostAddress.startsWith("192.168.") || it.hostAddress.startsWith("10.")
            }?.hostAddress ?: candidates.firstOrNull()?.hostAddress ?: "127.0.0.1"
        }.getOrDefault("127.0.0.1")
    }

    /** Returns full URL for manual entry. */
    fun remoteUrl(): String {
        val ip = findLocalIp()
        val port = _activePort.value
        return "http://$ip:$port"
    }

    /** Returns pairing URL with OTP token for the QR code. */
    fun pairingUrl(): String {
        val base = remoteUrl()
        val otp = DesktopRemoteSecurity.getOtp()
        return "$base/?otp=$otp"
    }

    /** Generates a high-contrast QR code ImageBitmap from [content]. */
    fun generateQrBitmap(content: String, size: Int = 260): ImageBitmap? = runCatching {
        val hints = mapOf(
            EncodeHintType.MARGIN to 1,
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.CHARACTER_SET to "UTF-8",
        )
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val width = matrix.width
        val height = matrix.height

        val pixels = IntArray(width * height)
        val black = 0xFF000000.toInt()
        val white = 0xFFFFFFFF.toInt()

        for (y in 0 until height) {
            for (x in 0 until width) {
                pixels[y * width + x] = if (matrix.get(x, y)) black else white
            }
        }

        val bitmap = Bitmap().apply {
            allocPixels(ImageInfo(width, height, ColorType.RGBA_8888, ColorAlphaType.OPAQUE))
            installPixels(pixels.toByteArray())
        }
        bitmap.asComposeImageBitmap()
    }.getOrNull()

    private fun IntArray.toByteArray(): ByteArray {
        val bytes = ByteArray(this.size * 4)
        for (i in indices) {
            val pixel = this[i]
            // Skia RGBA_8888 on Little Endian: R, G, B, A
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            val a = (pixel shr 24) and 0xFF
            bytes[i * 4 + 0] = r.toByte()
            bytes[i * 4 + 1] = g.toByte()
            bytes[i * 4 + 2] = b.toByte()
            bytes[i * 4 + 3] = a.toByte()
        }
        return bytes
    }
}
