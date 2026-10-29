package com.music.mavimusic.android.data

import android.content.Context
import com.music.mavimusic.data.model.Song
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class AndroidPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("mavimusic_prefs", Context.MODE_PRIVATE)

    fun getVisualizerStyle(): String = prefs.getString("visualizer_style", "SPECTRUM") ?: "SPECTRUM"
    fun setVisualizerStyle(value: String) { prefs.edit().putString("visualizer_style", value).apply() }
    fun getReduceVisualizerMotion(): Boolean = prefs.getBoolean("visualizer_reduce_motion", false)
    fun setReduceVisualizerMotion(value: Boolean) { prefs.edit().putBoolean("visualizer_reduce_motion", value).apply() }

    fun saveLastSong(song: Song) {
        val json = songToJson(song)
        prefs.edit().putString(KEY_LAST_SONG, json.toString()).apply()
    }

    fun getLastSong(): Song? {
        val str = prefs.getString(KEY_LAST_SONG, null) ?: return null
        return try {
            jsonToSong(JSONObject(str))
        } catch (_: Exception) {
            null
        }
    }

    fun saveQueue(queue: List<Song>, index: Int) {
        val array = JSONArray()
        queue.take(50).forEach { song ->
            array.put(songToJson(song))
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
                jsonToSong(array.getJSONObject(i))?.let { list.add(it) }
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

    // --- FAVORITES (ME GUSTA) ---

    fun getFavorites(): List<Song> {
        val str = prefs.getString(KEY_FAVORITES, null) ?: return emptyList()
        return try {
            val array = JSONArray(str)
            val list = mutableListOf<Song>()
            for (i in 0 until array.length()) {
                jsonToSong(array.getJSONObject(i))?.let { list.add(it) }
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveFavorites(list: List<Song>) {
        val array = JSONArray()
        list.forEach { song ->
            array.put(songToJson(song))
        }
        prefs.edit().putString(KEY_FAVORITES, array.toString()).apply()
    }

    fun isFavorite(videoId: String): Boolean {
        return getFavorites().any { it.videoId == videoId }
    }

    fun toggleFavorite(song: Song): Boolean {
        val current = getFavorites().toMutableList()
        val index = current.indexOfFirst { it.videoId == song.videoId }
        val nowFavorite: Boolean
        if (index >= 0) {
            current.removeAt(index)
            nowFavorite = false
        } else {
            current.add(0, song)
            nowFavorite = true
        }
        saveFavorites(current)
        return nowFavorite
    }

    // --- PLAYLISTS ---

    fun getPlaylists(): List<UserPlaylist> {
        val str = prefs.getString(KEY_PLAYLISTS, null) ?: return emptyList()
        return try {
            val array = JSONArray(str)
            val list = mutableListOf<UserPlaylist>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.getString("id")
                val name = obj.getString("name")
                val songsArray = obj.optJSONArray("songs") ?: JSONArray()
                val songs = mutableListOf<Song>()
                for (j in 0 until songsArray.length()) {
                    jsonToSong(songsArray.getJSONObject(j))?.let { songs.add(it) }
                }
                list.add(UserPlaylist(id = id, name = name, songs = songs))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun savePlaylists(playlists: List<UserPlaylist>) {
        val array = JSONArray()
        playlists.forEach { pl ->
            val obj = JSONObject().apply {
                put("id", pl.id)
                put("name", pl.name)
                val songsArr = JSONArray()
                pl.songs.forEach { song -> songsArr.put(songToJson(song)) }
                put("songs", songsArr)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_PLAYLISTS, array.toString()).apply()
    }

    fun createPlaylist(name: String): UserPlaylist {
        val pl = UserPlaylist(id = UUID.randomUUID().toString(), name = name.trim())
        val current = getPlaylists().toMutableList()
        current.add(0, pl)
        savePlaylists(current)
        return pl
    }

    fun deletePlaylist(playlistId: String) {
        val current = getPlaylists().filter { it.id != playlistId }
        savePlaylists(current)
    }

    fun addSongToPlaylist(playlistId: String, song: Song) {
        val current = getPlaylists().map { pl ->
            if (pl.id == playlistId) {
                if (pl.songs.none { it.videoId == song.videoId }) {
                    pl.copy(songs = pl.songs + song)
                } else pl
            } else pl
        }
        savePlaylists(current)
    }

    fun removeSongFromPlaylist(playlistId: String, videoId: String) {
        val current = getPlaylists().map { pl ->
            if (pl.id == playlistId) {
                pl.copy(songs = pl.songs.filter { it.videoId != videoId })
            } else pl
        }
        savePlaylists(current)
    }

    // --- JSON SERIALIZATION HELPERS ---

    private fun songToJson(song: Song): JSONObject {
        return JSONObject().apply {
            put("videoId", song.videoId)
            put("title", song.title)
            put("artist", song.artist)
            put("thumbnailUrl", song.thumbnailUrl ?: "")
            put("durationText", song.durationText ?: "")
            put("albumName", song.albumName ?: "")
            put("localPath", song.localPath ?: "")
        }
    }

    private fun jsonToSong(json: JSONObject): Song? {
        return try {
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

    companion object {
        private const val KEY_LAST_SONG = "last_played_song"
        private const val KEY_QUEUE = "last_playback_queue"
        private const val KEY_QUEUE_INDEX = "last_playback_queue_index"
        private const val KEY_SEARCH_HISTORY = "search_history_v1"
        private const val KEY_FAVORITES = "user_favorites_v1"
        private const val KEY_PLAYLISTS = "user_playlists_v1"

        val defaultSearchHistory = listOf("Dua Lipa", "Coldplay", "Bad Bunny", "Queen", "The Weeknd")
    }
}
