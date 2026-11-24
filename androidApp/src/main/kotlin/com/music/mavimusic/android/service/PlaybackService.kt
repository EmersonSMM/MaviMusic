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
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.music.mavimusic.android.MainActivity
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
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, sessionPlayer)
            .setSessionActivity(sessionActivityPendingIntent)
            .build()

        AndroidPlayerEngine.init(applicationContext)
        AndroidPlayerEngine.attachPlayer(player)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

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
}
