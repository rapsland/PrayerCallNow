package com.salanow.ezannow.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.salanow.ezannow.widget.EzanNowWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val supported = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED
        )
        if (action !in supported) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                PrayerAlarmScheduler.scheduleNextAlarm(context)
                PrayerNotificationHelper.refreshPersistentStatusNotification(context)
                EzanNowWidgetProvider.updateAllWidgets(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
