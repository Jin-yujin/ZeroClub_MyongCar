package com.example.mju_car.ui.mypage

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.mju_car.R
import com.example.mju_car.data.StorageService
import com.example.mju_car.data.vo.ReportVO
import com.example.mju_car.databinding.FragmentReportBinding
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class ReportFragment : Fragment() {
    private var _binding: FragmentReportBinding? = null
    private val binding get() = _binding!!

    // Firebase DB Service 객체 주입
    private val storageService: StorageService by inject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 신고하기 버튼 클릭 시
        binding.btnReport.setOnClickListener {
            val reportedNickname = binding.etReportedNickname.text.toString().trim()
            val reportedCause = binding.etReportedCause.text.toString().trim()

            if (reportedNickname.isNotEmpty()) {
                reportUser(reportedNickname)    // 사용자 신고 함수 호출

                if (reportedCause.isNotEmpty()) {
                    // 코루틴을 사용하여 비동기로 데이터 저장 처리
                    lifecycleScope.launch {
                        try {
                            storageService.save(
                                ReportVO(
                                    nickName = reportedNickname,
                                    cause = reportedCause
                                )
                            )
                            Toast.makeText(requireContext(), "신고가 완료되었습니다.", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(requireContext(), "신고에 실패했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "신고 사유를 입력해주세요.", Toast.LENGTH_SHORT).show()

                }
            } else {
                Toast.makeText(requireContext(), "닉네임을 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
        }

        // 신고 사유 입력란의 텍스트 변화 감지 리스너 설정
        binding.etReportedCause.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                val reportCause = s.toString().trim()
                binding.btnReport.isEnabled = reportCause.isNotEmpty()
            }
        })
    }

    // 사용자를 신고하는 함수
    private fun reportUser(reportedNickname: String) {
        val db = FirebaseFirestore.getInstance()
        val usersRef = db.collection("USERS")

        // 닉네임을 기준으로 사용자 검색
        usersRef.whereEqualTo("nickName", reportedNickname)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    for (document in querySnapshot.documents) {
                        val userRef = usersRef.document(document.id)

                        // Firestore 트랜잭션을 사용하여 reportCount 업데이트와 score 업데이트를 함께 처리
                        FirebaseFirestore.getInstance().runTransaction { transaction ->
                            val snapshot = transaction.get(userRef)
                            val currentReportCount = snapshot.getLong("reportCount")?.toInt() ?: 0
                            val newReportCount = currentReportCount + 1

                            // reportCount 업데이트
                            transaction.update(userRef, "reportCount", newReportCount)

                        }.addOnFailureListener {
                                    Toast.makeText(
                                        requireContext(),
                                        "신고 처리 중 오류가 발생했습니다.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                        Toast.makeText(requireContext(), "신고가 완료되었습니다.", Toast.LENGTH_SHORT).show()

                        navigateToMypageFragment() // 신고 완료 후 MypageFragment로 이동

                    } else {
                        Toast.makeText(requireContext(), "해당 닉네임을 찾을 수 없습니다.", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "데이터베이스 오류가 발생했습니다.", Toast.LENGTH_SHORT)
                            .show()
                    }
            }

    // MypageFragment로 이동하는 함수
    private fun navigateToMypageFragment() {
        val fragment = MypageFragment() // MypageFragment로 이동
        requireActivity()
            .supportFragmentManager
            .beginTransaction()
            .replace(R.id.lyFrameLayout, fragment) // lyFrameLayout을 MypageFragment로 교체
            .commit()


    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}