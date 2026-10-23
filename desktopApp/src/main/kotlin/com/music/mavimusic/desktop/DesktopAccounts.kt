package com.music.mavimusic.desktop

import com.music.mavimusic.data.model.Account
import com.music.mavimusic.data.model.AccountChannel
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.security.MessageDigest

/** One Personal or Brand identity available under a Google login. */
@Serializable
data class DesktopYouTubeProfile(
    val profileId: String,
    val name: String,
    val handle: String = "",
    val avatar: String? = null,
    val pageId: String? = null,
    val dataSyncId: String? = null,
    val authUser: String = "0",
    val isBrandAccount: Boolean = false,
)

/** A Google login kept independently from every other captured login. */
@Serializable
data class DesktopGoogleAccount(
    val accountId: String,
    val name: String = "",
    val email: String = "",
    val avatar: String? = null,
    val profiles: List<DesktopYouTubeProfile> = emptyList(),
    val activeProfileId: String? = null,
    /** The browser profile this session was taken from, as the path of its cookie store. */
    val sourceProfile: String? = null,
)

/** Every Google login this app is holding, and which channel it is acting as. */
internal object DesktopAccounts {

    private const val KEY_ACCOUNTS = "google_accounts_v1"
    private const val KEY_ACTIVE_ACCOUNT = "active_google_account_v1"
    private const val KEY_ACTIVE_PROFILE = "active_google_profile_v1"

    private val _changes = kotlinx.coroutines.flow.MutableStateFlow(0L)
    val changes: kotlinx.coroutines.flow.StateFlow<Long> = _changes
    private fun changed() { _changes.update { it + 1 } }
    private var selectionRevision = 0L

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val persistence get() = DesktopPersistence()

    /** Restores local preferences without consulting Keychain. */
    fun restoreLocalSessions() {
        if (!DesktopPlatform.isMac) return
        accounts().forEach { account ->
            vault(account.accountId).read()?.let { writeCookie(account.accountId, it) }
        }
        DesktopSpotifyToken.restoreLocalCookie()
        DesktopPreferenceMigration.finish()
    }

    /** Explicit one-time recovery only; existing local sign-ins take precedence. */
    suspend fun recoverSessionsFromKeychain(): Int = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        var recovered = 0
        accounts().forEach { account ->
            if (cookieOf(account.accountId).isNullOrBlank()) {
                DesktopSecretStore.recoverMacSession(attributesFor(account.accountId))
                    ?.toString(Charsets.UTF_8)?.takeIf(String::isNotBlank)?.let { recoveredCookie ->
                        synchronized(this@DesktopAccounts) {
                            if (accounts().any { it.accountId == account.accountId } && cookieOf(account.accountId).isNullOrBlank()) {
                                writeCookie(account.accountId, recoveredCookie)
                                recovered++
                            }
                        }
                    }
            }
        }
        if (DesktopSpotifyToken.recoverCookieFromKeychain()) recovered++
        activate()
        recovered
    }

    fun accounts(): List<DesktopGoogleAccount> {
        val raw = DesktopPreferenceChunks.read(persistence.preferences, KEY_ACCOUNTS) ?: return emptyList()
        return runCatching { json.decodeFromString<List<DesktopGoogleAccount>>(raw) }.getOrDefault(emptyList())
    }

    fun activeAccountId(): String? = persistence.string(KEY_ACTIVE_ACCOUNT).takeIf { it.isNotBlank() }

    fun activeProfileId(): String? = persistence.string(KEY_ACTIVE_PROFILE).takeIf { it.isNotBlank() }

    fun active(): DesktopGoogleAccount? =
        accounts().firstOrNull { it.accountId == activeAccountId() } ?: accounts().firstOrNull()

    fun activeProfile(): DesktopYouTubeProfile? {
        val account = active() ?: return null
        return account.profiles.firstOrNull { it.profileId == activeProfileId() }
            ?: account.profiles.firstOrNull { it.profileId == account.activeProfileId }
            ?: account.profiles.firstOrNull()
    }

    /** The identity for a stored account, or null when its cookie has gone. */
    fun sessionFor(account: DesktopGoogleAccount, profile: DesktopYouTubeProfile?): DesktopYouTubeAuth.Session? {
        val cookie = cookieOf(account.accountId) ?: return null
        return DesktopYouTubeAuth.Session(
            cookie = cookie,
            authUser = profile?.authUser ?: "0",
            pageId = profile?.pageId,
            dataSyncId = profile?.dataSyncId,
        )
    }

    /** Puts the chosen account into force, renewing it first if it has gone flat. */
    internal data class Renewal(val session: DesktopYouTubeAuth.Session, val sourceProfile: String)

    suspend fun activate(
        readScope: suspend (String) -> DesktopYouTubeAuth.Session? = DesktopYouTubeSession::adoptSessionScope,
        renew: suspend (DesktopGoogleAccount) -> Renewal? = ::renewFromBrowser,
    ): DesktopYouTubeAuth.Session? {
        val token = DesktopYouTubeAuth.beginChecking()
        try {
            val request = synchronized(this) {
                val account = active() ?: run { DesktopYouTubeAuth.adopt(null); return null }
                val profile = activeProfile()
                val stored = sessionFor(account, profile) ?: run { DesktopYouTubeAuth.adopt(null); return null }
                Activation(account, profile, stored, selectionRevision, DesktopYouTubeAuth.revision)
            }
            fun current() = selectionRevision == request.selectionRevision &&
                DesktopYouTubeAuth.revision == request.authRevision
            val live = readScope(request.stored.cookie)
            synchronized(this) {
                if (!current()) return DesktopYouTubeAuth.session
                if (live != null) {
                    val healed = healedIdentity(request.account, request.profile, request.stored, live)
                    DesktopYouTubeAuth.adopt(healed)
                    return healed
                }
            }
            val renewed = renew(request.account)
            return synchronized(this) {
                // A late response must never undo a sign-in, channel switch or account deletion.
                if (!current()) return@synchronized DesktopYouTubeAuth.session
                if (renewed == null || !renewalBelongsTo(request.account, renewed.session)) {
                    DesktopYouTubeAuth.adopt(null, expired = true)
                    null
                } else {
                    writeCookie(request.account.accountId, renewed.session.cookie)
                    rememberSource(request.account, renewed.sourceProfile)
                    val healed = healedIdentity(request.account, request.profile, request.stored.copy(cookie = renewed.session.cookie, authUser = renewed.session.authUser), renewed.session)
                    DesktopYouTubeAuth.adopt(healed)
                    healed
                }
            }
        } finally { DesktopYouTubeAuth.finishChecking(token) }
    }

    private data class Activation(val account: DesktopGoogleAccount, val profile: DesktopYouTubeProfile?,
        val stored: DesktopYouTubeAuth.Session, val selectionRevision: Long, val authRevision: Long)

    internal fun renewalBelongsTo(account: DesktopGoogleAccount, session: DesktopYouTubeAuth.Session): Boolean =
        session.dataSyncId?.takeIf(String::isNotBlank) == account.accountId

    /**
     * The live shell's own answer about who this cookie acts as, unless the
     * listener deliberately picked a brand channel.
     *
     * What activate() used to do was adopt [stored] — the identity captured
     * when the login was saved — even though it had just gone and read [live],
     * the same question answered by the shell *now*. Google rotates an
     * account's `dataSyncId`, so a stored personal identity can silently go
     * stale; sending it as `onBehalfOfUser` is answered 400 INVALID_ARGUMENT
     * on every browse and next from then on, and this app's own log caught the
     * mismatch and still kept the stale side ("server shell identity differs
     * from selected profile; retaining override"). The feed 400s, radio and
     * history die, and only luck (a run that installed no override) says the
     * account is fine.
     *
     * For the account's own channel there is nothing to preserve: the shell's
     * fresh pair *is* the identity, so it is adopted, written back over the
     * stored profile, and used. A brand channel is different — there the
     * stored pair is the whole point of the override (the shell offers the
     * account's default, not the channel the listener chose), so it stands.
     * Android's equivalent guard is `loadChannels`, which re-detects profiles
     * and refreshes ids on every account load.
     */
    private fun healedIdentity(
        account: DesktopGoogleAccount,
        profile: DesktopYouTubeProfile?,
        stored: DesktopYouTubeAuth.Session,
        live: DesktopYouTubeAuth.Session,
    ): DesktopYouTubeAuth.Session {
        // No stored profile at all, or the listener did not pick a brand one:
        // the shell's answer wins, and the stored record learns from it.
        val brand = profile?.pageId?.takeIf { it.isNotBlank() }
        if (brand == null) {
            val changed = stored.dataSyncId != live.dataSyncId ||
                stored.authUser != live.authUser
            if (changed) {
                val fresh = profile?.copy(
                    dataSyncId = live.dataSyncId,
                    authUser = live.authUser,
                ) ?: DesktopYouTubeProfile(
                    profileId = live.dataSyncId ?: account.accountId,
                    name = account.name.ifBlank { "YouTube Music" },
                    handle = account.email,
                    avatar = account.avatar,
                    pageId = null,
                    dataSyncId = live.dataSyncId,
                    authUser = live.authUser,
                    isBrandAccount = false,
                )
                write(
                    accounts().map {
                        if (it.accountId == account.accountId) {
                            it.copy(
                                profiles = it.profiles.map { existing ->
                                    if (existing.profileId == fresh.profileId) fresh else existing
                                },
                                activeProfileId = fresh.profileId,
                            )
                        } else {
                            it
                        }
                    },
                )
                DesktopTrackLog.log(
                    "adopted the shell's fresh channel identity" +
                        if (stored.dataSyncId != live.dataSyncId) " (the stored one had gone stale)" else "",
                )
            }
            return live
        }
        // Brand channel: keep what the listener picked, but take the live
        // session's fresh cookie context for everything else.
        return stored
    }

    /** Reads the source browser's cookies again and saves them if they are live. */
    private suspend fun renewFromBrowser(account: DesktopGoogleAccount): Renewal? {
        val ordered = DesktopBrowserCookies.profiles()
            .sortedByDescending { it.database.toString() == account.sourceProfile }
        for (profile in ordered) {
            val jar = (DesktopBrowserCookies.read(profile) as? DesktopBrowserCookies.Result.Session)?.cookie ?: continue
            val session = DesktopYouTubeSession.adoptSessionScope(jar) ?: continue
            // A browser profile can have switched Google accounts since the last import.
            // Its filesystem path is a preference, never proof of account identity.
            if (!renewalBelongsTo(account, session)) continue
            return Renewal(session, profile.database.toString())
        }
        return null
    }

    /** Notes where a renewal came from, so the next one starts there. */
    private fun rememberSource(account: DesktopGoogleAccount, source: String) {
        if (account.sourceProfile == source) return
        write(accounts().map { if (it.accountId == account.accountId) it.copy(sourceProfile = source) else it })
    }

    /** Puts the chosen account and channel into force for this process. */
    @Synchronized fun applyActive(): DesktopYouTubeAuth.Session? {
        val account = active()
        if (account == null) {
            DesktopYouTubeAuth.adopt(null)
            return null
        }
        val session = sessionFor(account, activeProfile())
        DesktopYouTubeAuth.adopt(session)
        if (session == null) {
            DesktopTrackLog.log("account '${account.email.ifBlank { account.name }}' has no stored credential any more")
        }
        return session
    }

    /** Saves a captured login and makes it the active one. */
    @Synchronized fun save(
        cookie: String,
        scope: DesktopYouTubeAuth.Session,
        details: Account?,
        channels: List<AccountChannel>,
        sourceProfile: String? = null,
    ): DesktopGoogleAccount {
        val accountId = scope.dataSyncId?.takeIf { it.isNotBlank() } ?: digest(cookie).take(24)
        val profiles = channels.map { channel ->
            DesktopYouTubeProfile(
                profileId = channel.pageId ?: channel.dataSyncId ?: "profile:" + digest(channel.name).take(16),
                name = channel.name,
                handle = channel.subtitle,
                avatar = channel.thumbnailUrl,
                pageId = channel.pageId,
                dataSyncId = channel.dataSyncId,
                authUser = scope.authUser,
                isBrandAccount = channel.pageId != null,
            )
        }.ifEmpty {
            // No switcher answer is not the same as no channels: every login has at least the one
            // the shell is already acting as.
            listOf(
                DesktopYouTubeProfile(
                    profileId = scope.pageId ?: scope.dataSyncId ?: accountId,
                    name = details?.name?.ifBlank { null } ?: "YouTube Music",
                    handle = details?.email.orEmpty(),
                    avatar = details?.thumbnailUrl,
                    pageId = scope.pageId,
                    dataSyncId = scope.dataSyncId,
                    authUser = scope.authUser,
                    isBrandAccount = scope.pageId != null,
                ),
            )
        }
        val chosen = profiles.firstOrNull { it.pageId != null && it.pageId == scope.pageId }
            ?: profiles.firstOrNull { it.dataSyncId != null && it.dataSyncId == scope.dataSyncId }
            ?: profiles.first()
        val account = DesktopGoogleAccount(
            accountId = accountId,
            name = details?.name.orEmpty(),
            email = details?.email.orEmpty(),
            avatar = details?.thumbnailUrl,
            profiles = profiles,
            activeProfileId = chosen.profileId,
            sourceProfile = sourceProfile ?: accounts().firstOrNull { it.accountId == accountId }?.sourceProfile,
        )
        writeCookie(accountId, cookie)
        write(accounts().filterNot { it.accountId == accountId } + account)
        select(accountId, chosen.profileId)
        return account
    }

    @Synchronized fun select(accountId: String, profileId: String?) {
        selectionRevision++
        persistence.saveString(KEY_ACTIVE_ACCOUNT, accountId)
        persistence.saveString(KEY_ACTIVE_PROFILE, profileId.orEmpty())
        applyActive()
        changed()
    }

    @Synchronized fun remove(accountId: String) {
        val all = accounts()
        if (all.none { it.accountId == accountId }) return
        selectionRevision++
        val previous = active()
        val previousProfile = activeProfileId()
        DesktopSecretStore.remove(attributesFor(accountId))
        DesktopPreferenceChunks.remove(persistence.preferences, cookieFallbackKey(accountId))
        DesktopPreferenceMigration.clearLegacyCookie(cookieFallbackKey(accountId))
        val remaining = all.filterNot { it.accountId == accountId }
        write(remaining)
        val next = previous?.takeIf { it.accountId != accountId } ?: remaining.firstOrNull()
        persistence.saveString(KEY_ACTIVE_ACCOUNT, next?.accountId.orEmpty())
        persistence.saveString(KEY_ACTIVE_PROFILE,
            if (next?.accountId == previous?.accountId) previousProfile.orEmpty() else next?.activeProfileId.orEmpty())
        applyActive()
        changed()
    }

    private fun write(value: List<DesktopGoogleAccount>) {
        DesktopPreferenceChunks.write(persistence.preferences, KEY_ACCOUNTS, json.encodeToString(kotlinx.serialization.builtins.ListSerializer(DesktopGoogleAccount.serializer()), value))
        changed()
    }

    // ---- Where the credential itself lives ---------------------------------

    private fun attributesFor(accountId: String) =
        mapOf("application" to "mavimusic", "account" to accountId)

    private fun vault(accountId: String) = DesktopCookieVault(
        readSecure = { DesktopSecretStore.lookup(attributesFor(accountId)) },
        writeSecure = { bytes -> DesktopSecretStore.store("MaviMusic — Sesión de YouTube Music", attributesFor(accountId), bytes) },
        readLegacy = { DesktopPreferenceChunks.read(persistence.preferences, cookieFallbackKey(accountId))
            ?: DesktopPreferenceMigration.legacyCookie(cookieFallbackKey(accountId)) },
        clearLegacy = {
            DesktopPreferenceChunks.remove(persistence.preferences, cookieFallbackKey(accountId))
            DesktopPreferenceMigration.clearLegacyCookie(cookieFallbackKey(accountId))
        },
        requireSecure = false,
    )

    private fun writeCookie(accountId: String, cookie: String) {
        if (!vault(accountId).write(cookie)) {
            DesktopPreferenceChunks.write(persistence.preferences, cookieFallbackKey(accountId), cookie)
        }
    }

    fun cookieOf(accountId: String): String? = runCatching { vault(accountId).read() }
        .onFailure { DesktopTrackLog.log("The local Google session could not be read") }
        .getOrNull()

    private fun cookieFallbackKey(accountId: String) = "google_cookie_$accountId"

    private fun digest(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}

/** Reads the account renderers out of YouTube Music's own account endpoints. */
internal object DesktopAccountParser {

    /** Name, email and photo, from the header the account menu opens with. */
    fun account(root: JsonElement): Account? {
        val header = DesktopSearchClient.renderers(root, "activeAccountHeaderRenderer").firstOrNull() ?: return null
        val name = header.text("accountName")
        if (name.isBlank()) return null
        return Account(
            name = name,
            email = header.text("email").ifBlank { header.text("channelHandle") },
            thumbnailUrl = header.photo(),
        )
    }

    /** The channels a switcher response offers, in the order YouTube lists them. */
    fun channels(root: JsonElement): List<AccountChannel> =
        DesktopSearchClient.renderers(root, "accountItem").mapNotNull { item ->
            val name = item.text("accountName")
            if (name.isBlank()) return@mapNotNull null
            val pageId = item.findString("pageId")
            // `<accountSyncId>||<sessionSyncId>`; only the first half names the account, exactly as
            // in the shell's own DATASYNC_ID.
            val dataSyncId = item.findString("datasyncIdToken")?.substringBefore("||")?.takeIf { it.isNotBlank() }
            if (pageId == null && dataSyncId == null) return@mapNotNull null
            AccountChannel(
                name = name,
                subtitle = item.text("channelHandle").ifBlank { item.text("accountByline") },
                thumbnailUrl = item.photo(),
                pageId = pageId,
                dataSyncId = dataSyncId,
                activeOnWeb = (item["isSelected"] as? JsonPrimitive)?.content == "true",
            )
        }.distinctBy { it.key }

    /** A renderer's `runs`/`simpleText` field, whichever it used. */
    private fun JsonObject.text(field: String): String {
        val node = this[field] as? JsonObject ?: return ""
        (node["runs"] as? JsonArray)?.let { runs ->
            return runs.joinToString("") { it.jsonObject["text"]?.jsonPrimitive?.contentOrNull.orEmpty() }
        }
        return node["simpleText"]?.jsonPrimitive?.contentOrNull.orEmpty()
    }

    /** The largest thumbnail the account photo offers. */
    private fun JsonObject.photo(): String? =
        ((this["accountPhoto"] as? JsonObject)?.get("thumbnails") as? JsonArray)
            ?.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull

    /** The first value of [key] anywhere inside, whatever it is wrapped in. */
    private fun JsonElement.findString(key: String): String? = when (this) {
        is JsonObject -> (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
            ?: values.firstNotNullOfOrNull { it.findString(key) }
        is JsonArray -> firstNotNullOfOrNull { it.findString(key) }
        else -> null
    }
}
