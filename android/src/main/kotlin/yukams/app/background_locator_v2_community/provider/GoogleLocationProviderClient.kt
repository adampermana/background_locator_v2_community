package yukams.app.background_locator_v2_community.provider

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.*

class GoogleLocationProviderClient(context: Context, override var listener: LocationUpdateListener?) : BLLocationProvider {
    private val client: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private val locationCallback = LocationListener(listener)

    override fun removeLocationUpdates() {
        client.removeLocationUpdates(locationCallback)
    }

    @SuppressLint("MissingPermission")
    override fun requestLocationUpdates(request: LocationRequestOptions) {
        client.requestLocationUpdates(getLocationRequest(request), locationCallback, null)
    }

    private fun getLocationRequest(request: LocationRequestOptions): LocationRequest {
        return LocationRequest.Builder(request.interval)
            .setMinUpdateIntervalMillis(request.interval)
            .setMaxUpdateDelayMillis(request.interval)
            .setPriority(request.accuracy)
            .setMinUpdateDistanceMeters(request.distanceFilter)
            .build()
    }
}

private class LocationListener(val listener: LocationUpdateListener?) : LocationCallback() {
    override fun onLocationResult(location: LocationResult) {
        listener?.onLocationUpdated(LocationParserUtil.getLocationMapFromLocation(location))
    }
}