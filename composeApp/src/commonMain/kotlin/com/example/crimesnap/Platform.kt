package com.example.crimesnap

import androidx.compose.runtime.Composable
import kotlin.math.roundToLong

data class FileInfo(
    val name: String,
    val sizeInBytes: Long,
    val type: String
) {
    val sizeFormatted: String
        get() {
            val kb = sizeInBytes / 1024.0
            val mb = kb / 1024.0
            return when {
                mb >= 1.0 -> "${(mb * 100).roundToLong() / 100.0} MB"
                kb >= 1.0 -> "${(kb * 100).roundToLong() / 100.0} KB"
                else -> "$sizeInBytes Bytes"
            }
        }
}

interface Platform {
    val name: String
    fun getCurrentLocation(callback: (String) -> Unit)
    fun isLocationPermissionGranted(): Boolean
    fun requestLocationPermission()
    fun openAppSettings()
    fun isGpsEnabled(): Boolean
    fun requestLocationSettings()
    
    // Media Capture
    fun isCameraPermissionGranted(): Boolean
    fun requestCameraPermission()
    fun isAudioPermissionGranted(): Boolean
    fun requestAudioPermission()
    
    fun capturePhoto(onResult: (String?) -> Unit)
    fun captureVideo(onResult: (String?) -> Unit)
    fun recordAudio(onResult: (String?) -> Unit)
    
    fun getFileInfo(path: String?): FileInfo?

    // AI & Firebase
    fun analyzeImage(imagePath: String, onResult: (DetectionResult?) -> Unit)
    suspend fun uploadFile(localPath: String, remotePath: String): String?
    suspend fun saveReport(report: CrimeReport): Boolean
    
    // Utils
    fun getTimestamp(): Long
}

@Composable
expect fun BackHandler(enabled: Boolean = true, onBack: () -> Unit)

expect fun getPlatform(): Platform
