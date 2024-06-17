package com.example.mju_car.ui.register

import DatePickerFragment
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.mju_car.ChatActivity
import com.example.mju_car.data.StorageService
import com.example.mju_car.data.vo.ParticipantVO
import com.example.mju_car.data.vo.RoomVO
import com.example.mju_car.data.vo.UserVO
import com.example.mju_car.databinding.FragmentRegisterBinding
import com.example.mju_car.ui.receipt.formatTimestamp
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.koin.android.ext.android.inject
import java.text.DecimalFormat
import java.time.LocalDate

class RegisterFragment : Fragment() {
    private val storageService: StorageService by inject()  // DI로 주입된 StorageService 객체
    private var timeInfo = TimeInfo.EMPTY   // 초기 시간 정보 설정
    private var startDes = Pair(0, "")  // 출발지 정보
    private var endDes = Pair(0, "")    // 목적지 정보
    private var desiredNumberOfPeople = Pair(0,2)   // 원하는 인원수 정보
    private var _binding: FragmentRegisterBinding? = null   // 바인딩 객체 초기화
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 출발지, 목적지, 인원수 spinner 설정
        setupSpinners()

        // 날짜 선택 한 것 edittext에 보이게
        binding.edGoDate.apply {
            setText(LocalDate.now().toString())
            setOnClickListener {
                val datePickerFragment = DatePickerFragment { _, year, month, dayOfMonth ->
                    val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                    setText(selectedDate.toString())
                    timeInfo = timeInfo.copy(year = year, month = month + 1, day = dayOfMonth)
                }
                datePickerFragment.show(requireActivity().supportFragmentManager, "datePicker")
            }
        }

        // 시간 선택 및 저장
        binding.timePicker.setIs24HourView(true) // 0-24시 표현
        binding.timePicker.setOnTimeChangedListener { _, hour, minute ->
            timeInfo = timeInfo.copy(hour = hour, minute = minute)
        }

        // 등록 버튼 클릭 리스너 설정
        binding.buttonRegistList.setOnClickListener {
            lifecycleScope.launch {
                val currentUser = Firebase.auth.currentUser
                if (currentUser != null) {
                    val db = Firebase.firestore
                    val userDocRef = db.collection("USERS").document(currentUser.uid)
                    try {
                        val userDocSnapshot = userDocRef.get().await()
                        val userVO = userDocSnapshot.toObject(UserVO::class.java)
                        val userNickname = "[${userVO?.score}] ${userVO?.nickName}"

                        // 고유 문서 ID 생성
                        val postRef = db.collection("ROOMS").document()
                        val postId = postRef.id

                        // registerRide함수로 데이터 저장. 파라미터로 데이터 전달
                        registerRide(postId) {
                            val bundle = Bundle().apply {
                                putString("from", startDes.second)
                                putString("to", endDes.second)
                                putString("dateInfo", formatTimestamp(timeInfo.toMillis()))
                                putLong("departureTimeStamp", timeInfo.toMillis())
                                putString("owner", userNickname)
                                putInt("price", getPrice(startDes.second, endDes.second))
                                putString("identifierID", postId)
                                putString("idOwner", currentUser.uid)
                                putInt("curPartner", 1)
                                putInt("maxPartner", desiredNumberOfPeople.second)
                                putString("roomId", "room_${startDes.second}_${endDes.second}_${formatTimestamp(timeInfo.toMillis())}") // 채팅방 ID 추가
                            }
                            Log.e("RegisterFragment", "$postId ")

                            // 채팅창으로 이동
                            val intent = Intent(requireContext(), ChatActivity::class.java)
                            intent.putExtras(bundle)
                            startActivity(intent)
                        }
                    } catch (e: Exception) {
                        Log.e("RegisterFragment", "Failed to get user info", e)
                        Toast.makeText(requireContext(), "유저 정보를 가져올 수 없습니다", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "사용자가 로그인되지 않았습니다", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 뒤로가기 버튼 처리
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    requireActivity().supportFragmentManager.popBackStack()
                }
            }
        )
    }

    //스피너 설정
    private fun setupSpinners() {
        // 출발지 선택 스피너
        binding.spinnerStart.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                startDes = Pair(position, parent?.getItemAtPosition(position).toString())
                //사용자가 스피너로 값 변경 할 때 예상 시간 및 금액 다르게 표기
                updateExpectedTimeAndPrice()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        // 도착지 선택 스피너
        binding.spinnerDest.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                endDes = Pair(position, parent?.getItemAtPosition(position).toString())
                //사용자가 스피너로 값 변경 할 때 예상 시간 및 금액 다르게 표기
                updateExpectedTimeAndPrice()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        // 인원수 선택 스피너
        binding.spinnerMember.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val itemString = parent?.getItemAtPosition(position).toString()
                val itemNumber = itemString.toIntOrNull() ?: 0

                desiredNumberOfPeople = Pair(position, itemNumber)
          }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    // 숫자 포맷
    val df = DecimalFormat("###,### 원")
    val df_time = DecimalFormat("00 분")

    // 예상 시간 및 금액 표기 함수
    private fun updateExpectedTimeAndPrice() {
        if (startDes.second.isNotEmpty() && endDes.second.isNotEmpty()) {
            val expectedTime = getTime(startDes.second, endDes.second)
            binding.textViewExpectTime.text = df_time.format(expectedTime)

            val expectedPrice = getPrice(startDes.second, endDes.second)
            binding.textViewExpectCost.text = df.format(expectedPrice)
        }
    }

    // 경로별 시간 정보 반환 함수
    fun getTime(from: String, to: String): Int {
        return when (from to to) {
            "기흥역 5번출구" to "명지대 학생회관" -> 13
            "기흥역 5번출구" to "명지대 3공학관" -> 15
            "기흥역 5번출구" to "명지대 정문" -> 10
            "기흥역 5번출구" to "명지대역 1번출구" -> 16
            "기흥역 5번출구" to "명지대 진입로" -> 12
            "명지대역 1번출구" to "기흥역 5번출구" -> 16
            "명지대역 1번출구" to "명지대 진입로" -> 4
            "명지대역 1번출구" to "명지대 정문" -> 7
            "명지대역 1번출구" to "명지대 학생회관" -> 7
            "명지대역 1번출구" to "명지대 3공학관" -> 10
            "명지대 진입로" to "명지대 학생회관" -> 5
            "명지대 진입로" to "명지대 정문" -> 4
            "명지대 진입로" to "명지대 3공학관" -> 9
            "명지대 진입로" to "기흥역 5번출구" -> 13
            "명지대 진입로" to "명지대역 1번출구" -> 7
            "명지대 학생회관" to "기흥역 5번출구" -> 13
            "명지대 학생회관" to "명지대역 1번출구" -> 7
            "명지대 학생회관" to "명지대 진입로" -> 5
            "명지대 정문" to "기흥역 5번출구" -> 10
            "명지대 정문" to "명지대 진입로" -> 4
            "명지대 정문" to "명지대역 1번출구" -> 9
            "명지대 3공학관" to "기흥역 5번출구" -> 15
            "명지대 3공학관" to "명지대역 1번출구" -> 10
            "명지대 3공학관" to "명지대 진입로" -> 9
            else -> 0   // 기본값
        }
    }

    //경로별 금액 정보 반환 함수
    fun getPrice(from: String, to: String): Int {
        return when (from to to) {
            "기흥역 5번출구" to "명지대 학생회관" -> 12100
            "기흥역 5번출구" to "명지대 3공학관" -> 12800
            "기흥역 5번출구" to "명지대 정문" -> 11800
            "기흥역 5번출구" to "명지대역 1번출구" -> 14000
            "기흥역 5번출구" to "명지대 진입로" -> 12200
            "명지대역 1번출구" to "기흥역 5번출구" -> 14000
            "명지대역 1번출구" to "명지대 진입로" -> 4800
            "명지대역 1번출구" to "명지대 정문" -> 4800
            "명지대역 1번출구" to "명지대 학생회관" -> 4800
            "명지대역 1번출구" to "명지대 3공학관" -> 5500
            "명지대 진입로" to "명지대 학생회관" -> 4800
            "명지대 진입로" to "명지대역 1번출구" -> 4800
            "명지대 진입로" to "명지대 정문" -> 4800
            "명지대 진입로" to "명지대 3공학관" -> 5200
            "명지대 진입로" to "기흥역 5번출구" -> 12800
            "명지대 학생회관" to "기흥역 5번출구" -> 12100
            "명지대 학생회관" to "명지대역 1번출구" -> 4800
            "명지대 학생회관" to "명지대 진입로" -> 4800
            "명지대 정문" to "기흥역 5번출구" -> 11800
            "명지대 정문" to "명지대 진입로" -> 4800
            "명지대 정문" to "명지대역 1번출구" -> 4800
            "명지대 3공학관" to "기흥역 5번출구" -> 12800
            "명지대 3공학관" to "명지대역 1번출구" -> 5500
            "명지대 3공학관" to "명지대 진입로" -> 5200
            else -> 0   // 기본값
        }
    }

    // 등록 함수
    private fun registerRide(postId: String, onSuccess: () -> Unit) {
        //정보 입력 확인
        if (startDes.first == 0 || endDes.first == 0) {
            Toast.makeText(requireContext(), "출발지와 목적지를 선택해주세요", Toast.LENGTH_SHORT).show()
            return
        } else if(!timeInfo.isValid() || timeInfo.toMillis() <= System.currentTimeMillis()){
            Toast.makeText(requireContext(), "시간을 확인해주세요", Toast.LENGTH_SHORT).show()
            return
        } else if (!binding.checkWarning1.isChecked || !binding.checkWarning2.isChecked) {
            Toast.makeText(requireContext(), "약관에 동의해주세요", Toast.LENGTH_SHORT).show()
            return
        }

        // 코루틴을 이용하여 Firebase Firestore에 데이터를 저장
        lifecycleScope.launch {
            val currentUser = Firebase.auth.currentUser

            if (currentUser != null) {
                val db = Firebase.firestore
                val userDocRef = db.collection("USERS").document(currentUser.uid)

                try {
                    val userDocSnapshot = userDocRef.get().await()
                    val userVO = userDocSnapshot.toObject(UserVO::class.java)

                    //User에 등록 정보 저장
                    if (userVO != null) {
                        val nickname = userVO.nickName
                        val score = userVO.score
                        val entity = RoomVO(
                            departureLocation = startDes.second,
                            destinationLocation = endDes.second,
                            desiredNumberOfPeople = desiredNumberOfPeople.second,
                            currentNumberOfPeople = 1,
                            departureTimeStamp = timeInfo.toMillis(),
                            owner = "[$score] $nickname", // UserVO의 nickName을 사용
                            authorToken = userVO.authorToken,
                            idOwner = currentUser.uid,
                            identifierID = postId, // 자동 생성된 고유 문서 ID 사용
                            timeTake = getTime(startDes.second, endDes.second),
                            priceTake = getPrice(startDes.second, endDes.second),
                            participants = mutableListOf(ParticipantVO("[$score] $nickname", currentUser.uid)), //participants 배열에 owner 정보 추가
                        )

                        storageService.save(entity)

                        val updatedUserData = hashMapOf<String, Any>(
                            "participatedRoomIds" to FieldValue.arrayUnion(postId) // postId는 새로 생성된 방의 identifierID
                        )
                        userDocRef.update(updatedUserData)

                        Toast.makeText(requireContext(), "등록되었습니다", Toast.LENGTH_SHORT).show()
                        onSuccess() // 성공 콜백 호출

                    } else {
                        Log.e("RegisterFragment", "UserVO is null")
                        Toast.makeText(requireContext(), "유저 정보를 가져올 수 없습니다", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("RegisterFragment", "Failed to save ride info", e)
                    Toast.makeText(requireContext(), "등록에 실패하였습니다", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "사용자가 로그인되지 않았습니다", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
