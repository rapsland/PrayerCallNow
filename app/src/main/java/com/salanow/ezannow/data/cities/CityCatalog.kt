package com.salanow.ezannow.data.cities

import com.salanow.ezannow.domain.model.CityLocation
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object CityCatalog {

    val DEFAULT_CITY = CityLocation(
        id = "tr_istanbul",
        name = "İstanbul",
        country = "Türkiye",
        latitude = 41.0082,
        longitude = 28.9784,
        timeZoneId = "Europe/Istanbul"
    )

    val ALL_CITIES: List<CityLocation> = listOf(
        // 81 Türkiye İli
        tr("adana", "Adana", 37.0000, 35.3213),
        tr("adiyaman", "Adıyaman", 37.7648, 38.2786),
        tr("afyonkarahisar", "Afyonkarahisar", 38.7507, 30.5567),
        tr("agri", "Ağrı", 39.7191, 43.0503),
        tr("aksaray", "Aksaray", 38.3687, 34.0370),
        tr("amasya", "Amasya", 40.6499, 35.8353),
        tr("ankara", "Ankara", 39.9334, 32.8597),
        tr("antalya", "Antalya", 36.8969, 30.7133),
        tr("ardahan", "Ardahan", 41.1105, 42.7022),
        tr("artvin", "Artvin", 41.1828, 41.8183),
        tr("aydin", "Aydın", 37.8560, 27.8416),
        tr("balikesir", "Balıkesir", 39.6484, 27.8826),
        tr("bartin", "Bartın", 41.6344, 32.3375),
        tr("batman", "Batman", 37.8812, 41.1351),
        tr("bayburt", "Bayburt", 40.2552, 40.2249),
        tr("bilecik", "Bilecik", 40.1451, 29.9799),
        tr("bingol", "Bingöl", 38.8855, 40.4966),
        tr("bitlis", "Bitlis", 38.4006, 42.1095),
        tr("bolu", "Bolu", 40.7350, 31.6061),
        tr("burdur", "Burdur", 37.7204, 30.2908),
        tr("bursa", "Bursa", 40.1885, 29.0610),
        tr("canakkale", "Çanakkale", 40.1553, 26.4142),
        tr("cankiri", "Çankırı", 40.6013, 33.6134),
        tr("corum", "Çorum", 40.5506, 34.9556),
        tr("denizli", "Denizli", 37.7765, 29.0864),
        tr("diyarbakir", "Diyarbakır", 37.9144, 40.2306),
        tr("duzce", "Düzce", 40.8438, 31.1565),
        tr("edirne", "Edirne", 41.6818, 26.5623),
        tr("elazig", "Elazığ", 38.6810, 39.2264),
        tr("erzincan", "Erzincan", 39.7500, 39.5000),
        tr("erzurum", "Erzurum", 39.9000, 41.2700),
        tr("eskisehir", "Eskişehir", 39.7767, 30.5206),
        tr("gaziantep", "Gaziantep", 37.0662, 37.3833),
        tr("giresun", "Giresun", 40.9128, 38.3895),
        tr("gumushane", "Gümüşhane", 40.4603, 39.4814),
        tr("hakkari", "Hakkari", 37.5833, 43.7333),
        tr("hatay", "Hatay", 36.4018, 36.3498),
        tr("igdir", "Iğdır", 39.9167, 44.0333),
        tr("isparta", "Isparta", 37.7648, 30.5566),
        DEFAULT_CITY,
        tr("izmir", "İzmir", 38.4237, 27.1428),
        tr("kahramanmaras", "Kahramanmaraş", 37.5858, 36.9371),
        tr("karabuk", "Karabük", 41.2061, 32.6204),
        tr("karaman", "Karaman", 37.1759, 33.2287),
        tr("kars", "Kars", 40.6013, 43.0975),
        tr("kastamonu", "Kastamonu", 41.3887, 33.7827),
        tr("kayseri", "Kayseri", 38.7312, 35.4787),
        tr("kilis", "Kilis", 36.7184, 37.1212),
        tr("kirikkale", "Kırıkkale", 39.8468, 33.5153),
        tr("kirklareli", "Kırklareli", 41.7333, 27.2167),
        tr("kirsehir", "Kırşehir", 39.1425, 34.1709),
        tr("kocaeli", "Kocaeli", 40.8533, 29.8815),
        tr("konya", "Konya", 37.8667, 32.4833),
        tr("kutahya", "Kütahya", 39.4167, 29.9833),
        tr("malatya", "Malatya", 38.3552, 38.3095),
        tr("manisa", "Manisa", 38.6191, 27.4289),
        tr("mardin", "Mardin", 37.3212, 40.7245),
        tr("mersin", "Mersin", 36.8121, 34.6415),
        tr("mugla", "Muğla", 37.2153, 28.3636),
        tr("mus", "Muş", 38.7432, 41.5064),
        tr("nevsehir", "Nevşehir", 38.6244, 34.7239),
        tr("nigde", "Niğde", 37.9667, 34.6833),
        tr("ordu", "Ordu", 40.9839, 37.8764),
        tr("osmaniye", "Osmaniye", 37.0742, 36.2478),
        tr("rize", "Rize", 41.0201, 40.5234),
        tr("sakarya", "Sakarya", 40.7569, 30.3783),
        tr("samsun", "Samsun", 41.2867, 36.3300),
        tr("sanliurfa", "Şanlıurfa", 37.1591, 38.7969),
        tr("siirt", "Siirt", 37.9333, 41.9500),
        tr("sinop", "Sinop", 42.0231, 35.1531),
        tr("sirnak", "Şırnak", 37.4187, 42.4918),
        tr("sivas", "Sivas", 39.7477, 37.0179),
        tr("tekirdag", "Tekirdağ", 40.9833, 27.5167),
        tr("tokat", "Tokat", 40.3167, 36.5500),
        tr("trabzon", "Trabzon", 41.0015, 39.7178),
        tr("tunceli", "Tunceli", 39.1079, 39.5401),
        tr("usak", "Uşak", 38.6823, 29.4082),
        tr("van", "Van", 38.4891, 43.4089),
        tr("yalova", "Yalova", 40.6500, 29.2667),
        tr("yozgat", "Yozgat", 39.8181, 34.8147),
        tr("zonguldak", "Zonguldak", 41.4564, 31.7987),
        // Başlıca Avrupa, Azerbaycan ve Kutsal Topraklar Şehirleri
        CityLocation("de_berlin", "Berlin", "Almanya", 52.5200, 13.4050, "Europe/Berlin"),
        CityLocation("de_koln", "Köln", "Almanya", 50.9375, 6.9603, "Europe/Berlin"),
        CityLocation("de_munih", "Münih", "Almanya", 48.1351, 11.5820, "Europe/Berlin"),
        CityLocation("de_frankfurt", "Frankfurt", "Almanya", 50.1109, 8.6821, "Europe/Berlin"),
        CityLocation("de_hamburg", "Hamburg", "Almanya", 53.5511, 9.9937, "Europe/Berlin"),
        CityLocation("de_stuttgart", "Stuttgart", "Almanya", 48.7758, 9.1829, "Europe/Berlin"),
        CityLocation("de_dusseldorf", "Düsseldorf", "Almanya", 51.2277, 6.7735, "Europe/Berlin"),
        CityLocation("at_viyana", "Viyana", "Avusturya", 48.2082, 16.3738, "Europe/Vienna"),
        CityLocation("nl_amsterdam", "Amsterdam", "Hollanda", 52.3676, 4.9041, "Europe/Amsterdam"),
        CityLocation("be_bruksel", "Brüksel", "Belçika", 50.8503, 4.3517, "Europe/Brussels"),
        CityLocation("fr_paris", "Paris", "Fransa", 48.8566, 2.3522, "Europe/Paris"),
        CityLocation("gb_londra", "Londra", "Birleşik Krallık", 51.5072, -0.1276, "Europe/London"),
        CityLocation("ch_zurih", "Zürih", "İsviçre", 47.3769, 8.5417, "Europe/Zurich"),
        CityLocation("az_baku", "Bakü", "Azerbaycan", 40.4093, 49.8671, "Asia/Baku"),
        CityLocation("sa_mekke", "Mekke", "Suudi Arabistan", 21.4225, 39.8262, "Asia/Riyadh"),
        CityLocation("sa_medine", "Medine", "Suudi Arabistan", 24.4672, 39.6111, "Asia/Riyadh")
    )

    private fun tr(id: String, name: String, lat: Double, lon: Double) = CityLocation(
        id = "tr_$id",
        name = name,
        country = "Türkiye",
        latitude = lat,
        longitude = lon,
        timeZoneId = "Europe/Istanbul"
    )

    fun normalizeTurkish(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            sb.append(
                when (ch) {
                    'İ', 'I', 'ı' -> 'i'
                    'Ş', 'ş' -> 's'
                    'Ğ', 'ğ' -> 'g'
                    'Ü', 'ü' -> 'u'
                    'Ö', 'ö' -> 'o'
                    'Ç', 'ç' -> 'c'
                    else -> ch.lowercaseChar()
                }
            )
        }
        return sb.toString().trim()
    }

    fun searchCities(query: String): List<CityLocation> {
        val q = normalizeTurkish(query)
        if (q.isEmpty()) return ALL_CITIES
        val startsWith = mutableListOf<CityLocation>()
        val contains = mutableListOf<CityLocation>()
        for (city in ALL_CITIES) {
            val normName = normalizeTurkish(city.name)
            val normCountry = normalizeTurkish(city.country)
            when {
                normName.startsWith(q) -> startsWith += city
                normName.contains(q) || normCountry.contains(q) -> contains += city
            }
        }
        return startsWith + contains
    }

    fun findNearestCity(latitude: Double, longitude: Double): CityLocation {
        return ALL_CITIES.minByOrNull { city ->
            haversineKm(latitude, longitude, city.latitude, city.longitude)
        } ?: DEFAULT_CITY
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        return 2 * r * atan2(sqrt(a), sqrt(1 - a))
    }
}
