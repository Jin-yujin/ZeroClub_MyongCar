package com.example.mju_car.ui.search

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ListView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.mju_car.R
import com.example.mju_car.comp.ListViewAdapter
import com.example.mju_car.data.vo.RoomVO
import com.example.mju_car.databinding.FragmentSearchBinding
import com.example.mju_car.model.ListViewItem
import com.example.mju_car.ui.home.HomeFragment
import com.example.mju_car.ui.receipt.formatTimestamp
import com.example.mju_car.ui.viewdetail.ViewDetailFragment
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class SearchFragment : Fragment() {
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private lateinit var items: MutableList<ListViewItem>
    private var allRooms: List<RoomVO> = listOf()
    private var startDes = Pair(0, "")
    private var endDes = Pair(0, "")
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        val root: View = binding.root

        swipeRefreshLayout = binding.swipeRefreshLayout

        // Location 목록 Resource에서 조회
        val start_locations = resources.getStringArray(R.array.start_locations)
        val dest_locations = resources.getStringArray(R.array.dest_locations)

        // 출발지 Spinner 생성
        val stSpinner: Spinner = binding.spinnerStart
        val stAdapter: ArrayAdapter<String> =
            ArrayAdapter(this.requireContext(), android.R.layout.simple_list_item_1, start_locations)
        stSpinner.adapter = stAdapter
        stSpinner.setSelection(0)

        // 도착지 Spinner 생성
        val edSpinner: Spinner = binding.spinnerEnd
        val edAdapter: ArrayAdapter<String> =
            ArrayAdapter(this.requireContext(), android.R.layout.simple_list_item_1, dest_locations)
        edSpinner.adapter = edAdapter
        edSpinner.setSelection(0)

        // 출발지 Spinner 아이템 선택 리스너
        binding.spinnerStart.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                startDes = Pair(position, parent?.getItemAtPosition(position).toString())
                filterList(startDes.second, endDes.second)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // 도착지 Spinner 아이템 선택 리스너
        binding.spinnerEnd.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                endDes = Pair(position, parent?.getItemAtPosition(position).toString())
                filterList(startDes.second, endDes.second)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Refresh 버튼 처리
        val btnRefresh: ImageButton = binding.btnRefresh

        filterList(stSpinner.selectedItem.toString(), edSpinner.selectedItem.toString())

        // 파이어스토어에서 데이터 읽어오기
        fetchRooms()

        // Refresh 버튼 클릭 리스너 설정
        btnRefresh.setOnClickListener {
            stSpinner.setSelection(0)
            edSpinner.setSelection(0)
            setListViews(allRooms)
        }

        // Swipe-to-refresh 처리
        swipeRefreshLayout.setOnRefreshListener {
            fetchRooms() // 방 데이터 새로고침
        }

        // 뒤로가기 버튼 처리
        requireActivity().onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val fragment = HomeFragment()
                requireActivity()
                    .supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.lyFrameLayout, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        })

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val listView: ListView = binding.mListView
        listView.setOnScrollListener(object : AbsListView.OnScrollListener {
            override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {
                if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                    if (!listView.canScrollVertically(-1)) {
                        // ListView의 첫 번째 항목이 완전히 보이는 경우
                        swipeRefreshLayout.isEnabled = true
                    } else {
                        swipeRefreshLayout.isEnabled = false
                    }
                }
            }

            override fun onScroll(
                view: AbsListView,
                firstVisibleItem: Int,
                visibleItemCount: Int,
                totalItemCount: Int
            ) {
                // 필요한 때 구현
            }
        })
    }

    // 파이어스토어에서 방 데이터를 가져오는 함수
    private fun fetchRooms() {
        val db = Firebase.firestore
        db.collection("ROOMS")
            .get()
            .addOnSuccessListener { result ->
                val rooms = mutableListOf<RoomVO>()
                for (document in result) {
                    val room = document.toObject(RoomVO::class.java)
                    rooms.add(room)
                }
                allRooms = rooms
                setListViews(allRooms) //불러온 방 정보로 리스트 뷰에 표시
                swipeRefreshLayout.isRefreshing = false // 데이터 로드 완료 후 새로고침 아이콘 숨기기
            }
            .addOnFailureListener { exception ->
                Log.w("SearchFragment", "Error getting documents: $exception")
                swipeRefreshLayout.isRefreshing = false // 에러 발생 시에도 새로고침 아이콘 숨기기
            }
    }

    // 방 데이터를 리스트뷰에 표시하는 함수
    private fun setListViews(rooms: List<RoomVO>) {
        items = mutableListOf()

        val sortedRooms = rooms.sortedBy { it.departureTimeStamp }

        // room에서 가져온 ListViewItem으로 데이터 각각 삽입
        sortedRooms.forEach { room ->
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

            if (room.departureTimeStamp >= System.currentTimeMillis()) {
                // 현재 시간 이후의 방만 추가
                items.add(item)
            }
        }

        val adapter = ListViewAdapter(items)

        val mListView: ListView = binding.mListView
        mListView.adapter = adapter

        // ListView 항목 클릭 시 상세로 이동
        mListView.setOnItemClickListener { parent, view, position, id ->
            // 선택된 데이터
            val item = items[position]

            // 대상 Fragment 지정
            val fragment = ViewDetailFragment()

            // curPartner >= maxPartner -> 참여 불가
            if (item.curPartner >= item.maxPartner) {
                Toast.makeText(requireContext(), "모집 인원이 마감되어 참여할 수 없습니다.", Toast.LENGTH_SHORT)
                    .show()
            } else {

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

    // allRooms 리스트에서 출발지와 도착지 조건에 맞는 방 정보만 필터링
    private fun filterList(startLocation: String, endLocation: String) {
        val filteredRooms = allRooms.filter { room ->
            (startLocation == "출발지" || room.departureLocation == startLocation) &&
                    (endLocation == "목적지" || room.destinationLocation == endLocation)
        }
        setListViews(filteredRooms)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
