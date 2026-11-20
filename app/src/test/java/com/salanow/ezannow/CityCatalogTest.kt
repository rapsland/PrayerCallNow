package com.salanow.ezannow

import com.salanow.ezannow.data.cities.CityCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CityCatalogTest {

    @Test
    fun all81TurkishProvincesArePresent() {
        val turkishProvinces = CityCatalog.ALL_CITIES.filter { it.country == "Türkiye" }
        assertEquals(81, turkishProvinces.size)
        assertEquals(81, turkishProvinces.map { it.id }.toSet().size)
    }

    @Test
    fun allCitiesHaveValidCoordinates() {
        CityCatalog.ALL_CITIES.forEach { city ->
            assertTrue("${city.name} lat", city.latitude in -90.0..90.0)
            assertTrue("${city.name} lon", city.longitude in -180.0..180.0)
            assertTrue("${city.name} tz", city.timeZoneId.isNotBlank())
        }
    }

    @Test
    fun turkishDiacriticInsensitiveSearchMatchesCorrectly() {
        assertTrue(CityCatalog.searchCities("istanbul").any { it.name == "İstanbul" })
        assertTrue(CityCatalog.searchCities("sanliurfa").any { it.name == "Şanlıurfa" })
        assertTrue(CityCatalog.searchCities("izmir").any { it.name == "İzmir" })
        assertTrue(CityCatalog.searchCities("DİYARBAKIR").any { it.name == "Diyarbakır" })
    }
}
