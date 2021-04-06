package kr.co.pflogistics

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * GPS 유틸
 * @author hyobin im
 */

class GPSUtil(context: Context) : LocationListener {

    private var mContext: Context = context;

    // 위도, 경도
    private var lat: Double = 0.0;
    private var lon: Double = 0.0;

    private var lonLat: String? = null;

    private var mLocation: Location? = null;
    private var mLocationManager: LocationManager? = null;

    override fun onLocationChanged(location: Location) {

        lat = location.latitude;
        lon = location.longitude;

        println("위도: ${lat}, 경도:${lon}")

        MainActivity().onMapGetXY(lat, lon)
    }


    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
    }

    /** ============================================================================================================= */

    fun getLocation() {

        try {
            mLocationManager = mContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (ActivityCompat.checkSelfPermission(
                mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return
            }
            mLocation = mLocationManager!!.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if ((ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
                ActivityCompat.requestPermissions(
                    mContext as Activity,
                    IntroActivity().requiresPermission, LOCATION_PERMISSION_CODE
                )
            }
            mLocationManager!!.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100000, 5f, this)

        } catch (e: Exception) {
            Log.e("hbim", e.toString())
        }
    }

    // 위도
    fun getLatitude(): Double {
        if (mLocation != null) lat = mLocation!!.latitude
        return lat;
    }

    // 경도
    fun getLongitude(): Double {
        if (mLocation != null) lon = mLocation!!.longitude
        return lon;
    }

    companion object {
        private val TAG: String? = GPSUtil::class.simpleName;
        private const val LOCATION_PERMISSION_CODE = 2000
    }

}