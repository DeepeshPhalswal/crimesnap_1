package com.example.crimesnap

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
}

expect fun getPlatform(): Platform