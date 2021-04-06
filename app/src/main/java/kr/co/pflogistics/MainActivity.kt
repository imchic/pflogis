package kr.co.pflogistics

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.*
import com.naver.maps.map.overlay.Align
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.OverlayImage
import com.naver.maps.map.overlay.PathOverlay
import com.naver.maps.map.util.FusedLocationSource
import com.naver.maps.map.util.MarkerIcons
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.*

import android.app.SearchManager
import android.os.Handler
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import com.naver.maps.map.CameraAnimation

import com.naver.maps.map.CameraUpdate
import android.widget.Toast
import android.provider.SearchRecentSuggestions
import android.R.string.no
import android.app.AppOpsManager
import android.database.Cursor
import androidx.core.app.ActivityCompat


class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    lateinit var mContext: Context
    lateinit var locationSource: FusedLocationSource
    lateinit var naverMap: NaverMap
    lateinit var uiSettings: UiSettings

    lateinit var listView: ListView
    lateinit var alertDialog: AlertDialog.Builder
    lateinit var dialog: AlertDialog

    lateinit var gpsUtil: GPSUtil

    var lon: Double = 0.0
    var lat: Double = 0.0

    var lineOverLayList = ArrayList<LatLng>()
    val markers = mutableListOf<Marker>()
    val waypointArr = mutableListOf<String>()

    var requiresPermission = arrayOf(
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mContext = this

        initNaverMap(NAVER_LICENSE)
        locationSource = FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE)

        val searchManager = getSystemService(SEARCH_SERVICE) as SearchManager
        val searchView: SearchView = findViewById<SearchView>(R.id.search_view)

        //SearchRecentSuggestions(this, SearchHistoryProvider.AUTHORITY, SearchHistoryProvider.MODE).clearHistory() // recent value reset
        searchView.setIconifiedByDefault(false);
        searchView.queryHint = "주소입력"
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        searchView.setOnSuggestionListener(object : SearchView.OnSuggestionListener {
            override fun onSuggestionSelect(position: Int): Boolean {
                Log.d("sugSelect>>", position.toString())
                return true;
            }

            override fun onSuggestionClick(position: Int): Boolean {

                val cursor: Cursor = searchView.suggestionsAdapter.getItem(position) as Cursor
                val suggest1 = cursor.getString(cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1))
                Log.d("sugClick>>", suggest1)
                searchView.setQuery(suggest1, true)

                return true;
            }
        })
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String): Boolean {
                Log.d("change >>", newText); return false
            }

            override fun onQueryTextSubmit(query: String): Boolean {

                val searchRecentSuggestions =
                    SearchRecentSuggestions(mContext, SearchHistoryProvider.AUTHORITY, SearchHistoryProvider.MODE)
                searchRecentSuggestions.saveRecentQuery(query, null)

                val addrArr = listOf(4113152000, 4113555000)
                val addrItems = mutableListOf<String>()
                val sumStr: String = addrArr.joinToString(separator = ";")
                //HCODE@4113554500;4113555000
                //HCODE@4113152000;4113555000

                Log.d("sumStr >>", sumStr)
                Log.d("submit >>", query)

                var x: Double = 0.0
                var y: Double = 0.0

                alertDialog = AlertDialog.Builder(mContext)
                HttpUtil.getInstance(mContext).get(
                    "https://naveropenapi.apigw.ntruss.com/map-geocode/v2/geocode?query=$query",
                    object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            println(e.toString())
                        }

                        override fun onResponse(call: Call, response: Response) {
                            try {
                                if (txtAddr != null) {
                                    val result = JSONObject(response.body!!.string())
                                    val cnt = result.getJSONObject("meta").getString("totalCount")
                                    Log.d("response>>", "$result")
                                    Log.d("cnt>>>", result.getJSONObject("meta").getString("totalCount"))

                                    if (0 < cnt.toInt()) {
                                        for (i in 0 until result.getJSONArray("addresses").length()) {
                                            if ((result.getJSONArray("addresses")
                                                    .get(i) as JSONObject).getString("roadAddress").contains("성남시")
                                            ) {
                                                addrItems.add(
                                                    (result.getJSONArray("addresses")
                                                        .get(i) as JSONObject).getString("roadAddress")
                                                )
                                                Log.d(
                                                    "detailAddr>>>",
                                                    (result.getJSONArray("addresses")
                                                        .get(i) as JSONObject).getString("roadAddress")
                                                )
                                            }
                                        }

                                        runOnUiThread {
                                            val rowList: View = layoutInflater.inflate(R.layout.row, null)
                                            listView = rowList.findViewById(R.id.listView)

                                            val adapter =
                                                ArrayAdapter(mContext, android.R.layout.simple_list_item_1, addrItems)
                                            listView.adapter = adapter

                                            listView.setOnItemClickListener { parent, view, position, id ->
                                                val selectedItem = parent.getItemAtPosition(position) as String
                                                Log.d("selectedAddr>>", selectedItem)
                                                dialog.dismiss()

                                                HttpUtil.getInstance(mContext).get(
                                                    "https://naveropenapi.apigw.ntruss.com/map-geocode/v2/geocode?query=$selectedItem",
                                                    //"&filter=$sumStr",
                                                    object : Callback {
                                                        override fun onFailure(call: Call, e: IOException) {
                                                            println(e.toString())
                                                        }

                                                        override fun onResponse(call: Call, response: Response) {
                                                            runOnUiThread {
                                                                Handler().postDelayed({
                                                                    val searchLonLat =
                                                                        (JSONObject(response.body!!.string()).getJSONArray(
                                                                            "addresses"
                                                                        ).get(0) as JSONObject)
                                                                    x = searchLonLat.getString("x").toDouble()
                                                                    y = searchLonLat.getString("y").toDouble()
                                                                    val cameraUpdate =
                                                                        CameraUpdate.scrollAndZoomTo(LatLng(y, x), 18.0)
                                                                            .animate(CameraAnimation.Fly, 1500)
                                                                    naverMap.moveCamera(cameraUpdate)

                                                                    setMarker(y, x)

                                                                }, 500)
                                                            }

                                                        }
                                                    })
                                            }

                                            adapter.notifyDataSetChanged()
                                            alertDialog.setView(rowList)
                                            dialog = alertDialog.create()
                                            dialog.show()
                                        }
                                    } else {
                                        runOnUiThread {
                                            Toast.makeText(mContext, "똑바로 주소 입력해라!!!!!!", Toast.LENGTH_SHORT).show()
                                        }
                                    }

                                    //txtAddr.text = "현재 GPS: ${(JSONObject(response.body!!.string()).getJSONObject("response").getJSONArray("result").get(0) as JSONObject).getString("text")}"
                                }
                            } catch (e: NullPointerException) {
                                Log.e("hbim", e.toString())
                            }
                        }
                    });
                return false
            }
        })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (locationSource.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
            if (!locationSource.isActivated) { // 권한 거부됨
                naverMap.locationTrackingMode = LocationTrackingMode.None
            } else {
                naverMap.locationTrackingMode = LocationTrackingMode.Follow
            }
            return
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun initNaverMap(key: String) {

        NaverMapSdk.getInstance(this).client = NaverMapSdk.NaverCloudPlatformClient(key)

        val fm = supportFragmentManager
        val mapFragment = fm.findFragmentById(R.id.map_fragment) as MapFragment?
            ?: MapFragment.newInstance().also {
                fm.beginTransaction().add(R.id.map_fragment, it).commit()
            }

        mapFragment.getMapAsync(this)

    }

    override fun onMapReady(naverMap: NaverMap) {

        // GPS 위치정보
        gpsUtil = GPSUtil(mContext)
        gpsUtil.getLocation()

        lon = gpsUtil.getLatitude()
        lat = gpsUtil.getLongitude()

        this.naverMap = naverMap

        this.naverMap.locationSource = locationSource
        this.naverMap.isIndoorEnabled = true
        this.naverMap.locationTrackingMode = LocationTrackingMode.Follow

        this.uiSettings = naverMap.uiSettings
        this.uiSettings.isCompassEnabled = true
        this.uiSettings.isScaleBarEnabled = true
        this.uiSettings.isZoomControlEnabled = true
        this.uiSettings.isZoomGesturesEnabled = true
        this.uiSettings.isLocationButtonEnabled = true
        this.uiSettings.isRotateGesturesEnabled = false

        this.naverMap.moveCamera(
            CameraUpdate.toCameraPosition(
                CameraPosition(
                    LatLng(lon, lat),
                    this.naverMap.cameraPosition.zoom
                )
            ).animate(CameraAnimation.Easing)
        )

        this.naverMap.addOnLocationChangeListener { location ->

            naverMap.locationTrackingMode = LocationTrackingMode.Follow

            println("$lon, $lat")

            Log.d("mapChange>>>", "${location.latitude}, ${location.longitude}")
            runOnUiThread {
                HttpUtil.getInstance(this).get(
                    //"$VWORLD_GEOCODER_ADDR_API_URL&point=${lonlat.longitude},${lonlat.latitude}&format=json&type=road&zipcode=true&simple=false&key=$VWORLD_LICENSE",
                    "https://dapi.kakao.com/v2/local/geo/coord2address.json?x=${location.longitude}&y=${location.latitude}&input_coord=WGS84",
                    object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            println(e.toString())
                        }

                        override fun onResponse(call: Call, response: Response) {
                            try {
                                //Log.d("response>>>", (JSONObject(response.body!!.string()).getJSONArray("documents").get(0) as JSONObject).getJSONObject("address").getString("address_name"))
                                //if (txtAddr != null) {
                                runOnUiThread {
                                    txtAddr.text = "현재 GPS: ${(JSONObject(response.body!!.string()).getJSONArray("documents").get(0) as JSONObject).getJSONObject("address").getString("address_name")}"
                                }
                                //}
                            } catch (e: NullPointerException) {
                                Log.e("hbim", e.toString())
                            }
                        }
                    });
            }
        }

    }

    /* override fun onCreateOptionsMenu(menu: Menu?): Boolean {
         menuInflater.inflate(R.menu.map_menu, menu)
         return super.onCreateOptionsMenu(menu)
     }

     override fun onOptionsItemSelected(item: MenuItem): Boolean {
         // 클릭된 메뉴 아이템의 아이디 마다 when 구절로 클릭시 동작을 설정한다.
         when(item.itemId){
             R.id.search->{ // 검색 버튼
                 Log.d("hbim", "addr")
             }
         }
         return super.onOptionsItemSelected(item)
     }*/


    /***  마커 객체 생성*/
    private fun setMarker(lat: Double, lon: Double) {
        //onMapGetXY(lon, lat)
        val marker = Marker()
        marker.position = LatLng(lat, lon)

        var caption: String = ""

        if(markers.size == 0) caption = "출발지"

        for (i in 0 until markers.size) {
            caption = "목적지${i + 1}"
        }
        if(lineOverLayList.size == 0){
            if (lat != 0.0) {
                for (i in 0..2) {
                    lineOverLayList.add(LatLng(lat, lon))
                }
            }
        } else {
            lineOverLayList.add(LatLng(lat, lon))
        }
        Log.d("markerSize>>", markers.size.toString())

        setMarkerStyle(marker, MarkerIcons.BLACK, getColor(R.color.royal_blue), Marker.SIZE_AUTO, Marker.SIZE_AUTO)
        setMarkerText(marker, caption, Align.Top)
        markers.add(marker)

        markers.forEach { marker ->
            marker.map = naverMap
        }

        addLineOverLay(naverMap, lat, lon)

    }

    /***  마커 스타일*/
    private fun setMarkerStyle(marker: Marker, icons: OverlayImage, iconColor: Int, width: Int, height: Int): Marker {
        marker.icon = icons
        marker.iconTintColor = iconColor
        marker.width = width
        marker.height = height
        marker.map = this.naverMap
        return marker
    }

    /**
     *  마커 스타일 (텍스트)
     */
    private fun setMarkerText(marker: Marker, text: String, aligns: Align): Marker {
        marker.captionText = text
        //marker.captionHaloColor = getColor(R.color.crimson)
        marker.captionTextSize = 16f
        marker.setCaptionAligns(aligns)
        return marker
    }

    /**
     * 경로선
     */

    fun addLineOverLay(map: NaverMap, lat: Double, lon: Double) {

        Log.d("addLineCnt>>>", "${lineOverLayList.size}")

        HttpUtil.getInstance(this).get("https://naveropenapi.apigw.ntruss.com/map-direction-15/v1/driving?start=${lineOverLayList.get(0).longitude},${lineOverLayList.get(0).latitude}&goal=$lon,$lat&option=trafast",
            object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    println(e.toString())
                }

                override fun onResponse(call: Call, response: Response) {
                    val resultRoot = JSONObject(response.body!!.string()).toString()

                    if(JSONObject(resultRoot).getString("code").toString() == "0"){

                        val rootPath = (JSONObject(resultRoot).getJSONObject("route").getJSONArray("trafast").get(0) as JSONObject).getJSONArray("path")

                        for (i in 0 until rootPath.length()) {
                            waypointArr.add(rootPath[i].toString())

                            //Log.d("pass", resultRoot)
                        }

                    }

                }
        })

        val pathOverLay = PathOverlay()
        pathOverLay.color = Color.BLUE
        pathOverLay.patternImage = OverlayImage.fromResource(R.drawable.path_pattern)
        pathOverLay.patternInterval = 10

        //lineOverLayList.add(LatLng(lat, lon))

        pathOverLay.coords = lineOverLayList
        pathOverLay.map = map

    }

    /**
     *  현재 위경도 가져오기
     */
    fun onMapGetXY(x: Double, y: Double) {
        Log.d("onMapGetXY>>", "$x, $y")
    }

    companion object {

        private const val NAVER_LICENSE = "ywgp3sltx8"
        private const val VWORLD_LICENSE = "615D799D-2BD6-3342-B58B-123B6D62BB91"
        private const val VWORLD_GEOCODER_ADDR_API_URL = // 좌표를 주소로 변환
            "http://api.vworld.kr/req/address?service=address&request=getAddress&version=2.0&crs=epsg:4326&"
        private const val VWORLD_GEOCODER_COORD_API_URL = // 주소를 좌표로 변환
            "http://api.vworld.kr/req/address?service=address&request=getCoord&version=2.0&crs=epsg:4326&"

        private const val LOCATION_PERMISSION_REQUEST_CODE = 1000
        private val TAG: String? = MainActivity::class.simpleName
    }

}