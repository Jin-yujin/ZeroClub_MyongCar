package com.example.mju_car.ui.mypage

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.mju_car.MainActivity
import com.example.mju_car.R
import com.example.mju_car.data.AuthService
import com.example.mju_car.data.StorageService
import com.example.mju_car.data.vo.RoomVO
import com.example.mju_car.data.vo.UserVO
import com.example.mju_car.databinding.FragmentMypageBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.koin.android.ext.android.inject
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

class MypageFragment : Fragment() {
    // AuthService와 StorageService를 Koin을 통해 주입받음
    private val authService     : AuthService by inject()
    private val storageService  : StorageService by inject()
    private var _binding        : FragmentMypageBinding? = null
    private val binding get() = _binding!!
    private lateinit var btnHelp        : AppCompatButton
    private lateinit var btnNotice      : AppCompatButton
    private lateinit var btnPrivacy     : AppCompatButton
    private lateinit var btnReport      : AppCompatButton
    private lateinit var btnModPassword : AppCompatButton
    private lateinit var btnModAccount  : AppCompatButton
    private lateinit var btnSignOut     : AppCompatButton
    private lateinit var btnLogout      : Button
    private lateinit var user           : UserVO

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMypageBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // 데이터 세팅
        val email = authService.email()
        lifecycleScope.launch {
            if (email != null) {
                // Firestore에서 사용자 데이터 가져오기
                val data = storageService.findUser(email)
                if (data != null) {
                    user = data
                    Log.d("USER >> ", user.toString())
                    // 사용자 정보 설정
                    binding.tvNickname.text = user.nickName
                    binding.tvScore.text = user.score
                    binding.tvDesc.text = user.email
                }
            }
        }
        setupButtonListeners()
        handleBackPress()

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupButtonListeners()

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

    private fun setupButtonListeners() {
        // [고객센터] 이동 이벤트 처리
        btnHelp = binding.btnHelp
        btnHelp.setOnClickListener {
            navigateToFragment(HelpFragment())
        }

        // [공지사항] 이동 이벤트 처리
        btnNotice = binding.btnNotice
        btnNotice.setOnClickListener {
            navigateToFragment(NoticeFragment())
        }

        // [약관 및 정책] 이동 이벤트 처리
        btnPrivacy = binding.btnPrivacy
        btnPrivacy.setOnClickListener {
            navigateToFragment(PrivacyFragment())
        }

        // [신고하기] 이동 이벤트 처리
        btnReport = binding.btnReport
        btnReport.setOnClickListener {
            navigateToFragment(ReportFragment(), "ReportFragment")
        }

        // [비밀번호 변경] 이동 이벤트 처리
        btnModPassword = binding.btnModPassword
        btnModPassword.setOnClickListener {
            navigateToFragment(ModPasswordFragment())
        }

        // [계좌정보 변경] 이동 이벤트 처리
        btnModAccount = binding.btnModAccount
        btnModAccount.setOnClickListener {
            navigateToFragment(ModAccountFragment())
        }

        // [회원탈퇴] 이동 이벤트 처리
        btnSignOut = binding.btnSignOut
        btnSignOut.setOnClickListener {
            showSignOutDialog()
        }

        // [로그아웃] 이동 이벤트 처리
        btnLogout = binding.btnLogout
        btnLogout.setOnClickListener {
            showLogoutDialog()
        }
    }

    // Fragment 이동을 처리하는 함수
    private fun navigateToFragment(fragment: Fragment, tag: String? = null) {
        requireActivity()
            .supportFragmentManager
            .beginTransaction()
            .replace(R.id.lyFrameLayout, fragment)
            .apply { tag?.let { addToBackStack(it) } }
            .commit()
    }

    // 현재 사용자의 방 정보를 가져오는 함수
    private suspend fun fetchCurrentUserRooms(): List<RoomVO> {
        val currentUserUid = Firebase.auth.currentUser?.uid ?: return emptyList()
        val db = FirebaseFirestore.getInstance()
        val roomsSnapshot = db.collection("ROOMS").get().await()
        val rooms = roomsSnapshot.toObjects(RoomVO::class.java)

        // 현재 사용자가 참여 중인 방 필터링
        val currentUserRooms = rooms.filter { room ->
            room.participants.any { it.addedByUser == currentUserUid } &&
                    room.departureTimeStamp >= getMidnightTimestamp()
        }
        return currentUserRooms
    }

    // 자정 타임스탬프를 반환하는 함수
    fun getMidnightTimestamp(): Long {
        val now = LocalDateTime.now()
        val midnight = now.with(LocalTime.MIN)
        return midnight.toInstant(ZoneOffset.UTC).toEpochMilli()
    }

    // 회원 탈퇴 다이얼로그를 표시하는 함수
    private fun showSignOutDialog() {
        val builder = AlertDialog.Builder(requireContext())
        val currentUser = Firebase.auth.currentUser

        lifecycleScope.launch {
            try {
                val db = FirebaseFirestore.getInstance()
                val userDocRef = db.collection("USERS").document(currentUser!!.uid)
                val currentUserRooms = fetchCurrentUserRooms()

                // 현재 이용 중인 방이 없을 경우에만 탈퇴 확인 다이얼로그 표시
                if (currentUserRooms.isEmpty()) {
                    builder.setTitle("회원탈퇴")
                    builder.setMessage("탈퇴하시겠습니까?")
                    builder.setPositiveButton("확인") { _, _ ->
                        lifecycleScope.launch {
                            try {
                                deleteUser(currentUser.uid)
                                authService.logout()
                                requireActivity().startActivity(Intent(requireActivity(), MainActivity::class.java))
                                requireActivity().finish()
                            } catch (e: Exception) {
                                Log.e("SignOutError", "Error during sign out: ", e)
                                // 사용자에게 오류 메시지 표시
                            }
                        }
                    }
                    builder.setNegativeButton("취소", null)
                    builder.show()
                } else {
                    // 현재 이용 중인 방이 있을 경우 탈퇴 불가 안내 다이얼로그 표시
                    builder.setTitle("회원탈퇴 불가")
                    builder.setMessage("이용 중인 방이 있을 때는 \n회원 탈퇴가 불가능합니다. \n이용이 모두 끝난 후 탈퇴해주시길 바랍니다.")
                    builder.setPositiveButton("확인", null)
                    builder.show()
                }
            } catch (e: Exception) {
                Log.e("SignOutError", "Error during sign out: ", e)
                // 사용자에게 오류 메시지 표시
            }
        }
    }

    // 사용자 데이터를 삭제하는 함수
    private suspend fun deleteUser(uid: String) {
        val db = FirebaseFirestore.getInstance()
        val userDocRef = db.collection("USERS").document(uid)
        val userSnapshot = userDocRef.get().await()
        val user = userSnapshot.toObject<UserVO>()

        // 사용자가 참여 중인 방 데이터 처리
        user?.participatedRoomIds?.forEach { roomId ->
            val roomDocRef = db.collection("ROOMS").document(roomId)
            val roomSnapshot = roomDocRef.get().await()
            val room = roomSnapshot.toObject<RoomVO>()

            room?.let {
                it.participants.removeAll { participant -> participant.addedByUser == user.addedByUser }
//                room.currentNumberOfPeople -= 1
                roomDocRef.set(it).await() // 룸 정보 업데이트
            }
        }

        // 사용자가 방장인 방 데이터 처리
        val userRoomsQuery = db.collection("ROOMS").whereEqualTo("idOwner", user?.addedByUser).get().await()
        userRoomsQuery.documents.forEach { document ->
            document.reference.delete().await()
        }

        // 사용자 데이터 삭제
        userDocRef.delete().await()
        authService.delete()
        storageService.delete(user!!.addedByUser)
    }

    // 로그아웃 다이얼로그를 표시하는 함수
    private fun showLogoutDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("로그아웃")
        builder.setMessage("로그아웃하시겠습니까?")
        builder.setPositiveButton("확인") { _, _ ->
            authService.logout()
            requireActivity().startActivity(Intent(requireActivity(), MainActivity::class.java))
            requireActivity().finish()
        }
        builder.setNegativeButton("취소", null)
        builder.show()
    }

    private fun handleBackPress() {
        requireActivity().onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val fragment = MypageFragment()
                requireActivity()
                    .supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.lyFrameLayout, fragment)
                    .commit()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
