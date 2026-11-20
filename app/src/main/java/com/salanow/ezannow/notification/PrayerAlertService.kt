package com.salanow.ezannow.notification

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import com.salanow.ezannow.domain.model.NotificationSoundPreset
import com.salanow.ezannow.domain.model.PrayerType

/**
 * Short-lived foreground service started only when a background alarm fires.
 * Keeps the process awake cleanly during the 4–10s tone playback even when the
 * user has swiped EzanNow! out of Recents, then detaches the notification and stops itself.
 */
class PrayerAlertService : Service() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var wakeLock: PowerManager.WakeLock? = null
    private var timeoutRunnable: Runnable? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val isTestAlarm = intent?.getBooleanExtra(EXTRA_IS_TEST_ALARM, false) == true
        val cityName = intent?.getStringExtra(PrayerAlarmScheduler.EXTRA_CITY_NAME).orEmpty()
        val presetName = intent?.getStringExtra(PrayerAlarmScheduler.EXTRA_SOUND_PRESET)
        val soundPreset = NotificationSoundPreset.entries.firstOrNull { it.name == presetName }
            ?: NotificationSoundPreset.MELODI_1
        val customUri = intent?.getStringExtra(PrayerAlarmScheduler.EXTRA_CUSTOM_SOUND_URI).orEmpty()
        val durationSec = (intent?.getIntExtra(
            PrayerAlarmScheduler.EXTRA_SOUND_DURATION_SEC,
            GentleTonePlayer.DEFAULT_DURATION_SECONDS
        ) ?: GentleTonePlayer.DEFAULT_DURATION_SECONDS).coerceIn(
            GentleTonePlayer.MIN_DURATION_SECONDS,
            GentleTonePlayer.MAX_DURATION_SECONDS
        )

        val prayerName = intent?.getStringExtra(PrayerAlarmScheduler.EXTRA_PRAYER_NAME)
        val prayerTime = intent?.getStringExtra(PrayerAlarmScheduler.EXTRA_PRAYER_TIME).orEmpty()
        val leadMinutes = intent?.getIntExtra(PrayerAlarmScheduler.EXTRA_LEAD_MINUTES, 0) ?: 0
        val prayerType = PrayerType.entries.firstOrNull { it.name == prayerName }

        val notifId = if (isTestAlarm || prayerType == null) {
            PrayerNotificationHelper.TEST_NOTIFICATION_ID
        } else {
            PrayerNotificationHelper.notificationIdFor(prayerType, isSimulation = false)
        }

        val notification = if (isTestAlarm || prayerType == null) {
            PrayerNotificationHelper.buildTestNotification(
                context = this,
                cityName = cityName,
                soundPreset = soundPreset,
                soundDurationSeconds = durationSec
            )
        } else {
            PrayerNotificationHelper.buildPrayerNotification(
                context = this,
                prayerType = prayerType,
                prayerTimeFormatted = prayerTime,
                cityName = cityName,
                leadMinutes = leadMinutes,
                isSimulation = false,
                soundPreset = soundPreset
            )
        }

        val fgType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        } else {
            0
        }
        ServiceCompat.startForeground(this, notifId, notification, fgType)

        if (!PrayerNotificationHelper.hasNotificationPermission(this)) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf(startId)
            return START_NOT_STICKY
        }

        if (soundPreset == NotificationSoundPreset.SILENT) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
            stopSelf(startId)
            return START_NOT_STICKY
        }

        acquireWakeLock((durationSec + 5) * 1_000L)
        scheduleSafetyTimeout(startId, (durationSec + 2) * 1_000L)

        GentleTonePlayer.play(
            context = this,
            preset = soundPreset,
            customUriString = customUri,
            durationSeconds = durationSec,
            useNotificationStream = true,
            onFinished = {
                finishAlert(startId)
            }
        )

        return START_NOT_STICKY
    }

    private fun acquireWakeLock(timeoutMs: Long) {
        releaseWakeLock()
        val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
        val lock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG).apply {
            setReferenceCounted(false)
        }
        runCatching { lock.acquire(timeoutMs) }
        wakeLock = lock
    }

    private fun releaseWakeLock() {
        val lock = wakeLock
        wakeLock = null
        if (lock != null && lock.isHeld) {
            runCatching { lock.release() }
        }
    }

    private fun scheduleSafetyTimeout(startId: Int, delayMs: Long) {
        timeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        val runnable = Runnable {
            GentleTonePlayer.stop()
            finishAlert(startId)
        }
        timeoutRunnable = runnable
        mainHandler.postDelayed(runnable, delayMs)
    }

    private fun finishAlert(startId: Int) {
        timeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        timeoutRunnable = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
        releaseWakeLock()
        stopSelf(startId)
    }

    override fun onDestroy() {
        timeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        timeoutRunnable = null
        GentleTonePlayer.stop()
        releaseWakeLock()
        super.onDestroy()
    }

    companion object {
        private const val WAKE_LOCK_TAG = "EzanNow:PrayerAlertWakeLock"
        const val EXTRA_IS_TEST_ALARM = "extra_is_test_alarm"

        fun buildIntent(
            context: Context,
            isTestAlarm: Boolean,
            cityName: String,
            soundPreset: NotificationSoundPreset,
            customSoundUri: String,
            soundDurationSeconds: Int,
            prayerType: PrayerType? = null,
            prayerTimeFormatted: String = "",
            leadMinutes: Int = 0
        ): Intent {
            return Intent(context, PrayerAlertService::class.java).apply {
                putExtra(EXTRA_IS_TEST_ALARM, isTestAlarm)
                putExtra(PrayerAlarmScheduler.EXTRA_CITY_NAME, cityName)
                putExtra(PrayerAlarmScheduler.EXTRA_SOUND_PRESET, soundPreset.name)
                putExtra(PrayerAlarmScheduler.EXTRA_CUSTOM_SOUND_URI, customSoundUri)
                putExtra(PrayerAlarmScheduler.EXTRA_SOUND_DURATION_SEC, soundDurationSeconds)
                if (prayerType != null) {
                    putExtra(PrayerAlarmScheduler.EXTRA_PRAYER_NAME, prayerType.name)
                    putExtra(PrayerAlarmScheduler.EXTRA_PRAYER_TIME, prayerTimeFormatted)
                    putExtra(PrayerAlarmScheduler.EXTRA_LEAD_MINUTES, leadMinutes)
                }
            }
        }
    }
}
