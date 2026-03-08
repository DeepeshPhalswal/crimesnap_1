package com.example.crimesnap

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
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}