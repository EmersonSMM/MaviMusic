package com.music.mavimusic.desktop

import com.music.mavimusic.data.model.Account
import kotlinx.coroutines.*
import kotlin.test.*

class DesktopAccountRaceTest {
    private fun save(id: String, cookie: String = "SAPISID=synthetic-$id", source: String? = null): DesktopGoogleAccount {
        val session = DesktopYouTubeAuth.Session(cookie, dataSyncId = id)
        return DesktopAccounts.save(cookie, session, Account(id, "$id@example.invalid", null), emptyList(), source)
    }
    @Test fun `deleting an inactive account keeps the chosen account and removes every cookie chunk`() {
        val original = DesktopYouTubeAuth.session
        val removed = save("remove-chunked", "SAPISID=" + "synthetic".repeat(2000))
        val active = save("keep-active")
        try {
            val profile = DesktopAccounts.activeProfileId()
            assertNotNull(DesktopAccounts.cookieOf(removed.accountId))
            DesktopAccounts.remove(removed.accountId)
            assertNull(DesktopAccounts.cookieOf(removed.accountId))
            assertFalse(DesktopPersistence().preferences.keys().any { it.startsWith("google_cookie_${removed.accountId}") })
            assertEquals(active.accountId, DesktopAccounts.activeAccountId())
            assertEquals(profile, DesktopAccounts.activeProfileId())
            assertEquals("SAPISID=synthetic-keep-active", DesktopYouTubeAuth.session?.cookie)
        } finally { DesktopAccounts.remove(removed.accountId); DesktopAccounts.remove(active.accountId); DesktopYouTubeAuth.adopt(original) }
    }
    @Test fun `a delayed startup validation cannot undo a newer selected account`() = runBlocking {
        val original = DesktopYouTubeAuth.session
        val first = save("first-race")
        var second: DesktopGoogleAccount? = null
        try {
            val started = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val validation = async { DesktopAccounts.activate(readScope = { cookie ->
                started.complete(Unit); release.await(); DesktopYouTubeAuth.Session(cookie, dataSyncId = first.accountId)
            }, renew = { error("A valid session needs no renewal") }) }
            started.await()
            second = save("second-race")
            val selected = DesktopYouTubeAuth.session
            release.complete(Unit)
            assertEquals(selected, validation.await())
            assertEquals(second.accountId, DesktopAccounts.activeAccountId())
            assertEquals(selected, DesktopYouTubeAuth.session)
        } finally { DesktopAccounts.remove(first.accountId); second?.let { DesktopAccounts.remove(it.accountId) }; DesktopYouTubeAuth.adopt(original) }
    }
    @Test fun `browser source path alone cannot authorize an account substitution`() = runBlocking {
        val original = DesktopYouTubeAuth.session
        val account = save("right-account", source = "/same/browser/profile")
        try {
            val other = DesktopYouTubeAuth.Session("SAPISID=synthetic-other", dataSyncId = "wrong-account")
            assertFalse(DesktopAccounts.renewalBelongsTo(account, other))
            assertNull(DesktopAccounts.activate(readScope = { null }, renew = { DesktopAccounts.Renewal(other, "/same/browser/profile") }))
            assertNull(DesktopYouTubeAuth.session)
            assertTrue(DesktopYouTubeAuth.states.value.expired)
            assertEquals("SAPISID=synthetic-right-account", DesktopAccounts.cookieOf(account.accountId))
        } finally { DesktopAccounts.remove(account.accountId); DesktopYouTubeAuth.adopt(original) }
    }
    @Test fun `an older check cannot turn off the newer check's indicator`() {
        val original = DesktopYouTubeAuth.session
        try {
            val old = DesktopYouTubeAuth.beginChecking()
            val current = DesktopYouTubeAuth.beginChecking()
            DesktopYouTubeAuth.finishChecking(old)
            assertTrue(DesktopYouTubeAuth.states.value.checking)
            DesktopYouTubeAuth.finishChecking(current)
            assertFalse(DesktopYouTubeAuth.states.value.checking)
        } finally { DesktopYouTubeAuth.adopt(original) }
    }
}
