package kr.co.pflogistics

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat


class PermissionUtil {

    // 지정한 Permission check
    val REQ_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.INTERNET,
        Manifest.permission.CAMERA,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.VIBRATE,
        Manifest.permission.NFC
    )

    fun hasPermission(context: Context?): Boolean {
        for (permission in REQ_PERMISSIONS) {
            if (ActivityCompat.checkSelfPermission(
                    context!!,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) return false
        }
        return true
    }

    fun requestPermission(context: Context?) {
        for (permission in REQ_PERMISSIONS) {
            if (ActivityCompat.checkSelfPermission(context!!, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions((context as MainActivity?)!!, REQ_PERMISSIONS, PERMISSION_CODE)
                return
            }
        }
    }

    fun shouldShowRequestPermissionRationale(context: Context?): Boolean {
        for (permission in REQ_PERMISSIONS) {
            // true : 사용자가 전에 해당 요청을 거부
            // false : 거부했으며 다시묻지않음 또는 기기 정책에서 권한을 금지한 경우
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    (context as IntroActivity?)!!,
                    permission
                )
            ) return true
        }
        return false
    }

    fun launchPermissionSettings(context: Context) {
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        intent.data = Uri.fromParts("package", context.packageName, null)
        (context as MainActivity).startActivity(intent)
    }

    companion object{
        @Volatile private var instance: PermissionUtil? = null

        @JvmStatic fun getInstance(context: Context): PermissionUtil =
            instance ?: synchronized(this) {
                instance ?: PermissionUtil().also {
                    instance = it
                }
            }

        private val TAG: String? = PermissionUtil::class.simpleName;
        private const val PERMISSION_CODE = 2000
    }
}