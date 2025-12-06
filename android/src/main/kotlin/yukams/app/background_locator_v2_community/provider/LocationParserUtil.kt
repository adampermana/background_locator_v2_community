package yukams.app.background_locator_2.provider

import android.location.Location
import android.os.Build
import com.google.android.gms.location.LocationResult
import yukams.app.background_locator_2.Keys
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
                put(Keys.ARG_IS_MOCKED, isMocked)
                put(Keys.ARG_LATITUDE, location.latitude)
                put(Keys.ARG_LONGITUDE, location.longitude)
                put(Keys.ARG_ACCURACY, location.accuracy)
                put(Keys.ARG_ALTITUDE, location.altitude)
                put(Keys.ARG_SPEED, location.speed)
                put(Keys.ARG_SPEED_ACCURACY, speedAccuracy)
                put(Keys.ARG_HEADING, location.bearing)
                put(Keys.ARG_TIME, location.time.toDouble())
                put(Keys.ARG_PROVIDER, location.provider)
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
                put(Keys.ARG_IS_MOCKED, isMocked)
                put(Keys.ARG_LATITUDE, firstLocation.latitude)
                put(Keys.ARG_LONGITUDE, firstLocation.longitude)
                put(Keys.ARG_ACCURACY, firstLocation.accuracy)
                put(Keys.ARG_ALTITUDE, firstLocation.altitude)
                put(Keys.ARG_SPEED, firstLocation.speed)
                put(Keys.ARG_SPEED_ACCURACY, speedAccuracy)
                put(Keys.ARG_HEADING, firstLocation.bearing)
                put(Keys.ARG_TIME, firstLocation.time.toDouble())
            }
        }
    }
}