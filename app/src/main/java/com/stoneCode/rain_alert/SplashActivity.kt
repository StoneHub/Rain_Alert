package com.stoneCode.rain_alert

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.StoneCode.rain_alert.R
import com.stoneCode.rain_alert.firebase.FirebaseLogger
import com.stoneCode.rain_alert.ui.theme.Rain_AlertTheme
import kotlinx.coroutines.delay

/**
 * Splash Activity that displays on app startup
 * Handles initial loading operations before transitioning to the main activity
 */
class SplashActivity : ComponentActivity() {
    private val TAG = "SplashActivity"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Rain_AlertTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SplashScreen(
                        onInitializationComplete = {
                            navigateToMainActivity()
                        }
                    )
                }
            }
        }
        
        // Log app open event
        FirebaseLogger.getInstance().logAppOpen()
    }
    
    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // Close splash activity so it's not in the back stack
    }
}

@Composable
fun SplashScreen(
    onInitializationComplete: () -> Unit
) {
    var loadingText by remember { mutableStateOf("Initializing...") }
    
    // In a real app, you might perform actual initialization tasks here
    LaunchedEffect(true) {
        try {
            // Simulate app initialization with changing status messages
            delay(500)
            loadingText = "Checking for updates..."
            delay(500)
            loadingText = "Loading weather data..."
            delay(500)
            loadingText = "Ready!"
            delay(300)
            
            // Navigate to main activity
            onInitializationComplete()
        } catch (e: Exception) {
            Log.e("SplashScreen", "Error during initialization", e)
            loadingText = "Error initializing app"
            // Even on error, proceed to main activity after a delay
            delay(1000)
            onInitializationComplete()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App logo
        Image(
            painter = painterResource(id = R.mipmap.ic_launcher),
            contentDescription = "Rain Alert Logo",
            modifier = Modifier.size(120.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // App name
        Text(
            text = "Rain Alert",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Tagline
        Text(
            text = "Never get caught in the rain again",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Loading indicator
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.secondary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Loading text
        Text(
            text = loadingText,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}