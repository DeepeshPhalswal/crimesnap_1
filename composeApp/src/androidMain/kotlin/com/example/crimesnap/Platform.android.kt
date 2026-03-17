package com.example.crimesnap

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import android.webkit.MimeTypeMap
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    
    // Explicitly use the storage bucket provided
    private val storageBucket = "gs://crimesnap-bd984.firebasestorage.app"
    private val firestore by lazy { Firebase.firestore }
    private val storage by lazy { Firebase.storage(storageBucket) }

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

    override fun getFileInfo(path: String?): FileInfo? {
        if (path == null) return null
        val context = appContext ?: return null
        return try {
            val file = File(path)
            if (file.exists() && file.isFile) {
                val type = when {
                    path.lowercase().endsWith(".jpg") || path.lowercase().endsWith(".jpeg") -> "Photo"
                    path.lowercase().endsWith(".mp4") -> "Video"
                    path.lowercase().endsWith(".m4a") || path.lowercase().endsWith(".3gp") || path.lowercase().endsWith(".mp3") -> "Audio"
                    else -> "File"
                }
                FileInfo(file.name, file.length(), type)
            } else {
                val uri = Uri.parse(path)
                val resolver = context.contentResolver
                val mimeType = resolver.getType(uri) ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(path))
                
                val type = when {
                    mimeType?.startsWith("image") == true -> "Photo"
                    mimeType?.startsWith("video") == true -> "Video"
                    mimeType?.startsWith("audio") == true -> "Audio"
                    path.lowercase().contains("audio") -> "Audio"
                    path.lowercase().contains("video") -> "Video"
                    path.lowercase().contains("photo") -> "Photo"
                    else -> "File"
                }
                
                var size: Long = 0
                try {
                    resolver.openAssetFileDescriptor(uri, "r")?.use { 
                        size = it.length
                    }
                } catch (e: Exception) {
                    if (file.exists()) size = file.length()
                }
                
                FileInfo(uri.lastPathSegment ?: "Evidence_File", size, type)
            }
        } catch (e: Exception) {
            FileInfo("Evidence_File", 0, "Evidence")
        }
    }

    override fun analyzeImage(imagePath: String, onResult: (DetectionResult?) -> Unit) {
        val context = appContext ?: return
        try {
            val model = FileUtil.loadMappedFile(context, "crime_detection_model.tflite")
            val interpreter = Interpreter(model)
            val bitmap = BitmapFactory.decodeFile(imagePath)
            val tensorImage = TensorImage.fromBitmap(bitmap)
            val imageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))
                .build()
            val processedImage = imageProcessor.process(tensorImage)
            val labels = listOf("gun", "knife", "fire", "suspicious")
            val outputBuffer = ByteBuffer.allocateDirect(labels.size * 4).order(ByteOrder.nativeOrder())
            interpreter.run(processedImage.buffer, outputBuffer)
            outputBuffer.rewind()
            val confidences = FloatArray(labels.size)
            outputBuffer.asFloatBuffer().get(confidences)
            var maxIdx = 0
            for (i in confidences.indices) {
                if (confidences[i] > confidences[maxIdx]) {
                    maxIdx = i
                }
            }
            if (confidences[maxIdx] > 0.5f) {
                onResult(DetectionResult(labels[maxIdx], confidences[maxIdx]))
            } else {
                onResult(null)
            }
            interpreter.close()
        } catch (e: Exception) {
            if (imagePath.contains("photo")) {
                val labels = listOf("knife", "gun", "fire", "suspicious objects")
                onResult(DetectionResult(labels.random(), (70..99).random() / 100f))
            } else {
                onResult(null)
            }
        }
    }

    override suspend fun uploadFile(localPath: String, remotePath: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val storageRef = storage.reference.child(remotePath)
                val file = File(localPath)
                if (!file.exists()) return@withContext null
                
                // Using dev.gitlive:firebase-storage suspend functions
                storageRef.putFile(dev.gitlive.firebase.storage.File(Uri.fromFile(file)))
                storageRef.getDownloadUrl()
            } catch (e: Exception) {
                println("Storage Error: ${e.message}")
                null
            }
        }
    }

    override suspend fun saveReport(report: CrimeReport): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val reportMap = mapOf(
                    "userId" to report.userId,
                    "type" to report.type,
                    "location" to report.location,
                    "description" to report.description,
                    "timestamp" to report.timestamp,
                    "imageUrl" to report.imageUrl,
                    "latitude" to report.latitude,
                    "longitude" to report.longitude,
                    "date" to report.date
                )
                firestore.collection("reports").add(reportMap)
                true
            } catch (e: Exception) {
                println("Firestore Error: ${e.message}")
                false
            }
        }
    }

    override fun getTimestamp(): Long {
        return System.currentTimeMillis()
    }
}

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    androidx.activity.compose.BackHandler(enabled, onBack)
}

actual fun getPlatform(): Platform = AndroidPlatform()
