package com.music.mavimusic.android.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.music.mavimusic.android.MainActivity
import com.music.mavimusic.android.R
import com.music.mavimusic.android.audio.AndroidAudioVisualization
import com.music.mavimusic.android.audio.AndroidPlayerEngine
import com.music.mavimusic.android.audio.PcmVisualizationSink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private var rawPlayer: ExoPlayer? = null
    private val analysisScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        // Register a proper Android Media3 notification provider so Android & Samsung One UI
        // always render native music playback controls in the notification shade & lockscreen.
        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(MEDIA_CHANNEL_ID)
            .setChannelName(R.string.media_notification_channel)
            .setNotificationId(MEDIA_NOTIFICATION_ID)
            .build()
        notificationProvider.setSmallIcon(R.drawable.ic_media_notification)
        setMediaNotificationProvider(notificationProvider)

        val capture = PcmVisualizationSink(analysisScope)
        val renderers = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean,
            ): AudioSink {
                return DefaultAudioSink.Builder(context)
                    .setAudioProcessors(arrayOf(TeeAudioProcessor(capture)))
                    .setEnableFloatOutput(false)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .build()
            }
        }
        val player = ExoPlayer.Builder(this, renderers)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                /* handleAudioFocus = */ true
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
        rawPlayer = player

        // Expose MaviMusic's queue traversal to lockscreen, notification, Bluetooth, and Android Auto.
        val sessionPlayer = object : ForwardingPlayer(player) {
            override fun getAvailableCommands(): Player.Commands {
                val base = super.getAvailableCommands().buildUpon()
                if (AndroidPlayerEngine.hasNext()) {
                    base.add(Player.COMMAND_SEEK_TO_NEXT)
                    base.add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                }
                if (AndroidPlayerEngine.hasPrevious()) {
                    base.add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    base.add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                }
                return base.build()
            }

            override fun isCommandAvailable(command: Int): Boolean = when (command) {
                Player.COMMAND_SEEK_TO_NEXT, Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> AndroidPlayerEngine.hasNext()
                Player.COMMAND_SEEK_TO_PREVIOUS, Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> AndroidPlayerEngine.hasPrevious()
                else -> super.isCommandAvailable(command)
            }

            override fun hasNextMediaItem(): Boolean = AndroidPlayerEngine.hasNext()
            override fun hasPreviousMediaItem(): Boolean = AndroidPlayerEngine.hasPrevious()
            override fun seekToNext() { AndroidPlayerEngine.skipNext() }
            override fun seekToNextMediaItem() { AndroidPlayerEngine.skipNext() }
            override fun seekToPrevious() { AndroidPlayerEngine.skipPrevious() }
            override fun seekToPreviousMediaItem() { AndroidPlayerEngine.skipPrevious() }
            override fun play() { AndroidPlayerEngine.play() }
            override fun pause() { AndroidPlayerEngine.pause() }
        }

        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val session = MediaSession.Builder(this, sessionPlayer)
            .setSessionActivity(sessionActivityPendingIntent)
            .build()
        mediaSession = session
        addSession(session)

        // Ensure Android promotes the foreground media notification whenever playback begins or tracks switch.
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    onUpdateNotification(session, /* startInForegroundRequired = */ true)
                }
            }

            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                if (mediaItem != null) {
                    onUpdateNotification(session, /* startInForegroundRequired = */ player.playWhenReady)
                }
            }
        })

        AndroidPlayerEngine.init(applicationContext)
        AndroidPlayerEngine.attachPlayer(player)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = rawPlayer
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        AndroidAudioVisualization.enabled = false
        AndroidAudioVisualization.reset()
        analysisScope.cancel()
        AndroidPlayerEngine.detachPlayer()
        mediaSession?.release()
        mediaSession = null
        rawPlayer?.release()
        rawPlayer = null
        super.onDestroy()
    }

    companion object {
        const val MEDIA_CHANNEL_ID = "mavimusic_playback_channel"
        const val MEDIA_NOTIFICATION_ID = 1001
    }
}
