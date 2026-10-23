package com.music.mavimusic.desktop

import com.music.mavimusic.data.model.Song
import com.music.mavimusic.data.model.artworkAt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

/** Export artwork is fetched independently of what the UI happened to cache. */
internal object DesktopExportArtwork {
    suspend fun resolve(song: Song, temporaryDirectory: Path): File? = withContext(Dispatchers.IO) {
        val urls = listOfNotNull(song.artworkAt(600), song.thumbnailUrl).distinct()
        for (url in urls) {
            // FFmpeg decodes WebP as well as JPEG/PNG; the cache's file suffix is not its format.
            DesktopArtworkCache.getCachedFile(url)?.takeIf { it.length() > 0 }?.let { return@withContext it }
            val file = Files.createTempFile(temporaryDirectory, ".mavimusic-cover-", ".image")
            val fetched = runCatching {
                val request = Request.Builder().url(url).header("Accept", "image/*").build()
                DesktopDownloadHttp.client.newCall(request).execute().use { response ->
                    check(response.isSuccessful) { "Artwork download failed (HTTP ${response.code})" }
                    val bytes = response.body.byteStream().use { it.readNBytes(MAX_BYTES + 1) }
                    check(bytes.isNotEmpty() && bytes.size <= MAX_BYTES) { "Invalid artwork size" }
                    Files.write(file, bytes)
                }
            }.isSuccess
            if (fetched) return@withContext file.toFile()
            Files.deleteIfExists(file)
        }
        null
    }

    private const val MAX_BYTES = 12 * 1024 * 1024
}
