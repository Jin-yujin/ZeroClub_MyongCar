package com.example.mju_car.ui.viewdetail

import com.example.mju_car.data.vo.ParticipantVO
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.example.mju_car.ChatActivity
import com.example.mju_car.MyBackgroundService
import com.example.mju_car.R
import com.example.mju_car.comp.PartnerAdapter
import com.example.mju_car.data.vo.RoomVO
import com.example.mju_car.data.vo.UserVO
import com.example.mju_car.databinding.FragmentViewdetailBinding
import com.example.mju_car.model.PartnerItem
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import java.text.DecimalFormat

class ViewDetailFragment : Fragment() {
    private var _binding: FragmentViewdetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: FirebaseFirestore

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentViewdetailBinding.inflate(inflater, container, false)
        val root: View = binding.root
        db = FirebaseFirestore.getInstance()

        // 파라미터 전달 받기
        val from        = arguments?.getString("from")
        val to          = arguments?.getString("to")
        val dateInfo    = arguments?.getString("dateInfo")
        val owner       = arguments?.getString("owner")
        val price       = arguments?.getInt("price")
        val curPartner  = arguments?.getInt("curPartner")
        val maxPartner  = arguments?.getInt("maxPartner")
        val identifierID  = arguments?.getString("identifierID")

        // 숫자 포맷
        val df          = DecimalFormat("###,###")

        // 파라미터 세팅
        binding.tvStart.text    = "출발지 : $from"
        binding.tvEnd.text      = "도착지 : $to"
        showRoute(from!!, to!!)
        binding.tvDateInfo.text = "📆 일정 : $dateInfo"
        binding.tvPrice.text    = "💰 예상금액 : 총 ${df.format(price)} 원"
        binding.tvOwner.text    = owner
        binding.tvPartnerCount.text     = "$curPartner / $maxPartner"
        binding.tvPartnerCount02.text   = df.format(curPartner)

        //선택된 방의 데이터 가져오기
        val roomRef = db.collection("ROOMS").document(identifierID!!)
        roomRef.get().addOnSuccessListener { roomSnapshot ->
            val roomVO = roomSnapshot.toObject(RoomVO::class.java)
            if (roomVO != null) {
                // 함께 참여할 사람들 데이터 ListView 생성
                val items = mutableListOf<PartnerItem>()
                for (participant in roomVO.participants) {
                    items.add(
                        PartnerItem(
                            R.drawable.profile_maru.toString(),
                            participant.nameScore,
                            true
                        )
                    )
                }
                val adapter = PartnerAdapter(items)

                Log.d("item", "$items")

                // 파트너 목록
                val mListView: ListView = binding.mListView
                mListView.adapter = adapter
                adapter.notifyDataSetChanged()
            } else {
                Toast.makeText(requireContext(), "예약 정보를 가져오지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        // 참여하기 버튼 클릭 시
        binding.btnApply.setOnClickListener {
             val currentUser = Firebase.auth.currentUser
            if (currentUser != null) {
                val userDocRef = db.collection("USERS").document(currentUser.uid)
                userDocRef.get().addOnSuccessListener { userDocSnapshot ->

                    val roomRef = db.collection("ROOMS").document(identifierID!!)
                    roomRef.get().addOnSuccessListener { roomSnapshot ->
                        val roomVO = roomSnapshot.toObject(RoomVO::class.java)
                        if (roomVO != null) {
                            val isParticipated = roomVO.participants.any { it.addedByUser == currentUser.uid }
                            if (isParticipated) {
                                Toast.makeText(requireContext(), "현재 참여되어있는 예약입니다!", Toast.LENGTH_SHORT).show()
                            } else {
                                // 참여 확인 대화 상자 생성
                                materialNegativePositiveDialog()
                            }
                        } else {
                            Toast.makeText(requireContext(), "예약 정보를 가져오지 못했습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }.addOnFailureListener {
                        Toast.makeText(requireContext(), "예약 정보를 가져오지 못했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener {
                    Toast.makeText(requireContext(), "사용자 정보를 가져오지 못했습니다.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "사용자가 로그인되지 않았습니다.", Toast.LENGTH_SHORT).show()
            }
        }
        return root
    }

    // 참여 확인 대화 상자 생성
    private fun materialNegativePositiveDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage("카풀 이용 완료 후, \n결제자가 정산을 요청하면\n입금을 해주셔야 합니다.\n입금 미진행시, 패널티가 발생하며\n이용에 제한이 생길 수 있습니다.\n\n참여 하시겠습니까?")
            .setNegativeButton("취소") { dialog, which ->
                // 취소 버튼 클릭 시 처리
            }
            .setPositiveButton("확인") { dialog, which ->
                // 확인 버튼 클릭 시 처리

                Toast.makeText(requireContext(), "참여되었습니다.", Toast.LENGTH_SHORT).show()

                val currentUser = Firebase.auth.currentUser

                if (currentUser != null) {
                    val userDocRef = db.collection("USERS").document(currentUser.uid)
                    userDocRef.get().addOnSuccessListener { userDocSnapshot ->
                        val userVO = userDocSnapshot.toObject(UserVO::class.java)
                        val userName = userVO?.nickName ?: ""
                        val score = userVO?.score

                        val bundle = Bundle().apply {
                            putString("from", arguments?.getString("from"))
                            putString("to", arguments?.getString("to"))
                            putString("dateInfo", arguments?.getString("dateInfo"))
                            putString("owner", arguments?.getString("owner"))
                            putInt("price", arguments?.getInt("price") ?: 0)
                            putInt("curPartner", arguments?.getInt("curPartner") ?: 0)
                            putInt("maxPartner", arguments?.getInt("maxPartner") ?: 0)
                            putString("idOwner",  arguments?.getString("idOwner") )
                            putLong("departureTimeStamp", arguments?.getLong("departureTimeStamp") ?: 0L)
                            putString("identifierID", arguments?.getString("identifierID"))
                            putString("roomId", "room_${arguments?.getString("identifierID")}") // 채팅방 ID 추가
                            putString("userNickName", userName) // 사용자 닉네임 추가
                        }

                        // 채팅 액티비티 시작
                        val intent = Intent(requireContext(), ChatActivity::class.java)
                        intent.putExtras(bundle)
                        startActivity(intent)

                        bundle.putString("userNickName", userName)
                        //게시물 식별코드 전달받기
                        val identifierID  = arguments?.getString("identifierID")

                        val roomRef = db.collection("ROOMS").document(identifierID!!)
                        roomRef.get().addOnSuccessListener { roomSnapshot ->
                            val roomVO = roomSnapshot.toObject(RoomVO::class.java)
                            if (roomVO != null) {
                                // 함께 탄 사람 정보에 참여자 닉네임 추가
                                val updatedParticipants = roomVO.participants.toMutableList().apply {
                                    add(ParticipantVO("[$score] $userName", addedByUser=currentUser.uid))
                                }

                                // RoomVO 업데이트
                                val updatedRoomData = roomVO.copy(
                                    participants = updatedParticipants,
                                    currentNumberOfPeople = roomVO.currentNumberOfPeople + 1
                                )
                                roomRef.set(updatedRoomData)

                                // 사용자의 participatedRoomIds에 게시글 id 추가 후 업데이트
                                val updatedUserData = hashMapOf<String, Any>(
                                    "participatedRoomIds" to FieldValue.arrayUnion(identifierID)
                                )
                                userDocRef.update(updatedUserData)
                            }
                        }

                        // MyBackgroundService 시작
                        val serviceIntent = Intent(requireContext(), MyBackgroundService::class.java)
                        serviceIntent.putExtra("identifierID", identifierID)
                        requireContext().startService(serviceIntent)

                        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
                            override fun handleOnBackPressed() {
                                requireActivity().supportFragmentManager.popBackStack()
                            }
                        })
                    }
                } else {
                    Toast.makeText(requireContext(), "사용자가 로그인되지 않았습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = FirebaseFirestore.getInstance()

        // 뒤로가기 버튼 처리
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val fragmentManager = requireActivity().supportFragmentManager
                if (fragmentManager.backStackEntryCount > 0) {
                    fragmentManager.popBackStack()
                } else {
                    requireActivity().finish()
                }
            }
        })

    }
    // webview에 이동경로 나타내기
    fun showRoute(from:String, to:String){
        binding.root.post {
            val mWebView = binding.mWebView

            mWebView.isHorizontalScrollBarEnabled = false //가로 스크롤
            mWebView.isVerticalScrollBarEnabled = false   //세로 스크롤

            // wide viewport를 사용하도록 설정
            mWebView.getSettings().useWideViewPort = true

            // 컨텐츠가 웹뷰보다 클 경우 스크린 크기에 맞게 조정
            mWebView.getSettings().loadWithOverviewMode = true

            // zoom 허용
            // mWebView.getSettings().setBuiltInZoomControls(true);
            mWebView.getSettings().setSupportZoom(true)

            val width   = mWebView.measuredWidth    // 너비
            val height  = mWebView.measuredHeight   // 높이
            val stLoc   = getLatLng(from)           // 출발지 위도 + 경도
            val edLoc   = getLatLng(to)             // 도착지 위도 + 경도

            // 이미지 로드
            val url = "https://simg.pstatic.net/static.map/v2/map/staticmap.bin?w=$width&h=$height&custom_deco=s:path_car|start:$stLoc|destination:$edLoc|respversion:4&caller=search_pathfinding&scale=1&logo=false&maptype=basic&ts=1714232296945"
            Log.d("LOCATION", url)
            mWebView.loadUrl(url)
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

        when (location) {
            "기흥역 5번출구" -> {
                return "$p01Lng,$p01Lat"
            }
            "명지대 학생회관" -> {
                return "$p02Lng,$p02Lat"
            }
            "명지대 정문" -> {
                return "$p03Lng,$p03Lat"
            }
            "명지대 3공학관" -> {
                return "$p04Lng,$p04Lat"
            }
            "명지대역 1번출구" -> {
                return "$p05Lng,$p05Lat"
            }
            "명지대 진입로" -> {
                return "$p06Lng,$p06Lat"
            }
            else -> return ","
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}