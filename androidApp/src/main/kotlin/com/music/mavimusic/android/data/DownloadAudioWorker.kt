package com.music.mavimusic.android.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.music.mavimusic.data.model.Song
import kotlinx.coroutines.CancellationException

/**
 * WorkManager worker that downloads and verifies an offline audio track so downloads
 * survive app backgrounding and process death.
 */
class DownloadAudioWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val videoId = inputData.getString(KEY_VIDEO_ID)?.takeIf { it.isNotBlank() } ?: return Result.failure()
        val title = inputData.getString(KEY_TITLE).orEmpty()
        val artist = inputData.getString(KEY_ARTIST).orEmpty()
        val thumbnailUrl = inputData.getString(KEY_THUMBNAIL_URL)?.takeIf { it.isNotBlank() }
        val durationText = inputData.getString(KEY_DURATION_TEXT)?.takeIf { it.isNotBlank() }
        val albumName = inputData.getString(KEY_ALBUM_NAME)?.takeIf { it.isNotBlank() }

        OfflineDownloadManager.init(applicationContext)

        val song = Song(
            videoId = videoId,
            title = title,
            artist = artist,
            thumbnailUrl = thumbnailUrl,
            durationText = durationText,
            albumName = albumName,
        )

        var lastNotifiedPct = -1
        notifyProgress(videoId, title, 0)
        return try {
            val ok = OfflineDownloadManager.performDownload(song) { fraction ->
                val pct = if (fraction >= 0f) (fraction * 100f).toInt().coerceIn(0, 100) else -1
                setProgressAsync(workDataOf(KEY_PROGRESS to fraction))
                if (pct < 0 || pct - lastNotifiedPct >= 10 || pct == 100) {
                    lastNotifiedPct = pct
                    notifyProgress(videoId, title, pct)
                }
            }
            clearNotification(videoId)
            if (ok) Result.success() else Result.failure()
        } catch (cancelled: CancellationException) {
            clearNotification(videoId)
            throw cancelled
        } catch (_: Exception) {
            clearNotification(videoId)
            Result.failure()
        }
    }

    private fun canNotify(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun notifyProgress(videoId: String, title: String, percent: Int) {
        if (!canNotify()) return
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        ensureChannel(manager)
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title.ifBlank { "Descargando pista" })
            .setContentText(if (percent >= 0) "Descargando $percent %" else "Descargando…")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setProgress(100, percent.coerceAtLeast(0), percent < 0)
            .build()
        runCatching { manager.notify(notificationId(videoId), notification) }
    }

    private fun clearNotification(videoId: String) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        runCatching { manager.cancel(notificationId(videoId)) }
    }

    private fun ensureChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Descargas sin conexión",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "Progreso de descargas de audio en segundo plano"
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    private fun notificationId(videoId: String): Int = 4200 + (videoId.hashCode() and 0x0FFF)

    companion object {
        const val CHANNEL_ID = "mavimusic_offline_downloads"
        const val WORK_TAG = "mavimusic_offline_download"
        const val KEY_VIDEO_ID = "videoId"
        const val KEY_TITLE = "title"
        const val KEY_ARTIST = "artist"
        const val KEY_THUMBNAIL_URL = "thumbnailUrl"
        const val KEY_DURATION_TEXT = "durationText"
        const val KEY_ALBUM_NAME = "albumName"
        const val KEY_PROGRESS = "progress"

        fun uniqueWorkName(videoId: String): String = "download_$videoId"

        fun inputDataFor(song: Song): Data = workDataOf(
            KEY_VIDEO_ID to song.videoId,
            KEY_TITLE to song.title,
            KEY_ARTIST to song.artist,
            KEY_THUMBNAIL_URL to (song.thumbnailUrl ?: ""),
            KEY_DURATION_TEXT to (song.durationText ?: ""),
            KEY_ALBUM_NAME to (song.albumName ?: ""),
        )
    }
}
