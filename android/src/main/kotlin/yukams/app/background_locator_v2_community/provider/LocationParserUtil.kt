package yukams.app.background_locator_v2_community.provider

import android.location.Location
import android.os.Build
import com.google.android.gms.location.LocationResult
import yukams.app.background_locator_v2_community.Keys
import java.util.HashMap

class LocationParserUtil {
    companion object {
        fun getLocationMapFromLocation(location: Location): HashMap<Any, Any> {
            var speedAccuracy = 0f
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                speedAccuracy = location.speedAccuracyMetersPerSecond
            }
            var isMocked = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                isMocked = location.isFromMockProvider
            }

            return HashMap<Any, Any>().apply {
                put(Keys.Companion.ARG_IS_MOCKED, isMocked)
                put(Keys.Companion.ARG_LATITUDE, location.latitude)
                put(Keys.Companion.ARG_LONGITUDE, location.longitude)
                put(Keys.Companion.ARG_ACCURACY, location.accuracy)
                put(Keys.Companion.ARG_ALTITUDE, location.altitude)
                put(Keys.Companion.ARG_SPEED, location.speed)
                put(Keys.Companion.ARG_SPEED_ACCURACY, speedAccuracy)
                put(Keys.Companion.ARG_HEADING, location.bearing)
                put(Keys.Companion.ARG_TIME, location.time.toDouble())
                put(Keys.Companion.ARG_PROVIDER, location.provider ?: "unknown")
            }
        }

        fun getLocationMapFromLocation(location: LocationResult?): HashMap<Any, Any>? {
            val firstLocation = location?.lastLocation ?: return null

            var speedAccuracy = 0f
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                speedAccuracy = firstLocation.speedAccuracyMetersPerSecond
            }
            var isMocked = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                isMocked = firstLocation.isFromMockProvider
            }

            return HashMap<Any, Any>().apply {
                put(Keys.Companion.ARG_IS_MOCKED, isMocked)
                put(Keys.Companion.ARG_LATITUDE, firstLocation.latitude)
                put(Keys.Companion.ARG_LONGITUDE, firstLocation.longitude)
                put(Keys.Companion.ARG_ACCURACY, firstLocation.accuracy)
                put(Keys.Companion.ARG_ALTITUDE, firstLocation.altitude)
                put(Keys.Companion.ARG_SPEED, firstLocation.speed)
                put(Keys.Companion.ARG_SPEED_ACCURACY, speedAccuracy)
                put(Keys.Companion.ARG_HEADING, firstLocation.bearing)
                put(Keys.Companion.ARG_TIME, firstLocation.time.toDouble())
            }
        }
    }
}