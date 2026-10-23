package com.music.mavimusic.desktop

/** One snapshot for the account menu, avatar and library. Reading it never opens Keychain. */
internal data class DesktopAccountSnapshot(val accounts: List<DesktopGoogleAccount>, val accountId: String?, val profileId: String?) {
    companion object {
        fun read() = synchronized(DesktopAccounts) {
            DesktopAccountSnapshot(DesktopAccounts.accounts(), DesktopAccounts.activeAccountId(), DesktopAccounts.activeProfileId())
        }
    }
}
