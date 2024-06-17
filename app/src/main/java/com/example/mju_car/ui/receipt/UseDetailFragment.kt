package com.example.mju_car.ui.receipt

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.example.mju_car.R
import com.example.mju_car.comp.PartnerAdapter
import com.example.mju_car.data.vo.RoomVO
import com.example.mju_car.databinding.FragmentUsedetailBinding
import com.example.mju_car.model.PartnerItem
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import java.text.DecimalFormat

class UseDetailFragment  : Fragment() {
    private var _binding: FragmentUsedetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: FirebaseFirestore

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentUsedetailBinding.inflate(inflater, container, false)
        val root: View = binding.root

        db = FirebaseFirestore.getInstance()

        // 파라미터 전달 받기
        val from          = arguments?.getString("from")
        val to            = arguments?.getString("to")
        val dateInfo      = arguments?.getString("dateInfo")
        val price         = arguments?.getInt("price")
        val time          = arguments?.getInt("time")
        val curPartner    = (arguments?.getInt("curPartner") ?: -1) - 1
        val identifierID  = arguments?.getString("identifierID")

        // 숫자 포맷
        val df            = DecimalFormat("###,###")

        // UI에 데이터를 설정
        binding.textViewDestination.text   = "$from"
        binding.textViewDeparture.text     = "$to"
        showRoute(from!!, to!!)
        binding.textViewDateValue.text     = "$dateInfo"
        binding.textViewToatalCost.text    = "${df.format(price)} 원"
        binding.tvPartnerCount02.text      = df.format(curPartner)

        // 파트너 정보를 Firestore에서 가져와 ListView에 표시
        val roomRef = db.collection("ROOMS").document(identifierID!!)
        roomRef.get().addOnSuccessListener { roomSnapshot ->
            val roomVO = roomSnapshot.toObject(RoomVO::class.java)
            if (roomVO != null) {
                // 함께 참여한 사람들 데이터 ListView 생성
                val items = mutableListOf<PartnerItem>()

                val currentUser = Firebase.auth.currentUser
                val currentUserUid = currentUser?.uid ?: ""
                val currentUserNickname = currentUser?.displayName ?: "" // 현재 사용자의 닉네임

                for (participant in roomVO.participants) {
                    // 현재 사용자와 동일하지 않은 참가자만 추가
                    if (participant.addedByUser != currentUserUid && participant.nameScore != currentUserNickname) {
                        items.add(
                            PartnerItem(
                                R.drawable.profile_maru.toString(),
                                participant.nameScore,
                                true
                            )
                        )
                    }
                }

                val adapter = PartnerAdapter(items)

                // 파트너 목록
                val pastListView: ListView? = binding.pastListView
                pastListView?.adapter = adapter
                adapter.notifyDataSetChanged()

            } else {
                Toast.makeText(requireContext(), "예약 정보를 가져오지 못했습니다.", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        // 뒤로가기 버튼 처리
        requireActivity().onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val fragment = ReceiptFragment()
                requireActivity()
                    .supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.receiptFrameLayout, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        })
        return root
    }

    //webview에 출발지와 도착지 이동경로를 표시하는 함수
    fun showRoute(from:String, to:String){
        binding.root.post {
            val pastWebView = binding.pastWebView

            pastWebView.isHorizontalScrollBarEnabled = false //가로 스크롤
            pastWebView.isVerticalScrollBarEnabled = false   //세로 스크롤

            // wide viewport를 사용하도록 설정
            pastWebView.getSettings().useWideViewPort = true

            // 컨텐츠가 웹뷰보다 클 경우 스크린 크기에 맞게 조정
            pastWebView.getSettings().loadWithOverviewMode = true

            //zoom 기능 활성화
            pastWebView.getSettings().builtInZoomControls = true;
            pastWebView.getSettings().setSupportZoom(true)

            val width   = pastWebView.measuredWidth    // 너비
            val height  = pastWebView.measuredHeight   // 높이
            val stLoc   = getLatLng(from)           // 출발지 위도 + 경도
            val edLoc   = getLatLng(to)             // 도착지 위도 + 경도

            // 이미지 로드
            val url = "https://simg.pstatic.net/static.map/v2/map/staticmap.bin?w=$width&h=$height&custom_deco=s:path_car|start:$stLoc|destination:$edLoc|respversion:4&caller=search_pathfinding&scale=1&logo=false&maptype=basic&ts=1714232296945"
            Log.d("LOCATION", url)
            pastWebView.loadUrl(url)
        }
    }

    //해당 위치의 위도,경도 반환
    fun getLatLng(location:String):String{
        val p01Lat = 37.274641      // 기흥역 5번 출구(위도)
        val p01Lng = 127.1157028    // 기흥역 5번 출구(경도)
        val p02Lat = 37.2233929     // 명지대학교 학생회관(위도)
        val p02Lng = 127.1872875    // 명지대학교 학생회관(경도)
        val p03Lat = 37.22476       // 명지대학교 정문(위도)
        val p03Lng = 127.1877       // 명지대학교 정문(경도)
        val p04Lat = 37.21927       // 3공학관 (위도)
        val p04Lng = 127.1827       // 3공학관(경도)
        val p05Lat = 37.23830       // 명지대역 1번 출구(위도)
        val p05Lng = 127.1901       // 명지대역 1번 출구(경도)
        val p06Lat = 37.23832       // 명지대역 사거리 (위도)
        val p06Lng = 127.1899       // 명지대역 사거리(경도)

        if(location == "기흥역 5번출구"){
            return "$p01Lng,$p01Lat"
        }else if(location == "명지대 학생회관"){
            return "$p02Lng,$p02Lat"
        }else if(location == "명지대 정문"){
            return "$p03Lng,$p03Lat"
        }else if(location == "명지대 3공학관"){
            return "$p04Lng,$p04Lat"
        }else if(location == "명지대역 1번출구"){
            return "$p05Lng,$p05Lat"
        }else if(location == "명지대 진입로"){
            return "$p06Lng,$p06Lat"
        }
        return ","
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}