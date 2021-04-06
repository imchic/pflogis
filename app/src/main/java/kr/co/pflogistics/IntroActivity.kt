package kr.co.pflogistics

import android.Manifest
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import com.naver.maps.map.LocationTrackingMode
import com.naver.maps.map.util.FusedLocationSource
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class IntroActivity : AppCompatActivity() {

    lateinit var mContext:Context
    lateinit var locationSource: FusedLocationSource

    var requiresPermission = arrayOf(
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro)

        mContext = this
        locationSource = FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE)
        permissionCheckUtil()
    }

    private fun permissionCheckUtil():Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return false
        } else {
            for (permission in requiresPermission) {
                val checkPermission = checkCallingOrSelfPermission(permission)
                if (checkPermission == PackageManager.PERMISSION_DENIED) {
                    requestPermissions(requiresPermission, 0)
                    break
                }
            }
        }
        return true;
    }

    companion object{
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1000
    }
}