package com.example.crimesnap

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        // Initialize the app context and current activity for location services
        AndroidPlatform.appContext = applicationContext
        AndroidPlatform.currentActivity = this

        setContent {
            App()
        }
    }
    
    override fun onResume() {
        super.onResume()
        AndroidPlatform.currentActivity = this
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                2001 -> { // Photo
                    AndroidPlatform.onMediaCaptured?.invoke(AndroidPlatform.tempMediaUri?.toString())
                }
                2002 -> { // Video
                    // Video intent might return a Uri in data.data or we use the one we provided
                    val uri = data?.data ?: AndroidPlatform.tempMediaUri
                    AndroidPlatform.onMediaCaptured?.invoke(uri?.toString())
                }
                2003 -> { // Audio
                    // Audio intent usually returns the Uri in data.data
                    AndroidPlatform.onMediaCaptured?.invoke(data?.data?.toString())
                }
            }
        } else {
            // User cancelled or capture failed
            AndroidPlatform.onMediaCaptured?.invoke(null)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}