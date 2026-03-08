package com.example.crimesnap

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import java.io.File
import java.util.Locale

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    
    companion object {
        @SuppressLint("StaticFieldLeak")
        var appContext: Context? = null
        var currentActivity: Activity? = null
        
        var onMediaCaptured: ((String?) -> Unit)? = null
        var tempMediaUri: Uri? = null
    }

    override fun isLocationPermissionGranted(): Boolean {
        val context = appContext ?: return false
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun requestLocationPermission() {
        val activity = currentActivity ?: return
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            1000
        )
    }

    override fun isCameraPermissionGranted(): Boolean {
        val context = appContext ?: return false
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun requestCameraPermission() {
        val activity = currentActivity ?: return
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.CAMERA),
            1002
        )
    }

    override fun isAudioPermissionGranted(): Boolean {
        val context = appContext ?: return false
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun requestAudioPermission() {
        val activity = currentActivity ?: return
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            1003
        )
    }

    override fun openAppSettings() {
        val activity = currentActivity ?: return
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", activity.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        activity.startActivity(intent)
    }

    override fun isGpsEnabled(): Boolean {
        val context = appContext ?: return false
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    override fun requestLocationSettings() {
        val activity = currentActivity ?: return
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).build()
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client: SettingsClient = LocationServices.getSettingsClient(activity)
        val task = client.checkLocationSettings(builder.build())

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    exception.startResolutionForResult(activity, 1001)
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun getCurrentLocation(callback: (String) -> Unit) {
        val context = appContext ?: run {
            callback("Context not initialized")
            return
        }
        
        if (!isLocationPermissionGranted()) {
            callback("PERMISSION_REQUIRED")
            return
        }

        if (!isGpsEnabled()) {
            callback("GPS_DISABLED")
            return
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            CancellationTokenSource().token
        ).addOnSuccessListener { location ->
            if (location != null) {
                val geocoder = Geocoder(context, Locale.getDefault())
                try {
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val address = addresses[0]
                        val fullAddress = buildString {
                            append("Lat: ${location.latitude}, Lon: ${location.longitude}\n")
                            append("Address: ${address.getAddressLine(0)}\n")
                            address.postalCode?.let { append("Pincode: $it") }
                        }
                        callback(fullAddress)
                    } else {
                        callback("Lat: ${location.latitude}, Lon: ${location.longitude}\n(Address not found)")
                    }
                } catch (e: Exception) {
                    callback("Lat: ${location.latitude}, Lon: ${location.longitude}\n(Error getting address)")
                }
            } else {
                callback("GPS_DISABLED")
            }
        }.addOnFailureListener {
            callback("Error detecting location: ${it.message}")
        }
    }

    override fun capturePhoto(onResult: (String?) -> Unit) {
        val activity = currentActivity ?: return
        onMediaCaptured = onResult
        
        if (!isCameraPermissionGranted()) {
            onResult("PERMISSION_REQUIRED")
            return
        }

        try {
            val photoFile = File(activity.getExternalFilesDir("evidence"), "photo_${System.currentTimeMillis()}.jpg")
            photoFile.parentFile?.mkdirs()
            tempMediaUri = FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", photoFile)
            
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, tempMediaUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            activity.startActivityForResult(intent, 2001)
        } catch (e: Exception) {
            onResult(null)
        }
    }

    override fun captureVideo(onResult: (String?) -> Unit) {
        val activity = currentActivity ?: return
        onMediaCaptured = onResult
        
        if (!isCameraPermissionGranted()) {
            onResult("PERMISSION_REQUIRED")
            return
        }

        try {
            val videoFile = File(activity.getExternalFilesDir("evidence"), "video_${System.currentTimeMillis()}.mp4")
            videoFile.parentFile?.mkdirs()
            tempMediaUri = FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", videoFile)

            val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, tempMediaUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            activity.startActivityForResult(intent, 2002)
        } catch (e: Exception) {
            onResult(null)
        }
    }

    override fun recordAudio(onResult: (String?) -> Unit) {
        val activity = currentActivity ?: return
        onMediaCaptured = onResult
        
        if (!isAudioPermissionGranted()) {
            onResult("PERMISSION_REQUIRED")
            return
        }

        try {
            val intent = Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION)
            if (intent.resolveActivity(activity.packageManager) != null) {
                activity.startActivityForResult(intent, 2003)
            } else {
                val altIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "audio/*"
                }
                activity.startActivityForResult(altIntent, 2003)
            }
        } catch (e: Exception) {
            onResult(null)
        }
    }
}

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    androidx.activity.compose.BackHandler(enabled, onBack)
}

actual fun getPlatform(): Platform = AndroidPlatform()
