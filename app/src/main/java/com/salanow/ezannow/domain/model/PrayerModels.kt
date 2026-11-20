package com.salanow.ezannow.domain.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone

enum class PrayerType(
    val turkishName: String,
    val shortName: String,
    val isFard: Boolean
) {
    IMSAK("İmsak", "İms", true),
    GUNES("Güneş", "Gün", false),
    OGLE("Öğle", "Öğl", true),
    IKINDI("İkindi", "İkn", true),
    AKSAM("Akşam", "Akş", true),
    YATSI("Yatsı", "Yat", true)
}

enum class QadaPrayerType(val turkishName: String) {
    SABAH("Sabah"),
    OGLE("Öğle"),
    IKINDI("İkindi"),
    AKSAM("Akşam"),
    YATSI("Yatsı"),
    VITIR("Vitir")
}

data class SimpleDate(
    val year: Int,
    val month: Int,
    val day: Int
) : Comparable<SimpleDate> {

    override fun compareTo(other: SimpleDate): Int {
        if (year != other.year) return year.compareTo(other.year)
        if (month != other.month) return month.compareTo(other.month)
        return day.compareTo(other.day)
    }

    fun toCalendar(timeZone: TimeZone = TimeZone.getDefault()): Calendar {
        return GregorianCalendar(timeZone).apply {
            clear()
            set(year, month - 1, day, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    fun plusDays(days: Int): SimpleDate {
        val cal = toCalendar(TimeZone.getTimeZone("UTC"))
        cal.add(Calendar.DAY_OF_MONTH, days)
        return SimpleDate(
            year = cal.get(Calendar.YEAR),
            month = cal.get(Calendar.MONTH) + 1,
            day = cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun plusMonths(months: Int): SimpleDate {
        val cal = GregorianCalendar(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, month - 1, 1, 0, 0, 0)
        }
        cal.add(Calendar.MONTH, months)
        return SimpleDate(
            year = cal.get(Calendar.YEAR),
            month = cal.get(Calendar.MONTH) + 1,
            day = 1
        )
    }

    fun daysInMonth(): Int {
        val cal = GregorianCalendar(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, month - 1, 1, 0, 0, 0)
        }
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    fun dayOfWeekIndex(): Int {
        return toCalendar(TimeZone.getTimeZone("UTC")).get(Calendar.DAY_OF_WEEK)
    }

    fun isFriday(): Boolean = dayOfWeekIndex() == Calendar.FRIDAY

    fun isThursday(): Boolean = dayOfWeekIndex() == Calendar.THURSDAY

    fun dayOfWeekShortTr(): String = when (dayOfWeekIndex()) {
        Calendar.MONDAY -> "Pzt"
        Calendar.TUESDAY -> "Sal"
        Calendar.WEDNESDAY -> "Çar"
        Calendar.THURSDAY -> "Per"
        Calendar.FRIDAY -> "Cum"
        Calendar.SATURDAY -> "Cmt"
        Calendar.SUNDAY -> "Paz"
        else -> ""
    }

    fun dayOfWeekFullTr(): String = when (dayOfWeekIndex()) {
        Calendar.MONDAY -> "Pazartesi"
        Calendar.TUESDAY -> "Salı"
        Calendar.WEDNESDAY -> "Çarşamba"
        Calendar.THURSDAY -> "Perşembe"
        Calendar.FRIDAY -> "Cuma"
        Calendar.SATURDAY -> "Cumartesi"
        Calendar.SUNDAY -> "Pazar"
        else -> ""
    }

    fun monthNameTr(): String = MONTH_NAMES_TR.getOrElse(month - 1) { "" }

    fun shortMonthNameTr(): String = SHORT_MONTH_NAMES_TR.getOrElse(month - 1) { "" }

    fun formatFullTr(): String = "$day ${monthNameTr()} $year, ${dayOfWeekFullTr()}"

    fun formatShortTr(): String = "$day ${shortMonthNameTr()}"

    fun formatMonthYearTr(): String = "${monthNameTr()} $year"

    companion object {
        private val MONTH_NAMES_TR = listOf(
            "Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran",
            "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık"
        )

        private val SHORT_MONTH_NAMES_TR = listOf(
            "Oca", "Şub", "Mar", "Nis", "May", "Haz",
            "Tem", "Ağu", "Eyl", "Eki", "Kas", "Ara"
        )

        fun today(timeZoneId: String): SimpleDate {
            val tz = TimeZone.getTimeZone(timeZoneId)
            val cal = Calendar.getInstance(tz)
            return SimpleDate(
                year = cal.get(Calendar.YEAR),
                month = cal.get(Calendar.MONTH) + 1,
                day = cal.get(Calendar.DAY_OF_MONTH)
            )
        }
    }
}

data class PrayerTimesDay(
    val date: SimpleDate,
    val timesMillis: Map<PrayerType, Long>,
    val timeZoneId: String
) {
    fun millisOf(type: PrayerType): Long = timesMillis[type] ?: 0L

    fun formattedTime(type: PrayerType): String {
        return formatMillis(millisOf(type), timeZoneId)
    }

    companion object {
        fun formatMillis(millis: Long, timeZoneId: String): String {
            val fmt = SimpleDateFormat("HH:mm", Locale.forLanguageTag("tr-TR"))
            fmt.timeZone = TimeZone.getTimeZone(timeZoneId)
            return fmt.format(Date(millis))
        }
    }
}

enum class CalculationMethod(
    val turkishName: String,
    val fajrAngle: Double,
    val ishaAngle: Double,
    val ishaIntervalMinutes: Int? = null,
    val methodOffsets: PrayerAdjustments = PrayerAdjustments()
) {
    DIYANET(
        turkishName = "Diyanet İşleri Başkanlığı",
        fajrAngle = 18.0,
        ishaAngle = 17.0,
        methodOffsets = PrayerAdjustments(
            imsak = 0,
            gunes = -7,
            ogle = 5,
            ikindi = 4,
            aksam = 7,
            yatsi = 0
        )
    ),
    MUSLIM_WORLD_LEAGUE(
        turkishName = "Dünya İslam Birliği (MWL)",
        fajrAngle = 18.0,
        ishaAngle = 17.0
    ),
    EGYPTIAN(
        turkishName = "Mısır Genel Kurumu",
        fajrAngle = 19.5,
        ishaAngle = 17.5
    ),
    KARACHI(
        turkishName = "Karaçi İslam Üniversitesi",
        fajrAngle = 18.0,
        ishaAngle = 18.0
    ),
    UMM_AL_QURA(
        turkishName = "Ümmü'l-Kura (Mekke)",
        fajrAngle = 18.5,
        ishaAngle = 0.0,
        ishaIntervalMinutes = 90
    ),
    DUBAI(
        turkishName = "Dubai / BAE",
        fajrAngle = 18.2,
        ishaAngle = 18.2
    ),
    KUWAIT(
        turkishName = "Kuveyt",
        fajrAngle = 18.0,
        ishaAngle = 17.5
    ),
    QATAR(
        turkishName = "Katar",
        fajrAngle = 18.0,
        ishaAngle = 0.0,
        ishaIntervalMinutes = 90
    ),
    SINGAPORE(
        turkishName = "Singapur",
        fajrAngle = 20.0,
        ishaAngle = 18.0
    ),
    ISNA(
        turkishName = "Kuzey Amerika (ISNA)",
        fajrAngle = 15.0,
        ishaAngle = 15.0
    )
}

enum class AsrMadhab(
    val turkishName: String,
    val shadowFactor: Double
) {
    STANDARD("Standart (Şafii / Diyanet)", 1.0),
    HANAFI("Hanefi", 2.0)
}

enum class HighLatitudeRule(val turkishName: String) {
    MIDDLE_OF_NIGHT("Gecenin Yarısı"),
    SEVENTH_OF_NIGHT("Gecenin Yedide Biri"),
    TWILIGHT_ANGLE("Açı Bazlı")
}

data class PrayerAdjustments(
    val imsak: Int = 0,
    val gunes: Int = 0,
    val ogle: Int = 0,
    val ikindi: Int = 0,
    val aksam: Int = 0,
    val yatsi: Int = 0
) {
    fun forPrayer(type: PrayerType): Int = when (type) {
        PrayerType.IMSAK -> imsak
        PrayerType.GUNES -> gunes
        PrayerType.OGLE -> ogle
        PrayerType.IKINDI -> ikindi
        PrayerType.AKSAM -> aksam
        PrayerType.YATSI -> yatsi
    }
}

data class CityLocation(
    val id: String,
    val name: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val timeZoneId: String,
    val isGps: Boolean = false
)

sealed interface KerahatStatus {
    data object None : KerahatStatus
    data class Active(val label: String, val remainingMinutes: Int) : KerahatStatus
    data class Upcoming(val label: String, val minutesUntilStart: Int) : KerahatStatus
}

data class NextPrayerInfo(
    val currentPrayer: PrayerType,
    val nextPrayer: PrayerType,
    val nextPrayerTimeMillis: Long,
    val previousPrayerTimeMillis: Long,
    val remainingMillis: Long,
    val progress: Float
)

enum class ThemeMode(val turkishName: String) {
    SYSTEM("Sistem Varsayılanı"),
    LIGHT("Açık Tema"),
    DARK("Koyu Tema")
}

enum class NotificationSoundPreset(
    val id: String,
    val turkishName: String,
    val subtitle: String
) {
    MELODI_1("melody_soft", "Yumuşak Melodi 1", "Varsayılan yumuşak bildirim melodisi"),
    MELODI_2("melody_calm", "Sakin Melodi 2", "Sakin bildirim melodisi"),
    MELODI_3("melody_short", "Kısa Melodi 3", "Kısa bildirim melodisi"),
    NEY_1("ney_classic", "Ney Taksimi 1", "Geleneksel ney taksimi"),
    NEY_2("ney_peaceful", "Ney Taksimi 2", "Huzurlu ney tınısı"),
    KANUN_1("kanun_classic", "Kanun Taksimi", "Klasik kanun taksimi"),
    TASAVVUF_1("tasavvuf_melody", "Tasavvuf Müziği", "Tasavvuf ezgisi"),
    EZAN_1("adhan_short_1", "Kısa Ezan 1", "Kısa ezan okunuşu 1"),
    EZAN_2("adhan_short_2", "Kısa Ezan 2", "Kısa ezan okunuşu 2"),
    SABAH("fajr_call", "Essalatu Hayrun Minennevm", "Sabah namazı uyarısı"),
    ALARM("alarm_classic", "Klasik Alarm", "Kısa uyandırma alarmı"),
    SYSTEM_DEFAULT("system_default", "Cihaz Varsayılan Sesi", "Telefonun bildirim sesi"),
    CUSTOM_URI("custom_uri", "Telefondan Özel Ses", "Telefondan seçilen özel ses dosyası"),
    SILENT("silent", "Sessiz (Sadece Titreşim)", "Ses çalmaz, bildirim gösterir")
}
