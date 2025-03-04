package com.stoneCode.rain_alert.ui.interop

import android.content.Context
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback

/**
 * A Composable that wraps a MapView and provides access to the underlying Google Map instance.
 * This is used to bridge the gap between Compose's GoogleMap and the traditional View-based API.
 */
@Composable
fun MapViewCapture(
    modifier: Modifier = Modifier,
    onMapReady: (GoogleMap) -> Unit = {},
    onViewCreated: (MapView) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Create a MapView
    val mapView = remember {
        MapView(context).apply {
            id = View.generateViewId()
        }
    }
    
    // Set up lifecycle-aware map initialization
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(null)
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // Get map asynchronously
    LaunchedEffect(mapView) {
        mapView.getMapAsync { googleMap ->
            onMapReady(googleMap)
        }
    }
    
    // Render the MapView
    AndroidView(
        factory = { mapView.also { onViewCreated(it) } },
        modifier = modifier
    )
}

// Store a static reference for later use if needed
object MapViewCaptureHelper {
    private var mapViewRef: MapView? = null
    private var googleMapRef: GoogleMap? = null
    
    fun setMapViewRef(mapView: MapView) {
        mapViewRef = mapView
        mapView.getMapAsync { googleMap ->
            googleMapRef = googleMap
        }
    }
    
    fun getMapView(): MapView? = mapViewRef
    
    fun getGoogleMap(): GoogleMap? = googleMapRef
}
