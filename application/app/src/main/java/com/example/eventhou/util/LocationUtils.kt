@file:JvmName("LocationUtils")

package com.example.eventhou.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.requestPermissions
import com.example.eventhou.data.model.AppUser
import com.example.eventhou.data.events.EventHandler
import com.google.android.gms.location.*
import com.google.firebase.firestore.FirebaseFirestore
import java.io.IOException
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

val GPS_ENABLE_REQUEST = 100
val ACCESS_FINE_LOCATION_REQUEST = 200

/**
 * The fused location client in order to save resources and only request location, when needed.
 */
private lateinit var fusedLocationClient: FusedLocationProviderClient

/**
 * Convert the current location to a city name.
 */
fun processLocation(location: Location, activityContext: Context): String? {
    val ltd = location.latitude
    val lng = location.longitude
    var cityName: String? = null
    try {
        val addresses = Geocoder(activityContext, Locale.getDefault()).getFromLocation(ltd, lng, 1)
        cityName = addresses[0].locality
        cityName = Geocoder(activityContext, Locale.ENGLISH).getFromLocationName(
            cityName,
            1
        )[0].locality
    } catch (ex: IOException) {
        // See: https://stackoverflow.com/questions/47331480/geocoder-getfromlocation-grpc-failed-on-android-real-device
        Log.e("LocationUtil", ex.toString())
    }
    return cityName
}

/**
 * Get the city name.
 */
suspend fun getCity(activityContext: Context, activity: Activity): String? {
    fusedLocationClient = LocationServices.getFusedLocationProviderClient(activityContext)
    fusedLocationClient.flushLocations()
    if (!checkPermission(activity)) {
        return null
    }
    var location = getLastLocation(
        fusedLocationClient,
        activity
    )
    if (location == null) {
        /**
         * Update Location in case there's no last location saved
         * locationManager?.requestLocationUpdates performs the update
         */
        location = requestLocation(activity)
    }

    return location?.let {
        processLocation(it, activityContext)
    }
}

/**
 * Get the last available location from fused location provider client.
 */
suspend fun getLastLocation(
    fusedLocationProviderClient: FusedLocationProviderClient,
    activity: Activity
): Location? {
    return suspendCoroutine { continuation ->
        if (checkPermission(activity)) {
            fusedLocationProviderClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    continuation.resume(location)
                }
                .addOnFailureListener {
                    continuation.resumeWithException(
                        Exception("locationFailure: cannot fetch location from fusedLocationProviderClient")
                    )
                }
        } else
            continuation.resume(null)
    }
}

/**
 * Request location considering using PRIORITY_LOW_POWER option.
 */
private suspend fun requestLocation(activity: Activity): Location? {
    val locationManager = activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    // Consider other providers than GPS
    //val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    //val passiveProvider = locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)
    //val bestProvider = locationManager.getBestProvider(Criteria(), false)

    if (!isGPSEnabled) {
        showGPSDiabledDialog(activity)
    }

    return suspendCoroutine { continuation ->
        if (checkPermission(activity)) {
            // Fetch new location, if no location is available
            val locationCallback = object : LocationCallback() {
                override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                    if (locationAvailability.isLocationAvailable) {
                        // No implementation needed.
                    }
                }

                override fun onLocationResult(locationResult: LocationResult) {
                    super.onLocationResult(locationResult)
                    continuation.resume(locationResult.lastLocation)
                    fusedLocationClient.removeLocationUpdates(this)
                }
            }
            val locationRequest =
                createLocationRequest()
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } else
            continuation.resume(null)
    }
}

/**
 * Create location request using PRIORITY_LOW_POWER option.
 */
private fun createLocationRequest(): LocationRequest? {
    return LocationRequest.create()?.apply {
        //interval = 10000
        //fastestInterval = 5000
        priority = LocationRequest.PRIORITY_LOW_POWER
        numUpdates = 1
    }
}

/**
 * Check if app has permission to request fine / coarse location.
 */
fun checkPermission(activity: Activity): Boolean {
    return if (ActivityCompat.checkSelfPermission(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
        && ActivityCompat.checkSelfPermission(
            activity,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        requestPermissions(
            activity,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            ACCESS_FINE_LOCATION_REQUEST
        )
        false
    } else {
        true
    }
}

/**
 * Show a dialog, if GPS is needed, but disabled in user settings.
 */
fun showGPSDiabledDialog(activity: Activity) {
    activity.runOnUiThread {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle("GPS Disabled")
        builder.setMessage("Gps is disabled, in order to use the application properly you need to enable GPS of your device")
        builder.setPositiveButton("Enable GPS") { _, _ ->
            activity.startActivityForResult(
                Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS),
                GPS_ENABLE_REQUEST
            )
        }.setNegativeButton("No thanks!") { _, _ -> }
        val mGPSDialog = builder.create()
        mGPSDialog.show()
    }
}

/**
 * Log the user location in firestore for DEBUG purposes.
 */
fun logUserLocation(userCity: String, db: FirebaseFirestore, date: String, user: AppUser) {
    val dbUser = db.collection(EventHandler.USERS_KEY).document(user.userId)
        .collection("metadata").document("locations")
        .collection(date).document()
    val location = hashMapOf(
        "date" to Date(),
        "city" to userCity
    )
    dbUser.set(location)
}
