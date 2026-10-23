package com.music.mavimusic.data.presence

enum class PresencePlatform(val wire: String) { Pc("pc") }

/**
 * Tells the MaviMusic server this install is open, so it can publish how many
 * apps are open right now (`GET /api/stats/live`).
 *
 * While open, one small POST goes out every few minutes. The server sets the
 * interval in each reply, so it can slow every client down without an update.
 * Closing sends one more POST so the count drops straight away; if that one is
 * lost, the server forgets the install a minute or two after its last ping.
 *
 * Only a random per-install UUID and the platform are sent, never anything
 * about the listener or what they play. This always goes to the official
 * server, not a custom Listen Together one, so the public count is only ours.
 */
class Presence(
    private val platform: PresencePlatform,
    private val installId: String,
    private val endpoint: String = ENDPOINT,
) {
    fun setOpen(isOpen: Boolean) {
        // Telemetry disabled
    }

    fun closeBlocking() {
        // Telemetry disabled
    }

    companion object {
        const val ENDPOINT = ""
    }
}
