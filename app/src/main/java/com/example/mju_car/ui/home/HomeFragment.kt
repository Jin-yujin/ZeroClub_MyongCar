package com.example.mju_car.ui.home

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.mju_car.R
import com.example.mju_car.comp.ListViewAdapter
import com.example.mju_car.data.vo.RoomVO
import com.example.mju_car.databinding.FragmentHomeBinding
import com.example.mju_car.model.ListViewItem
import com.example.mju_car.ui.receipt.formatTimestamp
import com.example.mju_car.ui.shuttle.TaxiFragment
import com.example.mju_car.ui.search.SearchFragment
import com.example.mju_car.ui.shuttle.BusFragment
import com.example.mju_car.ui.viewall.ViewAllFragment
import com.example.mju_car.ui.viewdetail.ViewDetailFragment
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private lateinit var items: MutableList<ListViewItem>

    private val binding get() = _binding!!
    private lateinit var btnSearch: Button   // 검색 이동 버튼
    private lateinit var btnViewAll: Button  // 리스트 전체보기 버튼
    private var hasReadNotification = false  // 학점제 공지사항 읽었는지 여부 확인
    private var backPressedTime: Long = 0    // 뒤로가기 버튼 클릭 시간 저장
    private val FINISH_INTERVAL_TIME = 2000  // 종료 토스트 메시지 (2초)
    private var isHomeFragment = true        // 홈 프래그먼트 여부 확인 변수

    // 프래그먼트의 UI를 생성하는 메서드
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // 뷰 바인딩 초기화
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // 검색 이동 이벤트 처리
        btnSearch = _binding!!.btnSearch
        btnSearch.setOnClickListener {
            val searchFragment = SearchFragment()
            requireActivity()
                .supportFragmentManager
                .beginTransaction()
                .add(R.id.lyFrameLayout, searchFragment)
                .addToBackStack(null)
                .commit()
            isHomeFragment = false
        }

        //리스트뷰 설정
        fetchAllData()

        // 리스트 전체보기 이동 이벤트 처리
        btnViewAll = _binding!!.btnViewAll
        btnViewAll.setOnClickListener {
            val viewAllFragment = ViewAllFragment()
            requireActivity()
                .supportFragmentManager
                .beginTransaction()
                .add(R.id.lyFrameLayout, viewAllFragment)
                .addToBackStack(null)
                .commit()
            isHomeFragment = false
        }

        // taxi 정류장 사진 클릭시
        binding.taxiStop.setOnClickListener {
            val taxiFragment = TaxiFragment()
            val transaction = requireActivity().supportFragmentManager.beginTransaction()
            transaction.replace(R.id.lyFrameLayout, taxiFragment)
            transaction.addToBackStack(null) // 뒤로 가기 스택에 추가
            transaction.commit()
            isHomeFragment = false
        }

        // Bus 정류장 사진 클릭시
        binding.busStop.setOnClickListener {
            val busFragment = BusFragment()
            val transaction = requireActivity().supportFragmentManager.beginTransaction()
            transaction.replace(R.id.lyFrameLayout, busFragment)
            transaction.addToBackStack(null) // 뒤로 가기 스택에 추가
            transaction.commit()
            isHomeFragment = false
        }

        // 학점제 공지사항 알림창 버튼 클릭시
        binding.notificationIcon.setOnClickListener {
            showNotificationDialog()
        }

        // 뒤로가기 버튼 처리
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (requireActivity().supportFragmentManager.backStackEntryCount > 0) {
                        requireActivity().supportFragmentManager.popBackStack()
                        isHomeFragment = true
                    } else {
                        handleBackPressed()
                    }
                }
            })
        // 학점제 공지사항 페이지 방문 여부
        updateNotificationBadge()

        return root
    }

    // 프래그먼트의 뷰가 생성된 후 호출되는 메서드
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateScore() // 홈 화면이 표시될 때 점수를 업데이트

        // 뒤로가기 버튼 처리
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    handleBackPressed()
                }
            }
        )
    }

    // 뒤로가기 버튼 처리
    private fun handleBackPressed() {
        if (isHomeFragment) {
            val tempTime = System.currentTimeMillis()
            val intervalTime = tempTime - backPressedTime

            if (intervalTime in 0..FINISH_INTERVAL_TIME) {
                activity?.finishAffinity()
            } else {
                backPressedTime = tempTime
                Toast.makeText(requireContext(), "'뒤로' 버튼을 한번 더 누르시면 종료됩니다.", Toast.LENGTH_SHORT)
                    .show()
            }
        } else {
            isHomeFragment = true
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    // 사용자의 학점을 업데이트하는 함수
    private fun updateScore() {
        lifecycleScope.launch {
            val currentUser = Firebase.auth.currentUser
            if (currentUser != null) {
                try {
                    val userDocRef =
                        Firebase.firestore.collection("USERS").document(currentUser.uid)
                    userDocRef.get()
                        .addOnSuccessListener { documentSnapshot ->
                            if (documentSnapshot.exists()) {
                                val currentReportCount =
                                    documentSnapshot.getLong("reportCount")?.toInt() ?: 0

                                val newScore = when {
                                    currentReportCount == 0 -> "A+"
                                    currentReportCount == 1 -> "B+"
                                    currentReportCount == 2 -> "C+"
                                    currentReportCount == 3 -> "C+"
                                    currentReportCount >= 4 -> "F"
                                    else -> "A"
                                }

                                val userName = documentSnapshot.getString("nickName") ?: ""

                                userDocRef.update("score", newScore).addOnSuccessListener {
                                    val participatedRoomIds =
                                        documentSnapshot.get("participatedRoomIds") as? List<String>
                                            ?: emptyList()
                                    if (participatedRoomIds != null) {
                                        updateRoomParticipantsAndOwners(
                                            participatedRoomIds,
                                            currentUser.uid,
                                            newScore,
                                            userName
                                        )
                                    }
                                }
                            }
                        }
                } catch (e: Exception) {
                    Log.e("RegisterActivity", "Failed to update FCM token", e)
                }
            }
        }
    }

    // 방의 참가자와 소유자의 정보를 업데이트하는 함수
    private fun updateRoomParticipantsAndOwners(
        roomIds: List<String>,
        userId: String,
        newScore: String,
        userName: String
    ) {
        for (roomId in roomIds) {
            val roomDocRef = Firebase.firestore.collection("ROOMS").document(roomId)

            roomDocRef.get().addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val room = documentSnapshot.toObject(RoomVO::class.java)
                    room?.let {
                        var updated = false

                        for (participant in room.participants) {
                            if (participant.addedByUser == userId) {
                                participant.nameScore = "[$newScore] $userName"
                                updated = true
                            }
                        }

                        if (room.idOwner == userId) {
                            room.owner = "[$newScore] $userName"
                            updated = true
                        }

                        if (updated) {
                            roomDocRef.set(room)
                                .addOnSuccessListener {
                                    Log.d("RegisterActivity", "Room $roomId updated successfully")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("RegisterActivity", "Failed to update room $roomId", e)
                                }
                        }
                    }
                }
            }
        }
    }

    // 학점제 공지사항 다이얼로그 표시
    private fun showNotificationDialog() {
        val builder = AlertDialog.Builder(requireContext())

        val title = TextView(requireContext())
        title.text = HtmlCompat.fromHtml("<center>★필독★</center>", HtmlCompat.FROM_HTML_MODE_LEGACY)
        title.gravity = Gravity.CENTER
        title.textSize = 20f // 타이틀 크기 설정
        title.setPadding(0, 20, 0, 20) // 상하 패딩 조정
        builder.setCustomTitle(title)

        builder.setMessage(
            "*학점에 따른 패널티 안내*\n" +
                    "누적 신고 0회: A+\n" +
                    "누적 신고 1회: B+\n" +
                    "누적 신고 2회: C+\n" +
                    "누적 신고 3-4회: F 학사경고(한 달 이용 제한)\n" +
                    "누적 신고 5회: 제적(영구 이용 제한)\n\n" +

                    "**미납으로 인해 신고를 받을 경우,\n3일 내로 정산하고 고객센터로 연락을 주시면\n신고 철회해드립니다. \n(다만, 이런 경우가 잦은 경우 \n철회할 수 없을 수 있습니다.)" +
                    "\n\n***이용중인 방 목록은 출발시각 기준 \n24시간 이후 과거내역으로 이동합니다. \n회원탈퇴 시 참고 부탁드립니다."
        )
        builder.setPositiveButton("확인") { dialog, _ ->
            hasReadNotification = true
            updateNotificationBadge()
            dialog.dismiss()
        }
        builder.show()
    }

    // 공지사항 읽음 여부에 따른 배지 업데이트
    private fun updateNotificationBadge() {
        binding.notificationBadge.visibility = if (hasReadNotification) View.GONE else View.VISIBLE
    }

    // 프래그먼트 뷰가 파괴될 때 호출되는 메서드
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // 데이터베이스에서 모든 방 데이터를 가져오는 함수
    private fun fetchAllData() {
        val db = Firebase.firestore

        db.collection("ROOMS")
            .get()
            .addOnSuccessListener { result ->
                val rooms = mutableListOf<RoomVO>()
                for (document in result) {
                    val room = document.toObject(RoomVO::class.java)
                    rooms.add(room)
                }
                setListViews(rooms)
            }
            .addOnFailureListener { exception ->
                Log.w("ViewAllFragment", "Error getting documents: $exception")
            }
    }

    // 가져온 데이터를 리스트뷰에 설정하는 함수
    private fun setListViews(rooms: List<RoomVO>) {
        items = mutableListOf()

        // 출발 시간 기준으로 정렬
        val sortedRooms = rooms.sortedBy { it.departureTimeStamp }
        var itemCount = 0
        val maxItems = 3

        sortedRooms.forEach { room ->
            if (itemCount >= maxItems) return@forEach

            val item = ListViewItem(
                from = room.departureLocation,
                to = room.destinationLocation,
                dateInfo = formatTimestamp(room.departureTimeStamp),
                writer = room.owner,
                price = room.priceTake,
                time = room.timeTake,
                curPartner = room.currentNumberOfPeople,
                maxPartner = room.desiredNumberOfPeople,
                identifierID = room.identifierID,
                departureTimeStamp = room.departureTimeStamp,
                idOwner = room.idOwner
            )

            // 현재 인원 수가 최대 인원 수 이하이고 출발 시간이 현재 시간 이후인 방만 추가
            if (room.currentNumberOfPeople <= room.desiredNumberOfPeople) {
                if (room.departureTimeStamp >= System.currentTimeMillis()) {
                    items.add(item)
                    itemCount++
                }
            }
        }

        // 어댑터 설정
        val adapter = ListViewAdapter(items)

        // binding이 null인지 확인
        if (_binding != null) {
            val mListView: ListView = binding.mListView
            mListView.adapter = adapter

            // ListView 항목 클릭 시 상세로 이동
            mListView.setOnItemClickListener { parent, view, position, id ->
                // 선택된 데이터
                val item = items[position]

                // 대상 Fragment 지정
                val fragment = ViewDetailFragment()

                // 전달 데이터 생성
                val bundle = Bundle()
                bundle.putString("from", item.from)
                bundle.putString("to", item.to)
                bundle.putString("dateInfo", item.dateInfo)
                bundle.putString("owner", item.writer)
                bundle.putInt("price", item.price)
                bundle.putInt("curPartner", item.curPartner)
                bundle.putInt("maxPartner", item.maxPartner)
                bundle.putString("identifierID", item.identifierID)
                bundle.putLong("departureTimeStamp", item.departureTimeStamp)
                fragment.arguments = bundle

                // Fragment 전달
                requireActivity()
                    .supportFragmentManager
                    .beginTransaction()
                    .add(R.id.lyFrameLayout, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        }
    }
}