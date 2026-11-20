package com.salanow.ezannow.notification

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
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
    private var activeAudioManager: AudioManager? = null
    private var activeFocusRequest: AudioFocusRequest? = null
    private var activeOnFinished: (() -> Unit)? = null
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
        durationSeconds: Int = DEFAULT_DURATION_SECONDS,
        useNotificationStream: Boolean = false,
        onFinished: (() -> Unit)? = null
    ) {
        if (preset == NotificationSoundPreset.SILENT) {
            stop()
            dispatchFinished(onFinished)
            return
        }

        val durationMs = durationSeconds
            .coerceIn(MIN_DURATION_SECONDS, MAX_DURATION_SECONDS) * 1_000L
        val appContext = context.applicationContext
        val startRunnable = Runnable {
            val previousFinished: (() -> Unit)?
            var pendingFinished: (() -> Unit)? = null
            synchronized(lock) {
                mainHandler.removeCallbacks(autoStopRunnable)
                previousFinished = stopAndReleaseLocked()
                activeOnFinished = onFinished

                val started = startPlayerForPreset(
                    appContext = appContext,
                    preset = preset,
                    customUriString = customUriString,
                    durationMs = durationMs,
                    useNotificationStream = useNotificationStream
                )
                if (!started && preset == NotificationSoundPreset.CUSTOM_URI) {
                    val fallbackStarted = startPlayerForPreset(
                        appContext = appContext,
                        preset = NotificationSoundPreset.MELODI_1,
                        customUriString = "",
                        durationMs = durationMs,
                        useNotificationStream = useNotificationStream
                    )
                    if (!fallbackStarted) {
                        pendingFinished = stopAndReleaseLocked()
                    }
                } else if (!started) {
                    pendingFinished = stopAndReleaseLocked()
                }
            }
            dispatchFinished(previousFinished)
            dispatchFinished(pendingFinished)
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
        durationMs: Long,
        useNotificationStream: Boolean
    ): Boolean {
        return runCatching {
            val attrs = if (useNotificationStream) {
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            } else {
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            }

            requestTransientAudioFocusLocked(appContext, attrs)

            val player = MediaPlayer()
            activePlayer = player
            player.setAudioAttributes(attrs)

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
                val finished: (() -> Unit)?
                synchronized(lock) {
                    mainHandler.removeCallbacks(autoStopRunnable)
                    finished = if (activePlayer === mp) {
                        stopAndReleaseLocked()
                    } else {
                        runCatching { mp.release() }
                        null
                    }
                }
                dispatchFinished(finished)
            }
            player.setOnErrorListener { mp, _, _ ->
                val finished: (() -> Unit)?
                synchronized(lock) {
                    mainHandler.removeCallbacks(autoStopRunnable)
                    finished = if (activePlayer === mp) {
                        stopAndReleaseLocked()
                    } else {
                        runCatching { mp.release() }
                        null
                    }
                }
                dispatchFinished(finished)
                true
            }
            player.prepare()
            player.start()
            mainHandler.postDelayed(autoStopRunnable, durationMs)
            true
        }.onFailure {
            val saved = activeOnFinished
            stopAndReleaseLocked()
            activeOnFinished = saved
        }.getOrDefault(false)
    }

    fun stop() {
        mainHandler.removeCallbacks(autoStopRunnable)
        val finished = synchronized(lock) {
            stopAndReleaseLocked()
        }
        dispatchFinished(finished)
    }

    private fun requestTransientAudioFocusLocked(
        appContext: Context,
        attrs: AudioAttributes
    ) {
        val am = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        activeAudioManager = am
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(attrs)
                .setAcceptsDelayedFocusGain(false)
                .setOnAudioFocusChangeListener { }
                .build()
            activeFocusRequest = req
            runCatching { am.requestAudioFocus(req) }
        } else {
            @Suppress("DEPRECATION")
            runCatching {
                am.requestAudioFocus(
                    null,
                    AudioManager.STREAM_NOTIFICATION,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
            }
        }
    }

    private fun stopAndReleaseLocked(): (() -> Unit)? {
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
        val am = activeAudioManager
        val req = activeFocusRequest
        activeAudioManager = null
        activeFocusRequest = null
        if (am != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && req != null) {
                runCatching { am.abandonAudioFocusRequest(req) }
            } else {
                @Suppress("DEPRECATION")
                runCatching { am.abandonAudioFocus(null) }
            }
        }
        val callback = activeOnFinished
        activeOnFinished = null
        return callback
    }

    private fun dispatchFinished(callback: (() -> Unit)?) {
        if (callback == null) return
        if (Looper.myLooper() == Looper.getMainLooper()) {
            callback.invoke()
        } else {
            mainHandler.post { callback.invoke() }
        }
    }
}
