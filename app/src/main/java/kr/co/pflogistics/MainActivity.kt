/*
 * Create by hbim on 2021. 4. 7.
 * Copyright (c) 2021. hbim. All rights reserved.
 */

package kr.co.pflogistics

import android.Manifest
import android.app.SearchManager
import android.content.Context
import android.content.DialogInterface
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.SearchRecentSuggestions
import android.transition.Slide
import android.util.Log
import android.view.Gravity
import android.view.Window
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.PermissionChecker
import com.google.android.material.bottomnavigation.BottomNavigationMenuView
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.*
import com.naver.maps.map.overlay.*
import com.naver.maps.map.util.FusedLocationSource
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException


class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    // common
    lateinit var mContext: Context
    lateinit var alertDialog: AlertDialog.Builder
    lateinit var bottomNavigationMenuView: BottomNavigationMenuView
    lateinit var gson:Gson

    // naverSdk
    lateinit var naverMap: NaverMap
    lateinit var locationSource: FusedLocationSource
    lateinit var uiSettings: UiSettings
    lateinit var marker:Marker
    lateinit var rootOverLay:PolylineOverlay

    // customUtil
    lateinit var gpsUtil: GPSUtil
    lateinit var logUtil: LogUtil

    var lon:Double = 0.0
    var lat:Double = 0.0

    // list
    var lineLatLngArr = ArrayList<LatLng>() // PolyLine
    var markersArr = mutableListOf<Overlay>() // 마커
    var waypointArr = mutableListOf<LatLng>() //  경유지
    var selectedAddrArr = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            with(window) {
                requestFeature(Window.FEATURE_CONTENT_TRANSITIONS)
                // set an slide transition
                enterTransition = Slide(Gravity.END)
                exitTransition = Slide(Gravity.START)
            }
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        locationSource = FusedLocationSource(this, PERMISSION_REQUEST_CODE)
        initUI()
        initNaverMap(NAVER_LICENSE)
    }

    override fun onStart() {
        super.onStart()
        if(hasPermission()){
            map_fragment.onStart()
        }
    }

    override fun onResume() {
        super.onResume()
        map_fragment.onResume()
    }

    override fun onPause() {
        super.onPause()
        map_fragment.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        map_fragment.onSaveInstanceState(outState)
    }

    override fun onStop() {
        super.onStop()
        map_fragment.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        map_fragment.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        map_fragment.onLowMemory()
    }

    fun initUI(){

        mContext = this
        logUtil = LogUtil(TAG!!)
        alertDialog = AlertDialog.Builder(mContext)

        //sw_map_switch.text = "일반/위성"
        val searchManager = getSystemService(SEARCH_SERVICE) as SearchManager
        val searchView: SearchView = findViewById(R.id.search_view)
        //SearchRecentSuggestions(this, SearchHistoryProvider.AUTHORITY, SearchHistoryProvider.MODE).clearHistory() // recent value reset

        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        searchView.setIconifiedByDefault(false)
        searchView.isSubmitButtonEnabled = true
        searchView.queryHint = "주소입력 (성남시만 해당)"
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        searchView.setOnSuggestionListener(object : SearchView.OnSuggestionListener {
            override fun onSuggestionSelect(position: Int): Boolean {
                Log.d("sugSelect>>", position.toString())
                return true
            }

            override fun onSuggestionClick(position: Int): Boolean {
                val cursor: Cursor = searchView.suggestionsAdapter.getItem(position) as Cursor
                val suggest1 = cursor.getString(cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1)) // 선택된 recentQuery String
                logUtil.d("서치뷰 스트링 -> $suggest1")
                searchView.setQuery(suggest1, true)
                return true
            }
        })
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String): Boolean { return false }
            override fun onQueryTextSubmit(query: String): Boolean {

                val searchRecentSuggestions = SearchRecentSuggestions(mContext, SearchHistoryProvider.AUTHORITY, SearchHistoryProvider.MODE)
                searchRecentSuggestions.saveRecentQuery(query, null)

                val addrArr = listOf(4113152000, 4113555000)
                val addrItems = mutableListOf<String>()
                addrArr.joinToString(separator = ";")

                HttpUtil.getInstance(mContext).callerUrlInfo(
                    "https://naveropenapi.apigw.ntruss.com/map-geocode/v2/geocode?query=$query",
                    object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            runOnUiThread { Toast.makeText(mContext, e.toString(), Toast.LENGTH_LONG).show() }
                        }
                        override fun onResponse(call: Call, response: Response) {
                            try {
                                val result = JSONObject(response.body!!.string())
                                val cnt = result.getJSONObject("meta").getString("totalCount")
                                var selectedItem = ""

                                if (0 < cnt.toInt()) {
                                    for (i in 0 until result.getJSONArray("addresses").length()) {
                                        if ((result.getJSONArray("addresses").get(i) as JSONObject).getString("roadAddress").contains("성남시")) {
                                            addrItems.add((result.getJSONArray("addresses").get(i) as JSONObject).getString("roadAddress"))
                                            selectedItem = (result.getJSONArray("addresses").get(i) as JSONObject).getString("roadAddress").toString()
                                        }
                                    }

                                    HttpUtil.getInstance(mContext)
                                        .callerUrlInfo("https://naveropenapi.apigw.ntruss.com/map-geocode/v2/geocode?query=$selectedItem",
                                            object : Callback {
                                                override fun onFailure(call: Call, e: IOException) { logUtil.e(e.toString()) }
                                                override fun onResponse(call: Call, response: Response) {
                                                    runOnUiThread {
                                                        Handler().postDelayed({
                                                            val searchLonLat = (JSONObject(response.body!!.string()).getJSONArray("addresses").get(0) as JSONObject)
                                                            var searchlat = searchLonLat.getString("x").toDouble()
                                                            var searchLon = searchLonLat.getString("y").toDouble()
                                                            val cameraUpdate = CameraUpdate.scrollAndZoomTo(LatLng(searchLon, searchlat), 18.0).animate(CameraAnimation.Fly, 1500)
                                                            naverMap.moveCamera(cameraUpdate)

                                                            // TODO: 2021-04-09  주소 지정 검색 후 리스트에 담아놔야함.
                                                            
                                                            var memoTxt = EditText(mContext)
                                                            
                                                            alertDialog
                                                                .setIcon(R.drawable.ic_placeholder)
                                                                .setTitle("$selectedItem")
                                                                .setMessage("지도 내에 해당 주소를 표시하시겠습니까? \n 메모는 선택사항")
                                                                .setPositiveButton("확인") { dialog, which -> setMarker(searchLon, searchlat, memoTxt.text.toString()) }
                                                                .setNegativeButton("취소", null)
                                                                .setView(memoTxt)
                                                                .create()
                                                            alertDialog.show()

                                                        }, 500)
                                                    }

                                                }
                                            })


                                } else {
                                    runOnUiThread {
                                        Toast.makeText(mContext, "주소를 확인해주세요.(성남시 3개구 기준)", Toast.LENGTH_SHORT).show()
                                    }
                                }

                            } catch (e: NullPointerException) {
                                logUtil.e(e.toString())
                            }

                        }
                    })
                return false
            }
        })

        btn_clear.setOnClickListener {
            alertDialog = AlertDialog.Builder(mContext)
            alertDialog
                .setTitle("초기화")
                .setMessage("경로지정 및 경로선 초기화를 진행하시겠습니까?")
                .setPositiveButton("확인") { dialog, which -> objectClear() }
                .setNegativeButton("취소", null)
                .create()
            alertDialog.show()
        }

        bottom_navigation.setOnNavigationItemSelectedListener { item ->
            when(item.itemId) {
                R.id.page_map -> {
                    logUtil.i("map"); true
                }
                R.id.page_list -> {
                    logUtil.i("list"); true
                }
                else -> false
            }
        }

    }

    // 네이버 MAP SDK Init
    fun initNaverMap(key: String) {
        NaverMapSdk.getInstance(this).client = NaverMapSdk.NaverCloudPlatformClient(key)
        val fm = supportFragmentManager
        val mapFragment = fm.findFragmentById(R.id.map_fragment) as MapFragment? ?: MapFragment.newInstance().also { fm.beginTransaction().add(R.id.map_fragment, it).commit() }
        mapFragment.getMapAsync(this)

        // GPS 위치정보
        gpsUtil = GPSUtil(mContext)
        gpsUtil.getLocation()

        lat = gpsUtil.getLatitude()
        lon = gpsUtil.getLongitude()

        logUtil.d("$lat, $lon")
    }

    // 네이버 MAP SDK
    override fun onMapReady(naverMap: NaverMap) {

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
        this.uiSettings.isRotateGesturesEnabled = true

        /*sw_map_switch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                this.naverMap.mapType = NaverMap.MapType.Hybrid
            } else {
                this.naverMap.mapType = NaverMap.MapType.Basic
            }
        }*/

        onMapGetXY(lon, lat)
        cameraUpdate()

    }

    // 퍼미션 체크
    fun hasPermission(): Boolean {
        return PermissionChecker.checkSelfPermission(this, PERMISSIONS[0]) ==
                PermissionChecker.PERMISSION_GRANTED &&
                PermissionChecker.checkSelfPermission(this, PERMISSIONS[1]) ==
                PermissionChecker.PERMISSION_GRANTED
    }

    // 퍼미션 결과
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (locationSource.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
            if (hasPermission()) {
                naverMap.locationTrackingMode = LocationTrackingMode.Follow
            }
            /*if (!locationSource.isActivated) { // 권한 거부됨
                naverMap.locationTrackingMode = LocationTrackingMode.None
            }*/
            return
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    // RestAPI String to JSONObject
    fun getResultToGSON(resultStr: String): JsonObject {
        gson = Gson()
        return JsonParser().parse(resultStr).asJsonObject
    }

    // 현재 위경도 가져오기
    fun onMapGetXY(lon: Double, lat: Double) {
        runOnUiThread {
            HttpUtil.getInstance(this).callerUrlInfo(
                "$VWORLD_GEOCODER_ADDR_API_URL&point=$lon,$lat&format=json&type=parcel&zipcode=true&simple=false&key=$VWORLD_LICENSE",
                object : Callback {
                    override fun onFailure(call: Call, e: IOException) { println(e.toString()) }
                    override fun onResponse(call: Call, response: Response) {
                        var resultData = getResultToGSON(response.body!!.string())
                        var status = resultData.get("response").asJsonObject.get("status").asString

                        if (status != "NOT_FOUND") {
                            //var point = resultData.get("response").asJsonObject.get("input").asJsonObject.get("point").asJsonObject
                            var addr = (resultData.get("response").asJsonObject.get("result").asJsonArray.get(0).asJsonObject.get("text").asString)
                            runOnUiThread { ->
                                //if (status != "NOT_FOUND") { txtAddr.text = "지번주소: $addr" } else Toast.makeText(mContext, "잘못된 결과값", Toast.LENGTH_SHORT).show()
                            }
                        }

                    }
                });
        }
    }

    //마커 객체 생성
    fun setMarker(lat: Double, lon: Double, memo:String) {
        val infoWindow = InfoWindow()
        infoWindow.position = LatLng(lat, lon)
        infoWindow.adapter = object : InfoWindow.DefaultTextAdapter(mContext) {
            override fun getText(infoWindow: InfoWindow): CharSequence {
                return "정보 창 내용"
            }
        }

        val markerIcon = OverlayImage.fromResource(R.drawable.ic_location)
        marker = Marker()
        marker.tag = memo
        marker.icon = markerIcon
        marker.position = LatLng(lat, lon)

        markersArr.add(marker)
        logUtil.i("Map Marker Size -> ${markersArr.size}")

        markersArr.forEach { marker -> marker.map = naverMap } // Array에 따라 Marker 생성
        //addLineOverLay(naverMap, lat, lon)

    }

    // 이동경로 표시
    /*fun addLineOverLay(map: NaverMap, lat: Double, lon: Double) {

        val sumStr = lon.toString() + lat.toString()
        selectedAddrArr.add(sumStr)
        val sumSelectedAddr: String = selectedAddrArr.joinToString(separator = ":")

        val pathOverLay = PathOverlay()
        rootOverLay = PolylineOverlay(
        rootOverLay.joinType = PolylineOverlay.LineJoin.Round
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            rootOverLay.color = getColor(R.color.cobalt_blue)
        }
        rootOverLay.width = 10

        if(lineLatLngArr.size > 3){ // 최초 출발지에서 검색된 주소지로 이동 루트 표현
            //HttpUtil.getInstance(this).get("https://naveropenapi.apigw.ntruss.com/map-direction-15/v1/driving?start=${lineOverLayList.get(0).longitude},${lineOverLayList.get(0).latitude}&goal=$lon,$lat&option=trafast",
            HttpUtil.getInstance(this).callerUrlInfo(
                "https://naveropenapi.apigw.ntruss.com/map-direction-15/v1/driving?" +
                        "start=${lineLatLngArr[0].longitude},${lineLatLngArr[0].latitude}" +
                        "waypoint=$sumSelectedAddr"+
                        "&goal=$lon,$lat" +
                        "&option=trafast",
                object : Callback {
                    override fun onFailure(call: Call, e: IOException) { logUtil.e(e.toString()) }
                    override fun onResponse(call: Call, response: Response) {
                        val resultRoot = getResultToGSON(response.body!!.string())!!

                        *//**
                         * code = '0' 성공 , '-1' 실패
                         * Congestion code(구간혼잡도) '1' 원활 '2' 서행 '3' 혼잡
                         * Speed(평균속도)  원활 30이상, 서행 15~30, 혼잡 15미만
                         *//*
                        if(resultRoot.get("code").asString == "0"){
                            val trafast = resultRoot.get("route").asJsonObject.get("trafast").asJsonArray.get(0)

                            val totalDistance = ((trafast as JsonObject).get("summary") as JsonObject).get("distance").asString.toInt() * 0.001
                            var duration = (trafast.get("summary") as JsonObject).get("duration").asString.toInt()
                            duration = ((duration / (1000 * 60)) % 60) // 밀리세컨드를 분으로

                            val rootPath = trafast.asJsonObject.get("path").asJsonArray // 경로
                            val selection = trafast.asJsonObject.get("section").asJsonArray // 섹션
                            val guildLine = trafast.asJsonObject.get("guide").asJsonArray // 가이드라인

                            var rootLonLat:String
                            var sumSectionMsg:String = ""

                            for(i in 0 until rootPath.size()) {
                                rootLonLat = rootPath[i].toString().replace("[", "").replace("]", "")
                                waypointArr.add(LatLng(rootLonLat.split(",")[1].toDouble(), rootLonLat.split(",")[0].toDouble()))
                            }

                            for(i in 0 until selection.size()){
                                //logUtil.i("길찾기 세션: ${getResultToGSON(selection[i].toString())}")
                                var sectionDistance = getResultToGSON(selection[i].toString()).get("distance").asString
                                sectionDistance = if (sectionDistance.toString().length > 3) { String.format("%.2f", (sectionDistance.toInt() * 0.001)) + "km" } else { sectionDistance.toString() + "m" }

                                var sectionCongestionCode = getResultToGSON(selection[i].toString()).get("congestion").asString
                                when (sectionCongestionCode) {
                                    "0" -> sectionCongestionCode = "거의없음"
                                    "1" -> sectionCongestionCode = "원활"
                                    "2" -> sectionCongestionCode = "서행"
                                    "3" -> sectionCongestionCode = "정체"
                                }
                                var sectionCongestionSpeed = getResultToGSON(selection[i].toString()).get("speed").asString + "km/h"

                                sumSectionMsg += "거리:${sectionDistance}  혼잡도:$sectionCongestionCode  평균속도:$sectionCongestionSpeed \n"
                                logUtil.i(sumSectionMsg)
                                runOnUiThread {

//                                    bottompanel.isClickable = true
//                                    val bottomSheetBehavior: BottomSheetBehavior<*>
//                                    bottomSheetBehavior = BottomSheetBehavior.from(bottompanel)
//                                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
//                                    bottomSheetBehavior.isDraggable = true
//
//                                    bottomSheetBehavior.addBottomSheetCallback(object :
//                                        BottomSheetBehavior.BottomSheetCallback() {
//                                        override fun onStateChanged(bottomSheet: View, state: Int) {
//                                            when (state) {
//                                                BottomSheetBehavior.STATE_EXPANDED -> { logUtil.d("STATE_EXPANDED"); }
//                                                BottomSheetBehavior.STATE_COLLAPSED -> { logUtil.d("STATE_COLLAPSED"); } // peek 높이 만큼 보이는 상태
//                                                BottomSheetBehavior.STATE_HALF_EXPANDED -> { logUtil.d("STATE_HALF_EXPANDED"); } // 반만 보임
//                                                BottomSheetBehavior.STATE_HIDDEN -> { logUtil.d("STATE_HIDDEN "); } // 숨김 상태
//                                            }
//                                        }
//
//                                        override fun onSlide(bottomSheet: View, slideOffset: Float) {}
//                                    });
//
//                                    bot_txt_distance.text = "총 거리: ${String.format("%.2f",totalDistance)}km \t 소요시간: ${duration}분"
//                                    txtRoute.text = sumSectionMsg
                                }
                            }

                            var sumGuidenMsg:String = ""
                            for(i in 0 until guildLine.size()){
                                logUtil.i("가이드라인: ${getResultToGSON(guildLine[i].toString())}")

                                var guildeMsg = getResultToGSON(guildLine[i].toString()).get("instructions").asString
                                var guildeDistance = getResultToGSON(guildLine[i].toString()).get("distance").asString
                                var guideDuration = (getResultToGSON(guildLine[i].toString()).get("duration").asString.toInt() % 3600) /60
                                guildeDistance = if (guildeDistance.toString().length > 3) { String.format("%.2f", (guildeDistance.toInt() * 0.001)) + "km" } else { guildeDistance.toString() + "m" }

                                sumGuidenMsg += "상세안내:${guildeMsg}  거리:$guildeDistance \n"
                            }

//                            runOnUiThread { txtGuide.text = sumGuidenMsg }

                            runOnUiThread {
                                rootOverLay.coords = waypointArr;
                                rootOverLay.map = map
                            }

                        } else {
                            runOnUiThread { Toast.makeText(mContext, resultRoot.get("message").asString, Toast.LENGTH_SHORT).show() }
                        }

                    }
                })


            //pathOverLay.color = Color.BLUE
            //pathOverLay.patternImage = OverlayImage.fromResource(R.drawable.path_pattern)
            //pathOverLay.patternInterval = 10
            //lineOverLayList.add(LatLng(lat, lon))
            //pathOverLay.coords = lineOverLayList
            //pathOverLay.map = map
        }

    }*/

    // 객체 초기화
    fun objectClear(){
        logUtil.d("Makrer Array Size -> ${markersArr.size}")
        if(markersArr.size > 0){

//            lineLatLngArr = mutableListOf<LatLng>() as ArrayList<LatLng>
//            waypointArr = mutableListOf<LatLng>() as ArrayList<LatLng>
//            selectedAddrArr = mutableListOf<String>() as ArrayList<String>

//            marker.map = naverMap

            markersArr.forEach { marker -> marker.map = null  }
            markersArr = mutableListOf<Overlay>() as ArrayList<Overlay>

        } else {
            Toast.makeText(mContext, "목적지 및 경로선이 존재하지 않습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    // 카메라 이동
    fun cameraUpdate() = CameraUpdate.toCameraPosition(CameraPosition(LatLng(lat, lon), this.naverMap.cameraPosition.zoom)).animate(CameraAnimation.Easing)

    companion object {

        private const val NAVER_LICENSE = "ywgp3sltx8"
        private const val VWORLD_LICENSE = "615D799D-2BD6-3342-B58B-123B6D62BB91"
        private const val VWORLD_GEOCODER_ADDR_API_URL =  "http://api.vworld.kr/req/address?service=address&request=getAddress&version=2.0&crs=epsg:4326&" // 좌표를 주소로 변환
        private const val VWORLD_GEOCODER_COORD_API_URL = "http://api.vworld.kr/req/address?service=address&request=getCoord&version=2.0&crs=epsg:4326&" // 주소를 좌표로 변환

        private const val PERMISSION_REQUEST_CODE = 1000
        private val PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION)
        private val TAG: String? = MainActivity::class.simpleName
    }

}