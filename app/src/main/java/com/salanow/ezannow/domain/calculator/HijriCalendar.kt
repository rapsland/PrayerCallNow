package com.salanow.ezannow.domain.calculator

import com.salanow.ezannow.domain.model.SimpleDate
import kotlin.math.floor

data class HijriDate(
    val year: Int,
    val month: Int,
    val day: Int
) {
    fun monthNameTr(): String = HIJRI_MONTHS_TR.getOrElse(month - 1) { "" }

    fun formatTr(): String = "$day ${monthNameTr()} $year"

    companion object {
        private val HIJRI_MONTHS_TR = listOf(
            "Muharrem",
            "Safer",
            "Rebiülevvel",
            "Rebiülahir",
            "Cemaziyelevvel",
            "Cemaziyelahir",
            "Recep",
            "Şaban",
            "Ramazan",
            "Şevval",
            "Zilkade",
            "Zilhicce"
        )
    }
}

data class ReligiousDayItem(
    val name: String,
    val gregorianDate: SimpleDate,
    val hijriDate: HijriDate,
    val daysUntil: Int
)

object HijriCalendar {

    /**
     * Converts a Gregorian date (with optional day offset -2..+2) to Hijri using
     * the standard Kuwaiti / Tabular Islamic Calendar algorithm.
     */
    fun gregorianToHijri(date: SimpleDate, dayOffset: Int = 0): HijriDate {
        val shifted = if (dayOffset != 0) date.plusDays(dayOffset) else date
        var day = shifted.day.toDouble()
        var month = shifted.month.toDouble()
        var year = shifted.year.toDouble()

        if (month < 3) {
            year -= 1
            month += 12
        }

        var a = floor(year / 100.0)
        var b = 2.0 - a + floor(a / 4.0)
        if (year < 1583) b = 0.0
        if (year == 1582.0) {
            if (month > 10) b = -10.0
            if (month == 10.0) {
                b = 0.0
                if (day > 4) b = -10.0
            }
        }

        val jd = floor(365.25 * (year + 4716.0)) + floor(30.6001 * (month + 1.0)) + day + b - 1524.0
        val iyear = 10631.0 / 30.0
        val epochAstro = 1948084.0
        val shift1 = 8.01 / 60.0

        var z = jd - epochAstro
        val cyc = floor(z / 10631.0)
        z -= 10631.0 * cyc
        val j = floor((z - shift1) / iyear)
        val iy = 30.0 * cyc + j
        z -= floor(j * iyear + shift1)
        var im = floor((z + 28.5001) / 29.5)
        if (im == 13.0) im = 12.0
        val id = z - floor(29.5001 * im - 29.0)

        return HijriDate(
            year = iy.toInt(),
            month = im.toInt().coerceIn(1, 12),
            day = id.toInt().coerceIn(1, 30)
        )
    }

    fun upcomingReligiousDays(fromDate: SimpleDate, dayOffset: Int = 0): List<ReligiousDayItem> {
        val results = mutableListOf<ReligiousDayItem>()
        var regaipAddedForYear: Int? = null

        for (offset in 0..365) {
            val gDate = fromDate.plusDays(offset)
            val hDate = gregorianToHijri(gDate, dayOffset)

            // Regaip Kandili: first Thursday night of Recep (day 1..7 of month 7, Thursday)
            if (hDate.month == 7 && hDate.day in 1..7 && gDate.isThursday() && regaipAddedForYear != hDate.year) {
                regaipAddedForYear = hDate.year
                results += ReligiousDayItem("Regaip Kandili", gDate, hDate, offset)
            }

            val fixedName = when (hDate.month to hDate.day) {
                1 to 1 -> "Hicri Yılbaşı"
                1 to 10 -> "Aşure Günü"
                3 to 12 -> "Mevlid Kandili"
                7 to 27 -> "Miraç Kandili"
                8 to 15 -> "Berat Kandili"
                9 to 1 -> "Ramazan Başlangıcı"
                9 to 27 -> "Kadir Gecesi"
                10 to 1 -> "Ramazan Bayramı (1. Gün)"
                12 to 9 -> "Arefe Günü"
                12 to 10 -> "Kurban Bayramı (1. Gün)"
                else -> null
            }

            if (fixedName != null) {
                results += ReligiousDayItem(fixedName, gDate, hDate, offset)
            }
        }

        return results
    }
}
