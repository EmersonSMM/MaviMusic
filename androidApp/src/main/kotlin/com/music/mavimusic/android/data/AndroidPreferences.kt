package com.music.mavimusic.android.data

import android.content.Context
import com.music.mavimusic.data.model.Song
import org.json.JSONArray
import org.json.JSONObject

class AndroidPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("mavimusic_prefs", Context.MODE_PRIVATE)

    fun saveLastSong(song: Song) {
        val json = JSONObject().apply {
            put("videoId", song.videoId)
            put("title", song.title)
            put("artist", song.artist)
            put("thumbnailUrl", song.thumbnailUrl ?: "")
            put("durationText", song.durationText ?: "")
            put("albumName", song.albumName ?: "")
            put("localPath", song.localPath ?: "")
        }
        prefs.edit().putString(KEY_LAST_SONG, json.toString()).apply()
    }

    fun getLastSong(): Song? {
        val str = prefs.getString(KEY_LAST_SONG, null) ?: return null
        return try {
            val json = JSONObject(str)
            Song(
                videoId = json.getString("videoId"),
                title = json.getString("title"),
                artist = json.getString("artist"),
                thumbnailUrl = json.optString("thumbnailUrl").takeIf { it.isNotBlank() },
                durationText = json.optString("durationText").takeIf { it.isNotBlank() },
                albumName = json.optString("albumName").takeIf { it.isNotBlank() },
                localPath = json.optString("localPath").takeIf { it.isNotBlank() }
            )
        } catch (_: Exception) {
            null
        }
    }

    fun saveQueue(queue: List<Song>, index: Int) {
        val array = JSONArray()
        queue.take(50).forEach { song ->
            val obj = JSONObject().apply {
                put("videoId", song.videoId)
                put("title", song.title)
                put("artist", song.artist)
                put("thumbnailUrl", song.thumbnailUrl ?: "")
                put("durationText", song.durationText ?: "")
                put("albumName", song.albumName ?: "")
            }
            array.put(obj)
        }
        prefs.edit()
            .putString(KEY_QUEUE, array.toString())
            .putInt(KEY_QUEUE_INDEX, index)
            .apply()
    }

    fun getQueue(): Pair<List<Song>, Int>? {
        val str = prefs.getString(KEY_QUEUE, null) ?: return null
        val index = prefs.getInt(KEY_QUEUE_INDEX, 0)
        return try {
            val array = JSONArray(str)
            val list = mutableListOf<Song>()
            for (i in 0 until array.length()) {
                val json = array.getJSONObject(i)
                list.add(
                    Song(
                        videoId = json.getString("videoId"),
                        title = json.getString("title"),
                        artist = json.getString("artist"),
                        thumbnailUrl = json.optString("thumbnailUrl").takeIf { it.isNotBlank() },
                        durationText = json.optString("durationText").takeIf { it.isNotBlank() },
                        albumName = json.optString("albumName").takeIf { it.isNotBlank() }
                    )
                )
            }
            if (list.isNotEmpty()) Pair(list, index.coerceIn(0, list.size - 1)) else null
        } catch (_: Exception) {
            null
        }
    }

    fun saveSearchHistory(history: List<String>) {
        val array = JSONArray()
        history.take(15).forEach { array.put(it) }
        prefs.edit().putString(KEY_SEARCH_HISTORY, array.toString()).apply()
    }

    fun getSearchHistory(): List<String> {
        val str = prefs.getString(KEY_SEARCH_HISTORY, null) ?: return defaultSearchHistory
        return try {
            val array = JSONArray(str)
            val list = mutableListOf<String>()
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
            if (list.isNotEmpty()) list else defaultSearchHistory
        } catch (_: Exception) {
            defaultSearchHistory
        }
    }

    companion object {
        private const val KEY_LAST_SONG = "last_played_song"
        private const val KEY_QUEUE = "last_playback_queue"
        private const val KEY_QUEUE_INDEX = "last_playback_queue_index"
        private const val KEY_SEARCH_HISTORY = "search_history_v1"

        val defaultSearchHistory = listOf("Dua Lipa", "Coldplay", "Bad Bunny", "Queen", "The Weeknd")
    }
}
