package com.salanow.ezannow

import com.salanow.ezannow.domain.calculator.HijriCalendar
import com.salanow.ezannow.domain.model.SimpleDate
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HijriCalendarTest {

    @Test
    fun gregorianToHijriReturnsValidRangesAndFormatting() {
        val date = SimpleDate(2026, 11, 20)
        val hijri = HijriCalendar.gregorianToHijri(date)

        assertTrue(hijri.year in 1445..1450)
        assertTrue(hijri.month in 1..12)
        assertTrue(hijri.day in 1..30)
        assertTrue(hijri.monthNameTr().isNotBlank())
        assertTrue(hijri.formatTr().contains(hijri.year.toString()))
    }

    @Test
    fun dayOffsetShiftsHijriDate() {
        val date = SimpleDate(2026, 11, 20)
        val base = HijriCalendar.gregorianToHijri(date, 0)
        val shifted = HijriCalendar.gregorianToHijri(date, 1)

        assertNotEquals(base, shifted)
    }

    @Test
    fun upcomingReligiousDaysAreNonEmptyAndSorted() {
        val from = SimpleDate(2026, 11, 20)
        val days = HijriCalendar.upcomingReligiousDays(from)

        assertTrue(days.isNotEmpty())
        assertTrue(days.any { it.name.contains("Ramazan") })
        assertTrue(days.any { it.name.contains("Kurban") })
        for (i in 0 until days.lastIndex) {
            assertTrue(days[i].daysUntil <= days[i + 1].daysUntil)
        }
    }
}
