package com.salanow.ezannow.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.salanow.ezannow.domain.model.NotificationSoundPreset
import com.salanow.ezannow.domain.model.PrayerType
import com.salanow.ezannow.widget.EzanNowWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PrayerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val kind = intent?.getStringExtra(PrayerAlarmScheduler.EXTRA_KIND)
        if (kind == PrayerAlarmScheduler.KIND_PRAYER) {
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
            if (prayerType != null) {
                PrayerNotificationHelper.showPrayerNotification(
                    context = context,
                    prayerType = prayerType,
                    prayerTimeFormatted = prayerTime,
                    cityName = cityName,
                    leadMinutes = leadMinutes,
                    isSimulation = false,
                    soundPreset = soundPreset,
                    customSoundUri = customUri,
                    soundDurationSeconds = soundDurationSec
                )
            }
        } else if (kind == PrayerAlarmScheduler.KIND_REFRESH) {
            // Clear all prayer notifications daily at 24:00
            PrayerNotificationHelper.cancelAllNotifications(context)
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                PrayerAlarmScheduler.scheduleNextAlarm(context)
                EzanNowWidgetProvider.updateAllWidgets(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
