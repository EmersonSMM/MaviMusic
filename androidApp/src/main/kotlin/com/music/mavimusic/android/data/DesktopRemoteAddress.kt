package com.music.mavimusic.android.data

import java.net.URI

/** Base address of the existing desktop remote; authentication stays in its web UI. */
internal object DesktopRemoteAddress {
    fun normalize(input: String): String {
        val address = input.trim()
        require(address.isNotEmpty() && address.none { it.isWhitespace() })
        val parsed = URI(if (address.contains("://")) address else "http://$address")
        require(parsed.scheme in setOf("http", "https"))
        require(!parsed.host.isNullOrBlank() && parsed.userInfo == null && parsed.query == null && parsed.fragment == null)
        require(parsed.path.isNullOrEmpty() || parsed.path == "/")
        val port = if (parsed.port == -1) 8765 else parsed.port
        require(port in 1..65535)
        return URI(parsed.scheme, null, parsed.host, port, "/", null, null).toASCIIString()
    }
}
