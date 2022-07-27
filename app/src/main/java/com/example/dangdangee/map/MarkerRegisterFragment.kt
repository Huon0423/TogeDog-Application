package com.example.dangdangee.map

import android.content.Intent
import android.graphics.Color
import android.graphics.PointF
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.example.dangdangee.R
import com.example.dangdangee.databinding.FragmentMarkerRegisterBinding
import com.naver.maps.geometry.LatLng
import com.naver.maps.geometry.LatLngBounds
import com.naver.maps.map.*
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.OverlayImage
import com.naver.maps.map.util.FusedLocationSource
import java.io.IOException
import java.util.*

class MarkerRegisterFragment : Fragment(), OnMapReadyCallback {
    private lateinit var mapView: MapView
    private lateinit var naverMap: NaverMap
    private lateinit var locationSource: FusedLocationSource
    private val binding by lazy { FragmentMarkerRegisterBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //현재 위치 사용 처리
        locationSource =
            FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE)

        //맵을 띄울 프래그먼트 설정
        val fm = childFragmentManager
        val mapFragment = fm.findFragmentById(R.id.register_map_view) as MapFragment?
            ?: MapFragment.newInstance().also {
                fm.beginTransaction().add(R.id.register_map_view, it).commit()
            }
        mapFragment.getMapAsync(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_marker_register, container, false)
    }

    //위도 경도 주소로 변환하여 보여줌
    private fun getAddress(lat: Double, lng: Double): String {
        val geoCoder = Geocoder(context, Locale.KOREA)
        val address: ArrayList<Address>
        var addressResult = "주소를 가져 올 수 없습니다."
        try {
            //세번째 파라미터는 좌표에 대해 주소를 리턴 받는 갯수로
            //한좌표에 대해 두개이상의 이름이 존재할수있기에 주소배열을 리턴받기 위해 최대갯수 설정
            address = geoCoder.getFromLocation(lat, lng, 1) as ArrayList<Address>
            if (address.size > 0) {
                // 주소 받아오기
                val currentLocationAddress = address[0].getAddressLine(0)
                    .toString()
                addressResult = currentLocationAddress
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return addressResult
    }

    //맵 레디 콜백
    override fun onMapReady(naverMap: NaverMap) {
        val resultIntent = Intent()
        val tv = binding.registerTvLocation //주소 표시
        val fab = binding.registerFloatingbtn //추가 플로팅 버튼
        this.naverMap = naverMap

        //지도 영역 처리
        naverMap.minZoom = 5.0 //최소 줌
        val northWest = LatLng(31.43, 122.37) //서북단
        val southEast = LatLng(44.35, 132.0) //동남단
        naverMap.extent = LatLngBounds(northWest, southEast) //지도 영역을 국내 위주로 축소

        //현재 위치 사용 처리
        naverMap.locationSource = locationSource
        naverMap.locationTrackingMode = LocationTrackingMode.NoFollow //위치 변해도 지도 안움직임
        naverMap.uiSettings.isLocationButtonEnabled = true //현재 위치 버튼 활성화

        //MainMapActivity에 디폴트로 돌려줄 값
        resultIntent.putExtra("latitude", 0.0) //등록안하면 디폴트로 0.0
        resultIntent.putExtra("longitude", 0.0) //등록안하면 디폴트로 0.0
        //setResult(AppCompatActivity.RESULT_OK, resultIntent) //디폴트

        //이동 중이면 표시, 확인 버튼 비활성화
        naverMap.addOnCameraChangeListener { _, _ ->
            tv.run {
                text = "위치 이동 중"
                setTextColor(Color.parseColor("#c4c4c4"))
            }
            fab.run {
                isVisible = false
            }//이동 중이면 등록 버튼 비활성화
        }

        // 카메라의 움직임 종료에 대한 이벤트 리스너
        // 좌표 -> 주소 변환 텍스트 세팅, 버튼 활성화
        naverMap.addOnCameraIdleListener {
            tv.run {
                text = getAddress(
                    naverMap.cameraPosition.target.latitude,
                    naverMap.cameraPosition.target.longitude
                )
                setTextColor(Color.parseColor("#2d2d2d"))
            }
            fab.run {
                isVisible = true
            }//이동 끝나면 등록 버튼 비활성화
        }



        //등록 버튼 누르면 위도 경도를 MainMapActivity로 전달
        fab.setOnClickListener {
            if (!tv.text.equals("주소를 가져 올 수 없습니다.")) {
                val cameraposition = naverMap.cameraPosition
                resultIntent.putExtra("latitude", cameraposition.target.latitude)
                resultIntent.putExtra("longitude", cameraposition.target.longitude,)
                //setResult(AppCompatActivity.RESULT_OK, resultIntent)
                Marker().apply {
                    position =
                        LatLng(cameraposition.target.latitude, cameraposition.target.longitude)
                    map = naverMap
                    icon = OverlayImage.fromResource(R.drawable.ic_baseline_pets_24) //아이콘
                    width = Marker.SIZE_AUTO //자동 사이즈
                    height = Marker.SIZE_AUTO //자동사이즈
                    anchor = PointF(0.5f, 0.5f)
                    isIconPerspectiveEnabled = true //원근감
                    captionRequestedWidth = 200 //캡션 길이
                    captionMinZoom = 12.0 //캡션 보이는 범위
                } //마커 위치 보기위한 것 추후 삭제
                //finish() //등록 후 finish
            }
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1000
    }
}