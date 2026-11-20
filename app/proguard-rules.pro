# Keep Android app components referenced in AndroidManifest.xml
-keep public class com.salanow.ezannow.EzanNowApp
-keep public class com.salanow.ezannow.MainActivity
-keep public class com.salanow.ezannow.notification.** extends android.content.BroadcastReceiver
-keep public class com.salanow.ezannow.widget.** extends android.appwidget.AppWidgetProvider
