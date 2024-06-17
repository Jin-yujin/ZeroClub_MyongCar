package com.example.mju_car.ui.viewall

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.mju_car.R
import com.example.mju_car.comp.DatePickerAdapter
import com.example.mju_car.comp.ListViewAdapter
import com.example.mju_car.data.vo.RoomVO
import com.example.mju_car.databinding.FragmentViewallBinding
import com.example.mju_car.model.ListViewItem
import com.example.mju_car.ui.home.HomeFragment
import com.example.mju_car.ui.receipt.formatTimestamp
import com.example.mju_car.ui.viewdetail.ViewDetailFragment
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

class ViewAllFragment : Fragment() {
    private var _binding: FragmentViewallBinding? = null
    private val binding get() = _binding!!
    private lateinit var edDate: EditText
    private lateinit var items: MutableList<ListViewItem>
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentViewallBinding.inflate(inflater, container, false)
        val root: View = binding.root

        swipeRefreshLayout = binding.swipeRefreshLayout
        edDate = binding.edDate
        edDate.setOnClickListener {
            showDatePickerDialog()  // 날짜 선택 다이얼로그를 표시
        }

        // Location 목록 Resource에서 조회
        val timeList = resources.getStringArray(R.array.times)

        // 시간 Spinner 생성
        val stSpinner: Spinner = binding.spinnerTime
        val stAdapter: ArrayAdapter<String> = ArrayAdapter(this.requireContext(), android.R.layout.simple_list_item_1, timeList)
        stSpinner.adapter = stAdapter
        stSpinner.setSelection(0)

        fetchAllData() // 모든 데이터를 가져옴

        // 시간 Spinner 아이템 선택 리스너 설정
        stSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // 날짜가 설정되어 있다면 fetchAndFilterData()로 데이터 필터링하여 가져옴
                if (edDate.text.isNotEmpty()) {
                    fetchAndFilterData(edDate.text.toString())
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // 뒤로가기 버튼 처리
        requireActivity().onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // 뒤로가기 눌렀을 때 동작할 코드
                val homeFragment = HomeFragment()
                requireActivity()
                    .supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.lyFrameLayout, homeFragment)
                    .addToBackStack(null)
                    .commit()
            }
        })
        // SwipeRefreshLayout 리스너 설정
        swipeRefreshLayout.setOnRefreshListener {
            fetchAllData()  // 모든 데이터를 새로고침
        }
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

    // 모든 방 데이터를 가져오는 함수
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
                setListViews(rooms) // 가져온 방 데이터를 리스트뷰에 표시
                swipeRefreshLayout.isRefreshing = false // 데이터 로드 완료 후 새로고침 아이콘 숨기기
            }
            .addOnFailureListener { exception ->
                Log.w("ViewAllFragment", "Error getting documents: $exception")
                swipeRefreshLayout.isRefreshing = false // 에러 발생 시에도 새로고침 아이콘 숨기기
            }
    }

    //날짜와 시간에 따라 데이터 필터링하여 가져오는 함수
    private fun fetchAndFilterData(selectedDate: String) {
        val db = Firebase.firestore

        // 선택한 날짜를 타임스탬프로 변환
        val dateTimestamp = convertDateToTimestamp(selectedDate)
        val nextDateTimestamp = dateTimestamp + 86400000 // 다음 날의 타임스탬프

        // Spinner에서 선택한 시간 가져오기
        val selectedTimeIndex = binding.spinnerTime.selectedItemPosition

        // 선택한 시간에 기반한 시작 및 종료 타임스탬프 계산
        val startTimestamp: Long
        val endTimestamp: Long

        if (selectedTimeIndex > 0) {
            startTimestamp = dateTimestamp + selectedTimeIndex * 3600000 // 선택한 시간의 시작
            endTimestamp = startTimestamp + 3600000 // 선택한 시간의 끝 (1시간 후)
        } else {
            startTimestamp = dateTimestamp
            endTimestamp = nextDateTimestamp
        }

        db.collection("ROOMS")
            .whereGreaterThanOrEqualTo("departureTimeStamp", startTimestamp)
            .whereLessThan("departureTimeStamp", endTimestamp)
            .get()
            .addOnSuccessListener { result ->
                val rooms = mutableListOf<RoomVO>()
                for (document in result) {
                    val room = document.toObject(RoomVO::class.java)
                    rooms.add(room)
                }
                setListViews(rooms) // 필터링된 방 데이터를 리스트뷰에 표시
                swipeRefreshLayout.isRefreshing = false // 데이터 로드 완료 후 새로고침 아이콘 숨기기
            }
            .addOnFailureListener { exception ->
                Log.w("ViewAllFragment", "Error getting documents: $exception")
                swipeRefreshLayout.isRefreshing = false // 에러 발생 시에도 새로고침 아이콘 숨기기
            }
    }

    // 날짜를 타임스탬프로 변환하는 함수
    private fun convertDateToTimestamp(date: String): Long {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.parse(date)?.time ?: 0
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
            val selectedItem = items[position]

            // 대상 Fragment 지정
            // curPartner >= maxPartner -> 참여 불가
            val fragment = ViewDetailFragment()
            if (selectedItem.curPartner >= selectedItem.maxPartner) {
                Toast.makeText(requireContext(), "모집 인원이 마감되어 참여할 수 없습니다.", Toast.LENGTH_SHORT)
                    .show()
            } else {
                // 전달 데이터 생성
                val bundle = Bundle()
                bundle.putString("from", selectedItem.from)
                bundle.putString("to", selectedItem.to)
                bundle.putString("dateInfo", selectedItem.dateInfo)
                bundle.putString("owner", selectedItem.writer)
                bundle.putInt("price", selectedItem.price)
                bundle.putInt("curPartner", selectedItem.curPartner)
                bundle.putInt("maxPartner", selectedItem.maxPartner)
                bundle.putLong("departureTimeStamp", selectedItem.departureTimeStamp)
                bundle.putString("identifierID", selectedItem.identifierID)
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

    // 날짜 선택 다이얼로그를 표시하는 함수
    private fun showDatePickerDialog() {
        val datePickerFragment: DialogFragment = DatePickerAdapter(this)
        datePickerFragment.show(requireActivity().supportFragmentManager, "datePicker")
    }

    // 날짜가 선택되었을 때 호출되는 함수
    fun onDateSet(selectedDate: String) {
        edDate.setText(selectedDate)
        fetchAndFilterData(selectedDate) // 선택된 날짜를 사용하여 데이터 필터링
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
