package com.salanow.ezannow.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Looper
import androidx.core.content.ContextCompat
import com.salanow.ezannow.data.cities.CityCatalog
import com.salanow.ezannow.domain.model.CityLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.TimeZone
import kotlin.coroutines.resume

sealed interface GpsDetectionResult {
    data class Success(val location: CityLocation) : GpsDetectionResult
    data object PermissionMissing : GpsDetectionResult
    data object ProviderDisabled : GpsDetectionResult
    data object Unavailable : GpsDetectionResult
}

object DeviceLocationHelper {

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    suspend fun detectCurrentLocation(context: Context): GpsDetectionResult {
        if (!hasLocationPermission(context)) {
            return GpsDetectionResult.PermissionMissing
        }

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return GpsDetectionResult.Unavailable

        val gpsEnabled = runCatching { lm.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false)
        val netEnabled = runCatching { lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false)
        if (!gpsEnabled && !netEnabled) {
            // Try passive last known location before reporting disabled
            val passive = runCatching { lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER) }.getOrNull()
            return if (passive != null) {
                GpsDetectionResult.Success(resolveCity(context, passive))
            } else {
                GpsDetectionResult.ProviderDisabled
            }
        }

        val fresh = withTimeoutOrNull(8_000L) {
            requestFreshLocation(context, lm, gpsEnabled, netEnabled)
        }
        val best = fresh ?: bestLastKnownLocation(lm)
        return if (best != null) {
            GpsDetectionResult.Success(resolveCity(context, best))
        } else {
            GpsDetectionResult.Unavailable
        }
    }

    @SuppressLint("MissingPermission")
    private fun bestLastKnownLocation(lm: LocationManager): Location? {
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )
        return providers.mapNotNull { provider ->
            runCatching { lm.getLastKnownLocation(provider) }.getOrNull()
        }.maxByOrNull { it.time }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestFreshLocation(
        context: Context,
        lm: LocationManager,
        gpsEnabled: Boolean,
        netEnabled: Boolean
    ): Location? = suspendCancellableCoroutine { cont ->
        val provider = when {
            netEnabled -> LocationManager.NETWORK_PROVIDER
            gpsEnabled -> LocationManager.GPS_PROVIDER
            else -> {
                cont.resume(null)
                return@suspendCancellableCoroutine
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val signal = CancellationSignal()
            cont.invokeOnCancellation { signal.cancel() }
            runCatching {
                lm.getCurrentLocation(provider, signal, context.mainExecutor) { loc ->
                    if (cont.isActive) cont.resume(loc)
                }
            }.onFailure {
                if (cont.isActive) cont.resume(null)
            }
        } else {
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    runCatching { lm.removeUpdates(this) }
                    if (cont.isActive) cont.resume(location)
                }

                @Deprecated("Deprecated in API 29")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }
            cont.invokeOnCancellation {
                runCatching { lm.removeUpdates(listener) }
            }
            runCatching {
                @Suppress("DEPRECATION")
                lm.requestSingleUpdate(provider, listener, Looper.getMainLooper())
            }.onFailure {
                if (cont.isActive) cont.resume(null)
            }
        }
    }

    private suspend fun resolveCity(context: Context, location: Location): CityLocation {
        val nearest = CityCatalog.findNearestCity(location.latitude, location.longitude)
        val geocoded = withContext(Dispatchers.IO) {
            runCatching {
                if (!Geocoder.isPresent()) return@runCatching null
                val geocoder = Geocoder(context, Locale.forLanguageTag("tr-TR"))
                @Suppress("DEPRECATION")
                val list = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                val addr = list?.firstOrNull() ?: return@runCatching null
                val city = addr.adminArea ?: addr.locality ?: addr.subAdminArea
                val district = addr.subAdminArea ?: addr.locality
                val displayName = when {
                    !district.isNullOrBlank() && !city.isNullOrBlank() && district != city -> "$district, $city"
                    !city.isNullOrBlank() -> city
                    !district.isNullOrBlank() -> district
                    else -> null
                }
                val country = addr.countryName ?: nearest.country
                if (displayName != null) displayName to country else null
            }.getOrNull()
        }

        val tzId = TimeZone.getDefault().id
        return CityLocation(
            id = "gps",
            name = geocoded?.first ?: "${nearest.name} (GPS)",
            country = geocoded?.second ?: nearest.country,
            latitude = location.latitude,
            longitude = location.longitude,
            timeZoneId = tzId,
            isGps = true
        )
    }
}
