package com.music.mavimusic.android.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidYouTubeAuthTest {

    @Test
    fun `detects valid signing cookies for YouTube Music session`() {
        assertTrue(AndroidYouTubeAuth.hasAuthCookies("VISITOR_INFO1_LIVE=abc; SAPISID=xyz123; PREF=hl=es"))
        assertTrue(AndroidYouTubeAuth.hasAuthCookies("__Secure-3PAPISID=secret456; SID=1"))
        assertTrue(AndroidYouTubeAuth.hasAuthCookies("__Secure-1PAPISID=secret789"))
    }

    @Test
    fun `rejects empty or anonymous cookies without signing secret`() {
        assertFalse(AndroidYouTubeAuth.hasAuthCookies(null))
        assertFalse(AndroidYouTubeAuth.hasAuthCookies(""))
        assertFalse(AndroidYouTubeAuth.hasAuthCookies("VISITOR_INFO1_LIVE=abc; PREF=hl=es"))
        assertFalse(AndroidYouTubeAuth.hasAuthCookies("SAPISID=; PREF=1"))
    }
}
