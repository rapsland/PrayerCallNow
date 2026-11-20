package com.salanow.ezannow.notification

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.annotation.RawRes
import com.salanow.ezannow.R
import com.salanow.ezannow.domain.model.NotificationSoundPreset

object GentleTonePlayer {

    const val DEFAULT_DURATION_SECONDS = 4
    const val MIN_DURATION_SECONDS = 1
    const val MAX_DURATION_SECONDS = 10

    private val lock = Any()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var activePlayer: MediaPlayer? = null
    private val autoStopRunnable = Runnable { stop() }

    @RawRes
    fun rawResForPreset(preset: NotificationSoundPreset): Int? = when (preset) {
        NotificationSoundPreset.MELODI_1 -> R.raw.ezannow_melody_soft
        NotificationSoundPreset.MELODI_2 -> R.raw.ezannow_melody_calm
        NotificationSoundPreset.MELODI_3 -> R.raw.ezannow_melody_short
        NotificationSoundPreset.NEY_1 -> R.raw.ezannow_ney_classic
        NotificationSoundPreset.NEY_2 -> R.raw.ezannow_ney_peaceful
        NotificationSoundPreset.KANUN_1 -> R.raw.ezannow_kanun_classic
        NotificationSoundPreset.TASAVVUF_1 -> R.raw.ezannow_tasavvuf_melody
        NotificationSoundPreset.EZAN_1 -> R.raw.ezannow_adhan_short_1
        NotificationSoundPreset.EZAN_2 -> R.raw.ezannow_adhan_short_2
        NotificationSoundPreset.SABAH -> R.raw.ezannow_fajr_call
        NotificationSoundPreset.ALARM -> R.raw.ezannow_alarm_classic
        NotificationSoundPreset.SYSTEM_DEFAULT,
        NotificationSoundPreset.CUSTOM_URI,
        NotificationSoundPreset.SILENT -> null
    }

    fun play(
        context: Context,
        preset: NotificationSoundPreset = NotificationSoundPreset.MELODI_1,
        customUriString: String = "",
        durationSeconds: Int = DEFAULT_DURATION_SECONDS
    ) {
        if (preset == NotificationSoundPreset.SILENT) {
            stop()
            return
        }

        val durationMs = durationSeconds
            .coerceIn(MIN_DURATION_SECONDS, MAX_DURATION_SECONDS) * 1_000L
        val appContext = context.applicationContext
        val startRunnable = Runnable {
            synchronized(lock) {
                mainHandler.removeCallbacks(autoStopRunnable)
                stopAndReleaseLocked()

                val started = startPlayerForPreset(appContext, preset, customUriString, durationMs)
                if (!started && preset == NotificationSoundPreset.CUSTOM_URI) {
                    startPlayerForPreset(
                        appContext,
                        NotificationSoundPreset.MELODI_1,
                        "",
                        durationMs
                    )
                }
            }
        }

        if (Looper.myLooper() == Looper.getMainLooper()) {
            startRunnable.run()
        } else {
            mainHandler.post(startRunnable)
        }
    }

    private fun startPlayerForPreset(
        appContext: Context,
        preset: NotificationSoundPreset,
        customUriString: String,
        durationMs: Long
    ): Boolean {
        return runCatching {
            val player = MediaPlayer()
            activePlayer = player
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )

            val rawRes = rawResForPreset(preset)
            if (rawRes != null) {
                val afd = appContext.resources.openRawResourceFd(rawRes)
                    ?: error("Raw audio missing: $rawRes")
                afd.use { descriptor ->
                    player.setDataSource(
                        descriptor.fileDescriptor,
                        descriptor.startOffset,
                        descriptor.length
                    )
                }
            } else if (preset == NotificationSoundPreset.SYSTEM_DEFAULT) {
                val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: error("No system notification URI")
                player.setDataSource(appContext, uri)
            } else {
                if (customUriString.isBlank()) error("Empty custom sound URI")
                player.setDataSource(appContext, Uri.parse(customUriString))
            }

            player.setOnCompletionListener { mp ->
                synchronized(lock) {
                    mainHandler.removeCallbacks(autoStopRunnable)
                    if (activePlayer === mp) {
                        stopAndReleaseLocked()
                    } else {
                        runCatching { mp.release() }
                    }
                }
            }
            player.setOnErrorListener { mp, _, _ ->
                synchronized(lock) {
                    mainHandler.removeCallbacks(autoStopRunnable)
                    if (activePlayer === mp) {
                        stopAndReleaseLocked()
                    } else {
                        runCatching { mp.release() }
                    }
                }
                true
            }
            player.prepare()
            player.start()
            mainHandler.postDelayed(autoStopRunnable, durationMs)
            true
        }.onFailure {
            stopAndReleaseLocked()
        }.getOrDefault(false)
    }

    fun stop() {
        mainHandler.removeCallbacks(autoStopRunnable)
        synchronized(lock) {
            stopAndReleaseLocked()
        }
    }

    private fun stopAndReleaseLocked() {
        val player = activePlayer
        activePlayer = null
        if (player != null) {
            runCatching {
                if (player.isPlaying) {
                    player.stop()
                }
            }
            runCatching { player.release() }
        }
    }
}
