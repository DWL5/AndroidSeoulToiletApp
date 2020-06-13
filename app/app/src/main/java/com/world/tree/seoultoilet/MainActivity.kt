package com.world.tree.seoultoilet

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.location.LocationManager
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.ClusterManager
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.layout_search_bar.*
import kotlinx.android.synthetic.main.layout_search_bar.view.*
import kotlinx.android.synthetic.main.layout_search_bar.view.imageView
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.util.jar.Manifest

class MainActivity : AppCompatActivity() {
    //런타임 에서 권한이 필요한 퍼미션 목록
    val PERMISSION = arrayOf(
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )

    //퍼이션 승인 요청 시 사용하는 코드
    val REQUEST_PERMISSION_CODE = 1

    //기본 맵 줌 레벨
    val DEFAULT_ZOOM_LEVEL = 17f

    // 현재위치를 가져올수 없는 경우 서울 시청의 위치로 지도를 보여주기 위해 서울시청의 위치를 변수로 선언
    // latLng 클래스는 위도와 경도를 가지는 클래스
    val CITY_HALL = LatLng(37.5662952, 126.97794509999994)

    var googleMap: GoogleMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //맵뷰에 onCreate 함수 호출
        mapView.onCreate(savedInstanceState)

        // 앱이 실행될 때 런타임에서 위치 서비스 관련 권한 체크
        if (hasPermission()) {
            initMap()
        } else {
            ActivityCompat.requestPermissions(this, PERMISSION, REQUEST_PERMISSION_CODE)
        }

        myLocationButton.setOnClickListener {
            onMyLocationButtonClick()
        }
    }


    // 앱에서 사용하는 권한이 있는지 체크하는 함수
    fun hasPermission(): Boolean {
        //퍼미션목록 중 하나라도 권한이 없으면 false 반환
        for (permission in PERMISSION) {
            if (ActivityCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

    var clusterManager: ClusterManager<MyItem>? = null

    var clusterRenderer: ClusterRenderer? = null


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        initMap()
    }

    @SuppressLint("MissingPermission")
    fun initMap() {
        // 맵뷰에서 구글 맵을 불러오는 함수. 컬백함수에서 구글 맵 객체가 전달됨
        mapView.getMapAsync {

            clusterManager = ClusterManager(this, it)
            clusterRenderer = ClusterRenderer(this, it, clusterManager)

            it.setOnCameraIdleListener(clusterManager)
            it.setOnMarkerClickListener(clusterManager)

            //구글맵 멤버 변수에 구글맵 객체 저장
            googleMap = it

            // 현재위치로 이동 버튼 비활성화
            it.uiSettings.isMyLocationButtonEnabled = false

            // 위치 사용 권한이 있는 경우
            when {
                hasPermission() -> {
                    // 현재위치 표시 활성화
                    it.isMyLocationEnabled = true

                    // 현재위치로 카메라 이동
                    it.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            getMyLocation(),
                            DEFAULT_ZOOM_LEVEL
                        )
                    )
                }

                else -> {
                    // 권한이 없으면 서울시청의 위치로 이동
                    it.moveCamera(CameraUpdateFactory.newLatLngZoom(CITY_HALL, DEFAULT_ZOOM_LEVEL))
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun getMyLocation(): LatLng {
        // 위치를 측정하는 프로바이더를 GPS 센서로 지정
        val locationProvider: String = LocationManager.GPS_PROVIDER

        // 위치 서비스 객체를 불러옴
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // 마지막으로 업데이트된 위치를 가져옴
        val lastKnownLocation: Location = locationManager.getLastKnownLocation(locationProvider)

        // 위도 경도 객체로 변환
        return LatLng(lastKnownLocation.latitude, lastKnownLocation.longitude)
    }

    fun onMyLocationButtonClick() {
        when {
            hasPermission() -> googleMap?.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    getMyLocation(),
                    DEFAULT_ZOOM_LEVEL
                )
            )
            else -> Toast.makeText(applicationContext, "위치사용권한 설정에 동의해주세요", Toast.LENGTH_LONG)
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    val API_KEY = "776878675373736f35376b4e514168"

    //앱이 비활성화될때 백그라운드 작업도 취소하기 위한 변수 선언
    var task: ToiletReadTask? = null

    // 서울시 화장실 정보 집합을 저장할 Array 변수, 검색을 위해 저장
    var toilets = JSONArray()


    val itemMap = mutableMapOf<JSONObject, MyItem>()

    val bitmap by lazy {
        val drawable = resources.getDrawable(R.drawable.restroom_sign) as BitmapDrawable
        Bitmap.createScaledBitmap(drawable.bitmap, 64, 64, false)
    }

    fun JSONArray.merge(anotherArray: JSONArray) {
        for (i in 0 until anotherArray.length()) {
            this.put(anotherArray.get(i))
        }
    }

    fun JSONArray.findByChildProperty(propertyName: String, value: String): JSONObject? {
        for (i in 0 until length()) {
            val obj = getJSONObject(i)
            if (value == obj.getString(propertyName)) return obj
        }

        return null
    }

    fun readData(startIndex: Int, lastIndex: Int): JSONObject {
        val url = URL(
            "http://openAPI.seoul.go.kr:8088" + "/${API_KEY}/json/SearchPublicToiletPOIService/${
            startIndex}/${lastIndex}"
        )
        val connection = url.openConnection()
        val data = connection.getInputStream().readBytes().toString(charset("UTF-8"))
        return JSONObject(data)
    }

    // 화장실 데이터를 읽어오는 AsyncTask
    inner class ToiletReadTask : AsyncTask<Void, JSONArray, String>() {
        override fun onProgressUpdate(vararg values: JSONArray?) {

            val array = values[0]
            array?.let {
                for (i in 0 until array.length()) {
                    addMarkers(array.getJSONObject(i))
                }
            }
            clusterManager?.cluster()
        }

        override fun onPostExecute(result: String?) {
            // 자동완성 텍스트뷰 에서 사용할 텍스트 리스트
            val textList = mutableListOf<String>()

            //모든 화장실의 이름을 텍스트 리스트에 추가
            for (i in 0 until toilets.length()) {
                val toilet = toilets.getJSONObject(i)
                textList.add(toilet.getString("FNAME"))
            }

            val adapter = ArrayAdapter<String>(
                this@MainActivity,
                android.R.layout.simple_dropdown_item_1line, textList
            )


            //자동완성이 시작되는 글자수 지정
            search_bar.autoCompleteTextView.threshold = 1

            //autoCompleteTextView의 어댑터를 상단에서 만든 어댑터로 지정
            search_bar.autoCompleteTextView.setAdapter(adapter)
        }

        override fun doInBackground(vararg params: Void?): String {
            // 서울시 데이터는 최대 1000개씩 가져올수 있기 때문에
            // STEP 만큼 StartIndex와 lastIndex 값을 변경하며 여러번 호출 해야함

            val step = 1000
            var startIndex = 1

            var lastIndex = step
            var totalCount = 0

            do {
                // 백그라운드 작업이 취소된 경우 루프를 빠져나간다.
                if (isCancelled) break

                // totalCount가 0 이 아닌 경우 최초 실행이 아니므로 step 만큼 startIndex와 lastIndex를 증가
                if (totalCount != 0) {
                    startIndex += step
                    lastIndex += step
                }

                val jsonObject = readData(startIndex, lastIndex)

                totalCount = jsonObject.getJSONObject("SearchPublicToiletPOIService")
                    .getInt("list_total_count")

                val rows =
                    jsonObject.getJSONObject("SearchPublicToiletPOIService").getJSONArray("row")

                toilets.merge(rows)
                publishProgress(rows)
            } while (lastIndex < totalCount)

            return "complete"
        }

        override fun onCancelled(result: String?) {
            super.onCancelled(result)
        }

        override fun onCancelled() {
            super.onCancelled()
        }

        override fun onPreExecute() {
            super.onPreExecute()

            // 구글맵 마커 초기화
            googleMap?.clear()

            // 화장실 정보 초기화
            toilets = JSONArray()

            // itemMap 변수 초기화
            itemMap.clear()
        }
    }


    override fun onStart() {
        super.onStart()
        task?.cancel(true)
        task = ToiletReadTask()
        task?.execute()

        search_bar.autoCompleteTextView.setOnItemClickListener { parent, view, position, id ->
            val title: String = parent.getItemAtPosition(position) as String

            toilets.findByChildProperty("FNAME", title)?.let {
                val myItem = itemMap[it]

                val marker = clusterRenderer?.getMarker(myItem)
                marker?.showInfoWindow()

                googleMap?.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(it.getDouble("Y_WGS84"), it.getDouble("X_WGS84")),
                        DEFAULT_ZOOM_LEVEL
                    )
                )
                clusterManager?.cluster()
            }
            search_bar.autoCompleteTextView.setText("")

        }
        
        search_bar.imageView.setOnClickListener {
            val keyword = search_bar.autoCompleteTextView.text.toString()

            if (TextUtils.isEmpty(keyword)) return@setOnClickListener

            toilets.findByChildProperty("FNAME", keyword)?.let {
                val myItem = itemMap[it]

                val marker = clusterRenderer?.getMarker(myItem)
                marker?.showInfoWindow()

                googleMap?.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(it.getDouble("Y_WGS84"), it.getDouble("X_WGS84")),
                        DEFAULT_ZOOM_LEVEL
                    )
                )
                clusterManager?.cluster()
            }

            search_bar.autoCompleteTextView.setText("")
        }
    }

    override fun onStop() {
        super.onStop()
        task?.cancel(true)
        task = null
    }

    fun addMarkers(toilet: JSONObject) {

        val item = MyItem(
            LatLng(
                toilet.getDouble("Y_WGS84"),
                toilet.getDouble("X_WGS84")
            ),
            toilet.getString("FNAME"),
            toilet.getString("ANAME"),
            BitmapDescriptorFactory.fromBitmap(bitmap)
        )
        clusterManager?.addItem(item)

        itemMap.put(toilet, item)

        /* googleMap?.addMarker(
             MarkerOptions()
                 .position(LatLng(toilet.getDouble("Y_WGS84"),
                     toilet.getDouble("X_WGS84")))
                 .title(toilet.getString("FNAME"))
                 .snippet(toilet.getString("ANAME"))
                 .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
         )*/
    }
}
