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
        
        // Initialize the app context and current activity
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
        
        if (requestCode == AndroidAuthManager.RC_SIGN_IN) {
            getAndroidAuthManager().handleSignInResult(data)
            return
        }

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                2001 -> { // Photo
                    AndroidPlatform.onMediaCaptured?.invoke(AndroidPlatform.tempMediaUri?.toString())
                }
                2002 -> { // Video
                    val uri = data?.data ?: AndroidPlatform.tempMediaUri
                    AndroidPlatform.onMediaCaptured?.invoke(uri?.toString())
                }
                2003 -> { // Audio
                    AndroidPlatform.onMediaCaptured?.invoke(data?.data?.toString())
                }
            }
        } else {
            AndroidPlatform.onMediaCaptured?.invoke(null)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}