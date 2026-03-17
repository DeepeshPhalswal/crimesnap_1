package com.example.crimesnap

import platform.UIKit.UIDevice
import androidx.compose.runtime.Composable

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
    
    override fun getCurrentLocation(callback: (String) -> Unit) {
        // iOS implementation
    }
    
    override fun isLocationPermissionGranted(): Boolean = true
    override fun requestLocationPermission() {}
    override fun openAppSettings() {}
    override fun isGpsEnabled(): Boolean = true
    override fun requestLocationSettings() {}
    
    override fun isCameraPermissionGranted(): Boolean = true
    override fun requestCameraPermission() {}
    override fun isAudioPermissionGranted(): Boolean = true
    override fun requestAudioPermission() {}
    
    override fun capturePhoto(onResult: (String?) -> Unit) {}
    override fun captureVideo(onResult: (String?) -> Unit) {}
    override fun recordAudio(onResult: (String?) -> Unit) {}
    
    override fun getFileInfo(path: String?): FileInfo? = null
    
    override fun analyzeImage(imagePath: String, onResult: (DetectionResult?) -> Unit) {}
    override suspend fun uploadFile(localPath: String, remotePath: String): String? = null
    override suspend fun saveReport(report: CrimeReport): Boolean = true
}

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS doesn't have a hardware back button
}

actual fun getPlatform(): Platform = IOSPlatform()
