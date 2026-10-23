package com.music.mavimusic.desktop

import java.nio.file.Files
import java.nio.file.Path
import org.bytedeco.ffmpeg.avformat.AVFormatContext
import org.bytedeco.ffmpeg.avformat.AVInputFormat
import org.bytedeco.ffmpeg.avutil.AVDictionary
import org.bytedeco.ffmpeg.global.avformat.*
import org.bytedeco.ffmpeg.global.avutil.*

internal data class DesktopLocalTags(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val trackNumber: Int? = null,
    val durationSeconds: Long? = null,
    val artworkUri: String? = null,
)

/** Read container headers with the decoder already bundled with the player. */
internal object DesktopLocalMetadata {
    private val cache = object : LinkedHashMap<String, DesktopLocalTags>(128, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, DesktopLocalTags>?) = size > 4096
    }

    @Synchronized
    fun read(path: Path): DesktopLocalTags = runCatching {
        val key = "${path.toAbsolutePath()}|${Files.size(path)}|${Files.getLastModifiedTime(path)}"
        cache[key]?.takeIf { it.artworkUri == null || Files.isRegularFile(Path.of(java.net.URI(it.artworkUri))) }
            ?.let { return it }
        val context = AVFormatContext(null)
        try {
            if (avformat_open_input(context, path.toAbsolutePath().toString(), null as AVInputFormat?, null as AVDictionary?) < 0) {
                return DesktopLocalTags()
            }
            avformat_find_stream_info(context, null as org.bytedeco.javacpp.PointerPointer<*>?)
            fun tag(name: String): String? {
                fun value(tags: AVDictionary?): String? = av_dict_get(tags, name, null, 0)
                    ?.takeUnless { it.isNull }?.value()?.string?.trim()?.takeIf(String::isNotBlank)
                value(context.metadata())?.let { return it }
                // Vorbis/Opus tags are stored on the audio stream rather than on the container.
                for (index in 0 until context.nb_streams()) {
                    val stream = context.streams(index)
                    if (stream.codecpar().codec_type() == AVMEDIA_TYPE_AUDIO) {
                        value(stream.metadata())?.let { return it }
                    }
                }
                return null
            }
            var artwork: String? = null
            for (index in 0 until context.nb_streams()) {
                val stream = context.streams(index)
                if (stream.disposition() and AV_DISPOSITION_ATTACHED_PIC == 0) continue
                val packet = stream.attached_pic()
                // A malformed tag must not allocate an unbounded byte array.
                if (packet.size() !in 1..(16 * 1024 * 1024)) continue
                val target = DesktopMediaCache.pathFor("local-cover:$key", "jpg")
                if (!Files.isRegularFile(target)) {
                    val bytes = ByteArray(packet.size())
                    packet.data().get(bytes)
                    Files.createDirectories(target.parent)
                    Files.write(target, bytes)
                }
                artwork = target.toUri().toString()
                break
            }
            DesktopLocalTags(
                title = tag("title"), artist = tag("artist") ?: tag("album_artist") ?: tag("albumartist"), album = tag("album"),
                trackNumber = (tag("track") ?: tag("tracknumber"))?.substringBefore('/')?.toIntOrNull()?.takeIf { it > 0 },
                durationSeconds = context.duration().takeIf { it > 0 && it != AV_NOPTS_VALUE }?.div(AV_TIME_BASE),
                artworkUri = artwork,
            ).also { cache[key] = it }
        } finally {
            avformat_close_input(context)
        }
    }.getOrDefault(DesktopLocalTags())
}
