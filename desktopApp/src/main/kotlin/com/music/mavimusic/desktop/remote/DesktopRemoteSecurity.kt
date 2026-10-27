package com.music.mavimusic.desktop.remote

import com.music.mavimusic.desktop.DesktopPersistence
import com.music.mavimusic.desktop.DesktopTrackLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.InetAddress
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID

/** Security, pairing, private IP validation and token session store for MaviMusic Remote. */
internal object DesktopRemoteSecurity {
    private const val KEY_AUTHORIZED_DEVICES = "remote_authorized_devices"
    private const val KEY_REQUIRE_APPROVAL = "remote_require_approval"
    private const val KEY_BACKGROUND_ALLOWED = "remote_background_allowed"
    private const val KEY_SERVER_PORT = "remote_server_port"
    private const val KEY_REMOTE_ENABLED = "remote_server_enabled"

    private val secureRandom = SecureRandom()
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Serializable
    data class DeviceSession(
        val id: String,
        val name: String,
        val tokenHash: String,
        val pairedAtMs: Long,
        val lastSeenAtMs: Long,
    )

    private val _devices = MutableStateFlow<List<DeviceSession>>(emptyList())
    val devices = _devices.asStateFlow()

    private var currentPin = generatePin()
    private var otpToken: String = generateRandomToken()
    private var otpExpiresAtMs: Long = System.currentTimeMillis() + 5 * 60 * 1000L

    private var failedAttempts = 0
    private var lockoutUntilMs = 0L

    init {
        loadDevices()
    }

    fun getPin(): String = synchronized(this) { currentPin }

    fun regeneratePin(): String = synchronized(this) {
        currentPin = generatePin()
        regenerateOtp()
        currentPin
    }

    fun getOtp(): String = synchronized(this) {
        val now = System.currentTimeMillis()
        if (now > otpExpiresAtMs) {
            regenerateOtp()
        }
        otpToken
    }

    fun regenerateOtp(): String = synchronized(this) {
        otpToken = generateRandomToken()
        otpExpiresAtMs = System.currentTimeMillis() + 5 * 60 * 1000L
        otpToken
    }

    private fun generatePin(): String = "%04d".format(secureRandom.nextInt(10_000))

    private fun generateRandomToken(): String {
        val bytes = ByteArray(24)
        secureRandom.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /** Checks whether an IP address belongs to standard private LAN subnets or loopback. */
    fun isPrivateNetwork(address: InetAddress): Boolean {
        if (address.isLoopbackAddress || address.isSiteLocalAddress || address.isLinkLocalAddress) {
            return true
        }
        val bytes = address.address
        if (bytes.size == 4) {
            val b0 = bytes[0].toInt() and 0xFF
            val b1 = bytes[1].toInt() and 0xFF
            // 10.0.0.0/8
            if (b0 == 10) return true
            // 172.16.0.0/12
            if (b0 == 172 && b1 in 16..31) return true
            // 192.168.0.0/16
            if (b0 == 192 && b1 == 168) return true
            // 169.254.0.0/16 (link local)
            if (b0 == 169 && b1 == 254) return true
            // 127.0.0.0/8 (loopback)
            if (b0 == 127) return true
        }
        return false
    }

    /** Attempts to pair a new device using PIN or OTP. Returns session token on success. */
    fun pair(pinAttempt: String?, otpAttempt: String?, deviceName: String): PairResult = synchronized(this) {
        val now = System.currentTimeMillis()
        if (now < lockoutUntilMs) {
            val remainingSec = ((lockoutUntilMs - now) / 1000L).coerceAtLeast(1)
            return PairResult.Error("Demasiados intentos fallidos. Bloqueado por $remainingSec segundos.")
        }

        var authorized = false
        if (!otpAttempt.isNullOrBlank()) {
            if (now <= otpExpiresAtMs && constantTimeEquals(otpAttempt, otpToken)) {
                authorized = true
                // Invalidate OTP after first successful use
                regenerateOtp()
            }
        } else if (!pinAttempt.isNullOrBlank()) {
            if (constantTimeEquals(pinAttempt.trim(), currentPin)) {
                authorized = true
            }
        }

        if (!authorized) {
            failedAttempts++
            if (failedAttempts >= 5) {
                lockoutUntilMs = now + 2 * 60 * 1000L // 2 min lockout
                failedAttempts = 0
                DesktopTrackLog.log("Remote security: PIN brute-force detected. Lockout applied for 2 minutes.")
            }
            return PairResult.Error("PIN o código de emparejamiento inválido o expirado.")
        }

        // Success
        failedAttempts = 0
        lockoutUntilMs = 0L

        val token = generateRandomToken() + generateRandomToken()
        val tokenHash = sha256(token)
        val deviceId = UUID.randomUUID().toString()
        val cleanName = deviceName.trim().ifBlank { "Navegador Web" }.take(50)

        val newSession = DeviceSession(
            id = deviceId,
            name = cleanName,
            tokenHash = tokenHash,
            pairedAtMs = now,
            lastSeenAtMs = now,
        )

        val updated = _devices.value + newSession
        _devices.value = updated
        persistDevices(updated)

        DesktopTrackLog.log("Remote security: paired new device '$cleanName' ($deviceId)")
        PairResult.Success(token = token, deviceId = deviceId, deviceName = cleanName)
    }

    /** Validates a session token. Returns the device session if valid, null otherwise. */
    fun validateToken(token: String?): DeviceSession? = synchronized(this) {
        if (token.isNullOrBlank()) return null
        val hash = sha256(token.trim())
        val list = _devices.value
        val match = list.firstOrNull { constantTimeEquals(it.tokenHash, hash) } ?: return null

        val now = System.currentTimeMillis()
        // Update last seen if more than 30s have passed
        if (now - match.lastSeenAtMs > 30_000L) {
            val updated = list.map {
                if (it.id == match.id) it.copy(lastSeenAtMs = now) else it
            }
            _devices.value = updated
            persistDevices(updated)
        }
        match
    }

    fun revokeDevice(deviceId: String) = synchronized(this) {
        val updated = _devices.value.filterNot { it.id == deviceId }
        _devices.value = updated
        persistDevices(updated)
        DesktopRemoteServer.disconnectDevice(deviceId)
        DesktopTrackLog.log("Remote security: revoked device $deviceId")
    }

    fun revokeAllDevices() = synchronized(this) {
        val old = _devices.value
        _devices.value = emptyList()
        persistDevices(emptyList())
        failedAttempts = 0
        lockoutUntilMs = 0L
        regeneratePin()
        old.forEach { DesktopRemoteServer.disconnectDevice(it.id) }
        DesktopTrackLog.log("Remote security: revoked all devices and rotated PIN")
    }

    private fun loadDevices() {
        val raw = DesktopPersistence().string(KEY_AUTHORIZED_DEVICES, "[]")
        val parsed = runCatching { json.decodeFromString<List<DeviceSession>>(raw) }.getOrDefault(emptyList())
        _devices.value = parsed
    }

    private fun persistDevices(list: List<DeviceSession>) {
        val raw = json.encodeToString(list)
        DesktopPersistence().saveString(KEY_AUTHORIZED_DEVICES, raw)
    }

    internal fun sha256(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    internal fun constantTimeEquals(a: String, b: String): Boolean {
        return MessageDigest.isEqual(a.toByteArray(Charsets.UTF_8), b.toByteArray(Charsets.UTF_8))
    }

    sealed interface PairResult {
        data class Success(val token: String, val deviceId: String, val deviceName: String) : PairResult
        data class Error(val message: String) : PairResult
    }
}
