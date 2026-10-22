package com.music.bitchord.desktop

import com.music.bitchord.data.model.artworkAt
import com.music.bitchord.data.model.CARD_ART_PX

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.decodeToImageBitmap
import java.io.File
import java.net.URI
import java.nio.file.Files

/**
 * Decoded artwork, kept to a budget and persisted to disk.
 *
 * It used to be an unbounded map. Every distinct URL a session ever loaded stayed decoded for the
 * life of the process, so browsing Explore and Search for a while was hundreds of megabytes of
 * bitmaps nothing would ever ask for again — which is where the gigabyte people reported was going.
 */
internal object DesktopArtworkCache {

    /**
     * A sixth of the heap, within reason.
     *
     * Derived rather than fixed so the budget still makes sense against whatever `-Xmx` the
     * launcher was given, and clamped at both ends so a small heap still caches something and a
     * large one does not decide it may hold half a gigabyte of thumbnails.
     */
    internal val budgetBytes: Long =
        (Runtime.getRuntime().maxMemory() / 6).coerceIn(32L * 1024 * 1024, 128L * 1024 * 1024)

    /** Four bytes a pixel, which is what every format here decodes to. */
    private val images = DesktopByteBudgetLru<ImageBitmap>(budgetBytes) {
        it.width.toLong() * it.height.toLong() * 4L
    }

    /** Everything held, dropped — what the Storage settings' "Clear image cache" does. */
    fun clear() = images.clear()

    /** Returns the local cached image file for [url] if it exists and has content. */
    fun getCachedFile(url: String?): File? {
        if (url.isNullOrBlank()) return null
        val existing = DesktopMediaCache.existing(url, "jpg")
            ?: DesktopMediaCache.existing(url, "png")
        return existing?.toFile()
    }

    /** Returns the local cached image file, or fetches and saves it to disk if not present. */
    suspend fun getOrFetchFile(url: String?): File? = withContext(Dispatchers.IO) {
        if (url.isNullOrBlank()) return@withContext null
        getCachedFile(url)?.let { return@withContext it }
        runCatching {
            val target = DesktopMediaCache.pathFor(url, "jpg")
            val connection = URI(url).toURL().openConnection().apply {
                connectTimeout = 10_000
                readTimeout = 10_000
            }
            val bytes = connection.getInputStream().use { it.readBytes() }
            if (bytes.isNotEmpty()) {
                Files.createDirectories(target.parent)
                Files.write(target, bytes)
                DesktopMediaCache.trim()
                target.toFile()
            } else {
                null
            }
        }.getOrNull()
    }

    suspend fun load(url: String?): ImageBitmap? {
        if (url.isNullOrBlank()) return null
        images.get(url)?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching {
                val cached = getCachedFile(url)
                val bytes = if (cached != null && cached.length() > 0) {
                    cached.readBytes()
                } else {
                    val target = DesktopMediaCache.pathFor(url, "jpg")
                    val downloaded = URI(url).toURL().openConnection().apply {
                        connectTimeout = 10_000
                        readTimeout = 10_000
                    }.getInputStream().use { it.readBytes() }
                    if (downloaded.isNotEmpty()) {
                        runCatching {
                            Files.createDirectories(target.parent)
                            Files.write(target, downloaded)
                            DesktopMediaCache.trim()
                        }
                    }
                    downloaded
                }
                bytes.decodeToImageBitmap()
            }.getOrNull()?.also { images.put(url, it) }
        }
    }
}

/** Artwork, fetched at the size the surface drawing it actually needs, with smooth filtering. */
@Composable
fun DesktopArtwork(
    url: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    contentDescription: String? = null,
    px: Int? = null,
) {
    val targetPx = px ?: CARD_ART_PX
    val sized = url.artworkAt(targetPx) ?: url
    val bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = sized) {
        value = withContext(Dispatchers.IO) {
            DesktopArtworkCache.load(sized)
        }
    }
    if (bitmap == null) {
        Box(modifier.background(Color(0xFF2B2B35)))
    } else {
        Image(
            painter = BitmapPainter(bitmap!!, filterQuality = FilterQuality.Medium),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier,
        )
    }
}
