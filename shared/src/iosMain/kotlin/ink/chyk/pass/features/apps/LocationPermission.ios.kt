@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package ink.chyk.pass.features.apps

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.darwin.NSObject

@Composable
actual fun rememberLocationPermissionGranted(): Boolean {
  val locationManager = remember { CLLocationManager() }
  var authorizationStatus by remember {
    mutableStateOf(CLLocationManager.authorizationStatus())
  }
  val delegate = remember {
    object : NSObject(), CLLocationManagerDelegateProtocol {
      override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
        authorizationStatus = manager.authorizationStatus
      }

      @Suppress("DEPRECATION")
      override fun locationManager(
        manager: CLLocationManager,
        didChangeAuthorizationStatus: CLAuthorizationStatus,
      ) {
        authorizationStatus = didChangeAuthorizationStatus
      }
    }
  }

  DisposableEffect(locationManager, delegate) {
    locationManager.delegate = delegate
    onDispose { locationManager.delegate = null }
  }
  LaunchedEffect(locationManager, authorizationStatus) {
    if (authorizationStatus == kCLAuthorizationStatusNotDetermined) {
      locationManager.requestWhenInUseAuthorization()
    }
  }

  return authorizationStatus == kCLAuthorizationStatusAuthorizedWhenInUse ||
    authorizationStatus == kCLAuthorizationStatusAuthorizedAlways
}
