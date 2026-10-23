package com.music.mavimusic.desktop

import com.music.mavimusic.data.model.Account
import kotlin.test.*

class DesktopAccountSessionTest {
    @Test fun `saved profile is independent from authentication and local session survives a restart`() {
        // Tests use MemoryPreferencesFactory; no browser, Keychain or Google request is involved.
        val original = DesktopYouTubeAuth.session
        val scope = DesktopYouTubeAuth.Session("SAPISID=synthetic-session", dataSyncId = "test-account-session")
        val account = DesktopAccounts.save(scope.cookie, scope, Account("Persona de prueba", "test@example.invalid", null), emptyList())
        try {
            assertEquals(scope.cookie, DesktopYouTubeAuth.states.value.session?.cookie)
            assertEquals(account.accountId, DesktopAccountSnapshot.read().accountId)
            DesktopYouTubeAuth.adopt(null)
            assertNull(DesktopYouTubeAuth.states.value.session)
            assertTrue(DesktopAccountSnapshot.read().accounts.any { it.accountId == account.accountId })
            DesktopAccounts.restoreLocalSessions()
            DesktopAccounts.applyActive()
            assertEquals(scope.cookie, DesktopYouTubeAuth.states.value.session?.cookie)
            DesktopYouTubeAuth.beginChecking()
            assertTrue(DesktopYouTubeAuth.states.value.checking)
            DesktopYouTubeAuth.adopt(null, expired = true)
            assertTrue(DesktopYouTubeAuth.states.value.expired)
            assertFalse(DesktopYouTubeAuth.states.value.checking)
            assertNull(DesktopYouTubeAuth.states.value.session)
            // A manual session adoption immediately publishes the recovered session.
            DesktopYouTubeAuth.adopt(scope)
            assertEquals(scope, DesktopYouTubeAuth.states.value.session)
            assertFalse(DesktopYouTubeAuth.states.value.expired)
        } finally { DesktopAccounts.remove(account.accountId); DesktopYouTubeAuth.adopt(original) }
    }
}
