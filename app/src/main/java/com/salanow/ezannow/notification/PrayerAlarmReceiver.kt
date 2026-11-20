package com.salanow.ezannow.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.salanow.ezannow.domain.model.NotificationSoundPreset
import com.salanow.ezannow.domain.model.PrayerType
import com.salanow.ezannow.widget.EzanNowWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class PrayerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val kind = intent?.getStringExtra(PrayerAlarmScheduler.EXTRA_KIND)
        var fallbackNeedsAudioWait: (() -> Unit)? = null

        if (kind == PrayerAlarmScheduler.KIND_PRAYER ||
            kind == PrayerAlarmScheduler.KIND_DELAYED_TEST ||
            kind == PrayerAlarmScheduler.KIND_CUSTOM_TEST_TIME
        ) {
            val isTestAlarm = kind == PrayerAlarmScheduler.KIND_DELAYED_TEST
            val isCustomTestTime = kind == PrayerAlarmScheduler.KIND_CUSTOM_TEST_TIME
            val prayerName = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_PRAYER_NAME)
            val prayerTime = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_PRAYER_TIME).orEmpty()
            val cityName = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_CITY_NAME).orEmpty()
            val leadMinutes = intent.getIntExtra(PrayerAlarmScheduler.EXTRA_LEAD_MINUTES, 0)
            val presetName = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_SOUND_PRESET)
            val soundPreset = NotificationSoundPreset.entries.firstOrNull { it.name == presetName }
                ?: NotificationSoundPreset.MELODI_1
            val customUri = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_CUSTOM_SOUND_URI).orEmpty()
            val soundDurationSec = intent.getIntExtra(
                PrayerAlarmScheduler.EXTRA_SOUND_DURATION_SEC,
                GentleTonePlayer.DEFAULT_DURATION_SECONDS
            )
            val prayerType = PrayerType.entries.firstOrNull { it.name == prayerName }

            if (isTestAlarm || isCustomTestTime || prayerType != null) {
                val serviceIntent = PrayerAlertService.buildIntent(
                    context = context,
                    isTestAlarm = isTestAlarm,
                    cityName = cityName,
                    soundPreset = soundPreset,
                    customSoundUri = customUri,
                    soundDurationSeconds = soundDurationSec,
                    prayerType = prayerType,
                    prayerTimeFormatted = prayerTime,
                    leadMinutes = leadMinutes,
                    isCustomTestTime = isCustomTestTime
                )

                val fgsStarted = runCatching {
                    ContextCompat.startForegroundService(context, serviceIntent)
                    true
                }.getOrDefault(false)

                if (!fgsStarted) {
                    val waitLatch = CountDownLatch(1)
                    if (isCustomTestTime) {
                        PrayerNotificationHelper.showCustomTestTimeNotification(
                            context = context,
                            timeFormatted = prayerTime,
                            cityName = cityName,
                            soundPreset = soundPreset,
                            customSoundUri = customUri,
                            soundDurationSeconds = soundDurationSec,
                            onPlaybackFinished = { waitLatch.countDown() }
                        )
                    } else if (isTestAlarm || prayerType == null) {
                        PrayerNotificationHelper.showTestNotification(
                            context = context,
                            cityName = cityName,
                            soundPreset = soundPreset,
                            customSoundUri = customUri,
                            soundDurationSeconds = soundDurationSec,
                            onPlaybackFinished = { waitLatch.countDown() }
                        )
                    } else {
                        PrayerNotificationHelper.showPrayerNotification(
                            context = context,
                            prayerType = prayerType,
                            prayerTimeFormatted = prayerTime,
                            cityName = cityName,
                            leadMinutes = leadMinutes,
                            isSimulation = false,
                            soundPreset = soundPreset,
                            customSoundUri = customUri,
                            soundDurationSeconds = soundDurationSec,
                            onPlaybackFinished = { waitLatch.countDown() }
                        )
                    }
                    fallbackNeedsAudioWait = {
                        val timeoutSec = soundDurationSec.coerceIn(1, 8).toLong()
                        waitLatch.await(timeoutSec, TimeUnit.SECONDS)
                    }
                }
            }
        } else if (kind == PrayerAlarmScheduler.KIND_REFRESH) {
            // Clear all prayer notifications daily at 24:00
            PrayerNotificationHelper.cancelAllNotifications(context)
        }

        val pendingResult = goAsync()
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "EzanNow:AlarmReceiver")?.apply {
            setReferenceCounted(false)
            runCatching { acquire(9_500L) }
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (kind != PrayerAlarmScheduler.KIND_DELAYED_TEST) {
                    PrayerAlarmScheduler.scheduleNextAlarm(context)
                    PrayerNotificationHelper.refreshPersistentStatusNotification(context)
                    EzanNowWidgetProvider.updateAllWidgets(context)
                }
                fallbackNeedsAudioWait?.invoke()
            } finally {
                if (wakeLock != null && wakeLock.isHeld) {
                    runCatching { wakeLock.release() }
                }
                pendingResult.finish()
            }
        }
    }
}
