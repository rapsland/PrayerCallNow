package com.salanow.ezannow

import android.app.Application
import com.salanow.ezannow.notification.PrayerAlarmScheduler
import com.salanow.ezannow.notification.PrayerNotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EzanNowApp : Application() {

    override fun onCreate() {
        super.onCreate()
        PrayerNotificationHelper.ensureChannel(this)
        CoroutineScope(Dispatchers.IO).launch {
            PrayerAlarmScheduler.scheduleNextAlarm(this@EzanNowApp)
        }
    }
}
