package com.ohadsam.tehilim.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.Executors
import kotlin.coroutines.resume

/** One-shot coarse location using the platform LocationManager (no Google Play Services). */
object DeviceLocation {
    private const val TIMEOUT_MS = 15_000L

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** Returns a fresh fix, else the system's last known one, else null (no permission / providers off / timeout). */
    @Suppress("MissingPermission")
    suspend fun fix(context: Context): GeoFix? {
        if (!hasPermission(context)) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            .filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }
        for (p in providers) {
            val loc = withTimeoutOrNull(TIMEOUT_MS) { current(lm, p) }
            if (loc != null) return loc.toFix()
        }
        return providers.mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }.maxByOrNull { it.time }?.toFix()
    }

    @Suppress("MissingPermission")
    private suspend fun current(lm: LocationManager, provider: String): Location? = suspendCancellableCoroutine { cont ->
        val signal = CancellationSignal()
        cont.invokeOnCancellation { signal.cancel() }
        val executor = Executors.newSingleThreadExecutor()
        try {
            LocationManagerCompat.getCurrentLocation(lm, provider, signal, executor) { loc ->
                executor.shutdown()
                if (cont.isActive) cont.resume(loc)
            }
        } catch (e: SecurityException) {
            executor.shutdown()
            if (cont.isActive) cont.resume(null)
        }
    }

    private fun Location.toFix() = GeoFix(latitude, longitude, if (hasAltitude()) altitude else 0.0, time)
}
