package ink.chyk.pass

import androidx.compose.runtime.Composable

/**
 * The platform's current knowledge of usable network connectivity.
 *
 * Unknown is intentionally treated as unavailable by error UI so a platform
 * never enables retry merely because it has not measured connectivity yet.
 */
enum class NetworkAvailability {
  Unknown,
  Unavailable,
  Available,
}

@Composable
expect fun rememberNetworkAvailability(): NetworkAvailability
