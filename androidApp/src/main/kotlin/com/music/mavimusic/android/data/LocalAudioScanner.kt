package com.music.mavimusic.android.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.music.mavimusic.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object LocalAudioScanner {
    suspend fun scanLocalTracks(context: Context): List<Song> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Song>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )

            cursor?.use {
                val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

                while (it.moveToNext()) {
                    val id = it.getLong(idCol)
                    val title = it.getString(titleCol) ?: "Canción local"
                    val artist = it.getString(artistCol)?.takeIf { a -> a != "<unknown>" } ?: "Archivo local"
                    val album = it.getString(albumCol)?.takeIf { al -> al != "<unknown>" }
                    val durationMs = it.getLong(durationCol)
                    val path = it.getString(dataCol)

                    val minutes = (durationMs / 1000) / 60
                    val seconds = (durationMs / 1000) % 60
                    val durText = "%d:%02d".format(minutes, seconds)

                    val contentUri: Uri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    list.add(
                        Song(
                            videoId = "local_$id",
                            title = title,
                            artist = artist,
                            thumbnailUrl = null,
                            durationText = durText,
                            albumName = album,
                            localPath = path ?: contentUri.toString()
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        list
    }
}
