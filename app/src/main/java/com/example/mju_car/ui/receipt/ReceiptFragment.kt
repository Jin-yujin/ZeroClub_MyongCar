package com.example.mju_car.ui.receipt

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.example.mju_car.ChatActivity
import com.example.mju_car.R
import com.example.mju_car.comp.ListViewAdapter
import com.example.mju_car.data.FirebaseUtil
import com.example.mju_car.data.vo.RoomVO
import com.example.mju_car.databinding.FragmentReceiptBinding
import com.example.mju_car.model.ListViewItem
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.Date
import java.util.Locale

class ReceiptFragment : Fragment(), FirebaseUtil.DataCallback {

    private var _binding: FragmentReceiptBinding? = null
    private val binding get() = _binding!!
    private lateinit var currentItems: MutableList<ListViewItem>
    private lateinit var pastItems: MutableList<ListViewItem>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReceiptBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // 파이어베이스에서 데이터 가져오기
        FirebaseUtil.fetchRooms(this)

        // 뒤로가기 버튼 처리
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    requireActivity().supportFragmentManager.popBackStack()
                }
            }
        )
        return root
    }

    override fun onDataReceived(rooms: List<RoomVO>) {
        if (!::currentItems.isInitialized) {
            currentItems = mutableListOf()
        } else {
            currentItems.clear()
        }

        if (!::pastItems.isInitialized) {
            pastItems = mutableListOf()
        } else {
            pastItems.clear()
        }

        setListViews(rooms)
    }

    override fun onError(exception: Exception) {
        Log.w("ReceiptFragment", "Error getting documents: $exception")
    }

    override fun onResume() {
        super.onResume()
        // 데이터 다시 로드
        FirebaseUtil.fetchRooms(this)
    }

    //이용내역 설정
    private fun setListViews(rooms: List<RoomVO>) {
        if (binding == null) {
            throw IllegalStateException("Binding is null")
        }

        currentItems = mutableListOf()
        pastItems = mutableListOf()

        val currentUser = Firebase.auth.currentUser
        val currentUserUid = currentUser?.uid ?: ""

        rooms.forEach { room ->
            //현재 사용자가 참여하고 있는 룸 확인
            val isParticipated = room.participants.any { it.addedByUser == currentUserUid }

            //DB에서 룸정보 가져오기
            if (isParticipated) {
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
                    idOwner = room.idOwner,
                    departureTimeStamp = room.departureTimeStamp
                )

                val now = System.currentTimeMillis()
                val oneDayAgo = now - (24 * 60 * 60 * 1000) // 24시간 전 타임스탬프

                if (room.departureTimeStamp >= oneDayAgo) {
                    // 현재 이용 내역 (출발 시간이 24시간 이내)
                    currentItems.add(item)
                } else {
                    // 과거 이용 내역 (출발 시간이 24시간 이전)
                    pastItems.add(item)
                }

            }
        }

        // 이용내역 시간순 정렬
        currentItems.sortBy { it.departureTimeStamp }   //오름차순
        pastItems.sortByDescending { it.departureTimeStamp }    //내림차순


        // 어댑터로 내역 전달
        val currentAdapter = ListViewAdapter(currentItems)
        binding.currentParty.adapter = currentAdapter

        val pastAdapter = ListViewAdapter(pastItems)
        binding.pastListView.adapter = pastAdapter


        //과거 이용내역 클릭시
        binding.pastListView.setOnItemClickListener { parent, view, position, id ->
            val item = pastItems[position]
            val selectedRoom = rooms.find { it.identifierID == item.identifierID }

            if (selectedRoom != null) {

                val fragment = UseDetailFragment()
                val bundle = Bundle().apply {
                    putString("from", item.from)
                    putString("to", item.to)
                    putString("dateInfo", item.dateInfo)
                    putString("owner", item.writer)
                    putInt("price", item.price)
                    putInt("time", item.time)
                    putInt("curPartner", item.curPartner)
                    putInt("maxPartner", item.maxPartner)
                    putString("identifierID", item.identifierID)
                }

                fragment.arguments = bundle

                requireActivity()
                    .supportFragmentManager
                    .beginTransaction()
                    .add(R.id.receiptFrameLayout, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        }
        //현재 이용내역 클릭시
        binding.currentParty.setOnItemClickListener { parent, view, position, id ->
            val item = currentItems[position]
            val selectedRoom = rooms.find { it.identifierID == item.identifierID }

            if (selectedRoom != null) {
                val bundle = Bundle().apply {
                    putString("from", item.from)
                    putString("to", item.to)
                    putString("dateInfo", item.dateInfo)
                    putString("owner", item.writer)
                    putInt("price", item.price)
                    putInt("time", item.time)
                    putString("idOwner", item.idOwner)
                    putInt("curPartner", item.curPartner)
                    putInt("maxPartner", item.maxPartner)
                    putString("identifierID", item.identifierID)
                    putLong("departureTimeStamp",item.departureTimeStamp)
                    putString("roomId", "room_${item.identifierID}")
                }

                val intent = Intent(requireContext(), ChatActivity::class.java)
                intent.putExtras(bundle)
                startActivity(intent)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

//타임스탬프 포맷 설정
fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MM월 dd일 HH : mm", Locale.KOREA)
    val date = Date(timestamp)
    return sdf.format(date) + " 출발"
}
