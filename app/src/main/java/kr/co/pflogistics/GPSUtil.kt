package kr.co.pflogistics

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * GPS 유틸
 * @author hyobin im
 */

class GPSUtil(context: Context) : LocationListener {

    var context: Context = context
    lateinit var logUtil: LogUtil

    // 위도, 경도
    private var lat: Double = 0.0
    private var lon: Double = 0.0

    var flag: Boolean = false

    private var mLocation: Location? = null
    private var mLocationManager: LocationManager? = null

    override fun onLocationChanged(location: Location) {
        if (location !== null) {
            lat = location.latitude;
            lon = location.longitude;
        }

        flag = true
        logUtil = LogUtil(TAG!!)
        logUtil.d("위치변경: $flag 위도: $lat, 경도:$lon")
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
    }


    /** ============================================================================================================= */

    fun getLocation() {

        try {
            mLocationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            mLocation = mLocationManager!!.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if ((ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED)
            ) {
                ActivityCompat.requestPermissions(
                    context as Activity,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSION_CODE
                )
            }
            mLocationManager!!.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 5f, this)

        } catch (e: Exception) {
            TAG?.let { LogUtil(it).e(e.toString()) };
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

        @Volatile
        private var instance: GPSUtil? = null

        @JvmStatic
        fun getInstance(context: Context): GPSUtil =
            instance ?: synchronized(this) {
                instance ?: GPSUtil(context).also {
                    instance = it
                }
            }

        private val TAG: String? = GPSUtil::class.simpleName;
        private const val LOCATION_PERMISSION_CODE = 2000
    }


}