package com.my.kizzy.rpc

/** One desktop identity for gateway, account requests and external artwork uploads. */
data class DiscordClientIdentity(val os: String, val userAgent: String) {
    companion object {
        val current: DiscordClientIdentity by lazy { forSystem(System.getProperty("os.name").orEmpty()) }

        fun forSystem(system: String): DiscordClientIdentity {
            val (os, platform) = when {
                system.startsWith("Windows", ignoreCase = true) -> "Windows" to "Windows NT 10.0; Win64; x64"
                system.startsWith("Mac", ignoreCase = true) -> "Mac OS X" to "Macintosh; Intel Mac OS X 10_15_7"
                else -> "Linux" to "X11; Linux x86_64"
            }
            return DiscordClientIdentity(os,
                "Mozilla/5.0 ($platform) AppleWebKit/537.36 (KHTML, like Gecko) discord/0.0.83 Chrome/120.0.0.0 Electron/28.2.10 Safari/537.36")
        }
    }
}
