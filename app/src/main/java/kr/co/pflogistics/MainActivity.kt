/*
 * Create by hbim on 2021. 4. 7.
 * Copyright (c) 2021. hbim. All rights reserved.
 */

package kr.co.pflogistics

import android.Manifest
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.PointF
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
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.PermissionChecker
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.*
import com.naver.maps.map.overlay.*
import com.naver.maps.map.util.FusedLocationSource
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException


class MainActivity : AppCompatActivity(), OnMapReadyCallback, ItemDragListener {

    // common
    lateinit var mContext: Context
    lateinit var alertDialog: AlertDialog.Builder
    lateinit var gson: Gson
    lateinit var searchView: SearchView

    // naverSdk
    lateinit var naverMap: NaverMap
    lateinit var locationSource: FusedLocationSource
    lateinit var uiSettings: UiSettings
    lateinit var marker: Marker
    lateinit var pathOverLay:PathOverlay
    lateinit var infoWindow: InfoWindow

    // customUtil
    lateinit var gpsUtil: GPSUtil
    lateinit var logUtil: LogUtil
    lateinit var prefs: PreferenceUtil
    lateinit var db: PfDB

    lateinit var data: Data
    var dataList = ArrayList<Data>()
    val dFragment = DestinationFragment()
    val gFragment = DestinationFragment()
    var fragmentManager  =  supportFragmentManager

    var lon: Double = 0.0
    var lat: Double = 0.0
    var sumGuidenMsg = ""

    // list
    var markersArr = mutableListOf<Overlay>() // 마커
    var infoWindowArr = mutableListOf<Overlay>() // infoWindow
    var waypointLatLngArr = mutableListOf<LatLng>() //  경유지
    var waypointArr = mutableListOf<Overlay>() //  경유지
    var distanceArr = mutableListOf<String>() // 데이터에 등록된 배송지 순차적으로 나열

    @RequiresApi(Build.VERSION_CODES.M)
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
        db  = PfDB.getInstance(this)!!
        gson = Gson()

        initUI()
        initNaverMap(NAVER_LICENSE)
    }

    override fun onStart() {
        super.onStart()
        if (hasPermission()) {
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
        //map_fragment.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        map_fragment.onLowMemory()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    fun handleIntent(intent: Intent?) {
        if (Intent.ACTION_SEARCH == intent!!.action) {
            val query = intent.getStringExtra(SearchManager.QUERY)
            searchView.setQuery(query, false)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun initUI() {

        mContext = this
        logUtil = LogUtil(TAG!!)
        alertDialog = AlertDialog.Builder(mContext)

        //sw_map_switch.text = "일반/위성"
        val searchManager = getSystemService(SEARCH_SERVICE) as SearchManager
        searchView = findViewById(R.id.search_view)
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
                val suggest1 =
                    cursor.getString(cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1)) // 선택된 recentQuery String
                logUtil.d("서치뷰 스트링 -> $suggest1")
                searchView.setQuery(suggest1, true)
                return true
            }
        })
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String): Boolean {
                return false
            }

            override fun onQueryTextSubmit(query: String): Boolean {

                val searchRecentSuggestions =
                    SearchRecentSuggestions(mContext, SearchHistoryProvider.AUTHORITY, SearchHistoryProvider.MODE)
                searchRecentSuggestions.saveRecentQuery(query, null)

                var selectedItem = ""


                var searchLat: Double = 0.0
                var searchLon: Double = 0.0

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

                                if (0 < cnt.toInt()) {
                                    for (i in 0 until result.getJSONArray("addresses").length()) {
                                        if ((result.getJSONArray("addresses").get(i) as JSONObject).getString("roadAddress").contains("성남시")) {
                                            selectedItem = (result.getJSONArray("addresses").get(i) as JSONObject).getString("roadAddress").toString()
                                        }
                                    }

                                    HttpUtil.getInstance(mContext)
                                        .callerUrlInfo("https://naveropenapi.apigw.ntruss.com/map-geocode/v2/geocode?query=$selectedItem",
                                            object : Callback {
                                                override fun onFailure(call: Call, e: IOException) = logUtil.e(e.toString())
                                                override fun onResponse(call: Call, response: Response) {
                                                    runOnUiThread {
                                                        Handler().postDelayed({
                                                            val searchLonLat =
                                                                (JSONObject(response.body!!.string()).getJSONArray("addresses")
                                                                    .get(0) as JSONObject)
                                                            searchLat = searchLonLat.getString("x").toDouble()
                                                            searchLon = searchLonLat.getString("y").toDouble()
                                                            val cameraUpdate = CameraUpdate.scrollAndZoomTo(
                                                                LatLng(
                                                                    searchLon,
                                                                    searchLat
                                                                ), 16.0
                                                            ).animate(CameraAnimation.Fly, 1500)
                                                            naverMap.moveCamera(cameraUpdate)

                                                            val sumLonLat = "$searchLon, $searchLat"

                                                            // TODO: 2021-04-09  주소 지정 검색 후 리스트에 담아놔야함.
                                                            val memoTxt = EditText(mContext)

                                                            alertDialog
                                                                .setIcon(R.drawable.ic_placeholder)
                                                                .setTitle("$selectedItem")
                                                                .setMessage("지도 내에 해당 주소를 표시하시겠습니까? \n 메모는 선택사항")
                                                                .setPositiveButton("확인") { dialog, which -> setMarker(searchLon, searchLat, memoTxt.text.toString())
                                                                    GlobalScope.launch(Dispatchers.IO) {
                                                                        delay(1000L)
                                                                        data = Data(0, selectedItem, sumLonLat, memoTxt.text.toString())
                                                                        db.dao().insert(data)
                                                                        logUtil.d("내부 DB QUERY -> insert()")
                                                                    }
                                                                }
                                                                .setNegativeButton("취소", null)
                                                                .setView(memoTxt)
                                                                .create()
                                                            alertDialog.show()

                                                        }, 500)
                                                    }

                                                }
                                            })

                                } else {
                                    runOnUiThread { Toast.makeText(mContext, "주소를 확인해주세요.(성남시 3개구 기준)", Toast.LENGTH_SHORT).show() }
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
            alertDialog.setTitle("초기화").setMessage("경로지정 및 경로선 초기화를 진행하시겠습니까?").setPositiveButton("확인") { _, _ -> objectClear() }.setNegativeButton("취소", null).create()
            alertDialog.show()
        }

        btn_distance.setOnClickListener{
            val mainAdapter = MainAdapter(mContext, dataList, this)
            dataList = mutableListOf<Data>() as ArrayList<Data>

            db.dao().getAll().observe(this) { values ->
                (JsonParser.parseString(gson.toJson(values)) as JsonArray).forEach { element ->
                    logUtil.d(element.toString())
                    dataList.add(Data((element as JsonObject).get("seq").asString.toInt(), element.get("addr").asString, element.get("lonlat").asString, element.get("memo").asString))
                    setMarker(element.get("lonlat").asString.split(",")[0].toDouble(), element.get("lonlat").asString.split(",")[1].toDouble(), element.get("memo").asString)
                }
            }
            addLineOverLay(naverMap, mainAdapter.dataList)
        }

        bottom_navigation.setOnNavigationItemSelectedListener { item -> when (item.itemId) {
            R.id.page_map -> {
                logUtil.i("fragment_map")

                objectClear()
                GlobalScope.launch {
                    prefs.getString("del", "").split(",").forEach { el ->
                        db.dao().deleteSeq(el.toLong())
                    }
                }

                fragmentManager.beginTransaction().remove(dFragment).commit()

                // 기존 데이터가 존재할시에 마커 및 메모 표현
                db.dao().getAll().observe(this) { values ->
                    (JsonParser.parseString(gson.toJson(values)) as JsonArray).forEach { element ->
                        logUtil.d("DB주소-> $element")
                        dataList.add(Data((element as JsonObject).get("seq").asString.toInt(), element.get("addr").asString, element.get("lonlat").asString, element.get("memo").asString))
                        setMarker(element.get("lonlat").asString.split(",")[0].toDouble(), element.get("lonlat").asString.split(",")[1].toDouble(), element.get("memo").asString)
                    }
                }
                true
            }
            R.id.page_list -> {
                logUtil.i("fragment_destinationList")
                if (dFragment.isAdded) fragmentManager.beginTransaction().remove(dFragment) else fragmentManager.beginTransaction().add(R.id.fragment_container, dFragment).show(dFragment).commit()
                true
            }

            R.id.page_guide -> {
                logUtil.i("fragmentDistanceGuideLine")

                if(sumGuidenMsg != ""){
                    alertDialog
                        .setIcon(R.drawable.ic_location)
                        .setTitle("경로안내")
                        .setMessage(sumGuidenMsg)
                        .setPositiveButton("확인") { dialog, which ->  }
                        .setNegativeButton("취소", null)
                        .create()
                    alertDialog.show()
                } else {
                    Toast.makeText(mContext, "경로가 지정되지 않았습니다.", Toast.LENGTH_SHORT).show()
                }
                true
            }

            else -> false }
        }

    }

    // 네이버 MAP SDK Init
    fun initNaverMap(key: String) {
        NaverMapSdk.getInstance(this).client = NaverMapSdk.NaverCloudPlatformClient(key)
        val fm = supportFragmentManager
        val mapFragment = fm.findFragmentById(R.id.map_fragment) as MapFragment? ?: MapFragment.newInstance()
            .also { fm.beginTransaction().add(R.id.map_fragment, it).commit() }
        mapFragment.getMapAsync(this)

        prefs = PreferenceUtil(mContext)

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

        // 기존 데이터가 존재할시에 마커 및 메모 표현
        logUtil.d("delArr Size-> ${MainAdapter(mContext, dataList, this).fragment.delArr.size}")
        db.dao().getAll().observe(this) { values ->
            (JsonParser.parseString(gson.toJson(values)) as JsonArray).forEach { element ->
                logUtil.d(element.toString())
                dataList.add(Data((element as JsonObject).get("seq").asString.toInt(), element.get("addr").asString, element.get("lonlat").asString, element.get("memo").asString))
                setMarker(element.get("lonlat").asString.split(",")[0].toDouble(), element.get("lonlat").asString.split(",")[1].toDouble(), element.get("memo").asString)
            }
        }

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
        return JsonParser().parse(resultStr).asJsonObject
    }

    // 현재 위경도 가져오기
    fun onMapGetXY(lon: Double, lat: Double) {
        runOnUiThread {
            HttpUtil.getInstance(this).callerUrlInfo(
                "$VWORLD_GEOCODER_ADDR_API_URL&point=$lon,$lat&format=json&type=parcel&zipcode=true&simple=false&key=$VWORLD_LICENSE",
                object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        println(e.toString())
                    }

                    override fun onResponse(call: Call, response: Response) {
                        var resultData = getResultToGSON(response.body!!.string())
                        var status = resultData.get("response").asJsonObject.get("status").asString

                        if (status != "NOT_FOUND") {
                            //var point = resultData.get("response").asJsonObject.get("input").asJsonObject.get("point").asJsonObject
                            var addr =
                                (resultData.get("response").asJsonObject.get("result").asJsonArray.get(0).asJsonObject.get(
                                    "text"
                                ).asString)
                            runOnUiThread { ->
                                //if (status != "NOT_FOUND") { txtAddr.text = "지번주소: $addr" } else Toast.makeText(mContext, "잘못된 결과값", Toast.LENGTH_SHORT).show()
                            }
                        }

                    }
                })
        }
    }

    //마커 객체 생성
    fun setMarker(lat: Double, lon: Double, memo: String) {
        infoWindow = InfoWindow()
        infoWindow.position = LatLng(lat, lon)
        infoWindow.adapter = object : InfoWindow.DefaultTextAdapter(mContext) {
            override fun getText(infoWindow: InfoWindow): CharSequence {
                return memo
            }
        }

        val markerIcon = OverlayImage.fromResource(R.drawable.ic_location)
        marker = Marker()
        marker.tag = memo
        marker.icon = markerIcon
        marker.position = LatLng(lat, lon)

        infoWindow.position = LatLng(lat, lon)
        infoWindow.anchor = PointF(1f, 1f)
        infoWindow.offsetX = 60
        infoWindow.offsetY = 80
        infoWindow.open(naverMap)

        infoWindowArr.add(infoWindow)
        markersArr.add(marker)
        logUtil.i("Map Marker Size -> ${markersArr.size}")

        markersArr.forEach { marker -> marker.map = naverMap } // Array에 따라 Marker 생성
    }

    // 이동경로 표시
    @RequiresApi(Build.VERSION_CODES.M)
    fun addLineOverLay(map: NaverMap, data:MutableList<Data>) {

        var getLonLat = ""
        var waypoint = ""

        if(data.size > 2){
            // 초기화
            distanceArr = mutableListOf<String>() as ArrayList<String>
            waypointLatLngArr = mutableListOf<LatLng>() as ArrayList<LatLng>

            waypointArr.forEach { line -> line.map = null }

            logUtil.d("distanceArrSize -> ${distanceArr.size}")

            data.forEach { el ->
                getLonLat = "${el.lonlat.toString().split(",")[1].trim()},${el.lonlat.toString().split(",")[0].trim()}"
                distanceArr.add(getLonLat)
            }

            logUtil.d("allPath -> $distanceArr")
            logUtil.d("start -> ${distanceArr[0]}")
            logUtil.d("end -> ${distanceArr[distanceArr.size-1]}")

            for(index in 0 until distanceArr.size) {
                // 처음과 마지막 배열은 제외 (출발지, 목적지)
                if(index != 0 && index != distanceArr.size-1){
                    waypoint += "${distanceArr[index]}|"
                }
            }

            waypoint = waypoint.substring(0, waypoint.length-1)
            logUtil.d("waypoint -> $waypoint")

            HttpUtil.getInstance(this).callerUrlInfo(
                "https://naveropenapi.apigw.ntruss.com/map-direction-15/v1/driving?start=${distanceArr[0]}&waypoints=$waypoint&goal=${distanceArr[distanceArr.size-1]}&option=tracomfort",
                //" https://naveropenapi.apigw.ntruss.com/map-direction-15/v1/driving?start=127.1280075,37.4478666&goal=127.1302862,37.43286&waypoints=127.1705305,37.4475991:127.1419877,37.4341479&option=tracomfort",
                object : Callback {
                    override fun onFailure(call: Call, e: IOException) { logUtil.e(e.toString()) }
                    override fun onResponse(call: Call, response: Response) {

                        waypointLatLngArr = mutableListOf<LatLng>() as ArrayList<LatLng>

                        val resultRoot = getResultToGSON(response.body!!.string())
                        logUtil.d(resultRoot.toString())

                        if(resultRoot.get("code").asString == "0") {
                            val trafast = resultRoot.get("route").asJsonObject.get("tracomfort").asJsonArray.get(0)
                            val totalDistance = ((trafast as JsonObject).get("summary") as JsonObject).get("distance").asString.toInt() * 0.001
                            var duration = (trafast.get("summary") as JsonObject).get("duration").asString.toInt()
                            duration = ((duration / (1000 * 60)) % 60) // 밀리세컨드를 분으로

                            val rootPath = trafast.asJsonObject.get("path").asJsonArray // 경로
                            val selection = trafast.asJsonObject.get("section").asJsonArray // 섹션
                            val guildLine = trafast.asJsonObject.get("guide").asJsonArray // 가이드라인
                            var rootLonLat: String

                            for (i in 0 until rootPath.size()) {
                                rootLonLat = rootPath[i].toString().replace("[", "").replace("]", "")
                                waypointLatLngArr.add(LatLng(rootLonLat.split(",")[1].toDouble(), rootLonLat.split(",")[0].toDouble()))
                            }

                            for(i in 0 until guildLine.size()){
                                logUtil.i("가이드라인: ${getResultToGSON(guildLine[i].toString())}")

                                var guildeMsg = getResultToGSON(guildLine[i].toString()).get("instructions").asString
                                var guildeDistance = getResultToGSON(guildLine[i].toString()).get("distance").asString
                                var guideDuration = (getResultToGSON(guildLine[i].toString()).get("duration").asString.toInt() % 3600) /60
                                guildeDistance = if (guildeDistance.toString().length > 3) { String.format("%.2f", (guildeDistance.toInt() * 0.001)) + "km" } else { guildeDistance.toString() + "m" }

                                sumGuidenMsg += "상세안내:${guildeMsg}  거리:$guildeDistance \n"
                            }

                            logUtil.d("detailGuideLine -> $sumGuidenMsg")

                            runOnUiThread {
                                //pathOverLay.map = null
                                pathOverLay = PathOverlay()
                                pathOverLay.color = getColor(R.color.royal_blue)
                                pathOverLay.patternImage = OverlayImage.fromResource(R.drawable.path_pattern)
                                pathOverLay.width = 10
                                pathOverLay.coords = waypointLatLngArr
                                waypointArr.add(pathOverLay)
                                pathOverLay.map = map
                                pathOverLay.coords = waypointLatLngArr
                            }
                        }
                    }
                })
        } else {
            Toast.makeText(mContext, "최소 경유지 포함 3개 이상 입력시 경로 출력", Toast.LENGTH_SHORT).show()
        }
    }

    // 객체 초기화
    fun objectClear() {
        logUtil.d("마커 갯수 -> ${markersArr.size}")
        //if (markersArr.size > 0) {

           /* GlobalScope.launch(Dispatchers.IO) {
                delay(1000L)
                db.dao().deleteAll()
                logUtil.d("내부 DB QUERY -> delete()")
            }*/

            sumGuidenMsg = ""
            markersArr.forEach { marker -> marker.map = null }
            infoWindowArr.forEach { info -> info.map = null }
            waypointArr.forEach { line -> line.map = null }

            waypointArr = mutableListOf<Overlay>() as ArrayList<Overlay>
            waypointLatLngArr = mutableListOf<LatLng>() as ArrayList<LatLng>
            distanceArr = mutableListOf<String>() as ArrayList<String>
            markersArr = mutableListOf<Overlay>() as ArrayList<Overlay>
            infoWindowArr = mutableListOf<Overlay>() as ArrayList<Overlay>

       //} else {
            //Toast.makeText(mContext, "목적지 및 경로선이 존재하지 않습니다.", Toast.LENGTH_SHORT).show()
        //}
    }

    // 카메라 이동
    fun cameraUpdate() =
        CameraUpdate.toCameraPosition(CameraPosition(LatLng(lat, lon), this.naverMap.cameraPosition.zoom))
            .animate(CameraAnimation.Easing)

    companion object {

        private const val NAVER_LICENSE = "ywgp3sltx8"
        private const val VWORLD_LICENSE = "615D799D-2BD6-3342-B58B-123B6D62BB91"
        private const val VWORLD_GEOCODER_ADDR_API_URL = "http://api.vworld.kr/req/address?service=address&request=getAddress&version=2.0&crs=epsg:4326&" // 좌표를 주소로 변환

        private const val PERMISSION_REQUEST_CODE = 1000
        private val PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        private val TAG: String? = MainActivity::class.simpleName
    }

    override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) {
        TODO("Not yet implemented")
    }

}