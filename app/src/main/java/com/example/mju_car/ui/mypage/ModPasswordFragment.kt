package com.example.mju_car.ui.mypage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.example.mju_car.R
import com.example.mju_car.databinding.FragmentModPasswordBinding
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class ModPasswordFragment : Fragment() {

    private var _binding: FragmentModPasswordBinding? = null

    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentModPasswordBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Firebase Authentication 인스턴스 초기화
        auth = FirebaseAuth.getInstance()

        binding.btnChangePassword.setOnClickListener {
            val currentPassword = binding.etCurrentPassword.text.toString().trim()
            val newPassword = binding.etNewPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()

            // 모든 필드가 채워졌는지 확인
            if (currentPassword.isNotEmpty() && newPassword.isNotEmpty() && confirmPassword.isNotEmpty()) {
                // 새 비밀번호와 확인 비밀번호가 일치하는지 확인
                if (newPassword == confirmPassword) {
                    changePassword(currentPassword, newPassword)
                } else {
                    showToast("새 비밀번호가 일치하지 않습니다.")
                }
            } else {
                showToast("모든 필드를 입력해주세요.")
            }
        }

        // 뒤로가기 버튼 처리
        requireActivity().onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateToMypageFragment()
            }
        })
        return root
    }

    private fun changePassword(currentPassword: String, newPassword: String) {
        val user = auth.currentUser ?: return

        // 현재 비밀번호 재인증
        val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)
        user.reauthenticateAndRetrieveData(credential)
            .addOnSuccessListener {
                // 새 비밀번호로 업데이트
                user.updatePassword(newPassword)
                    .addOnSuccessListener {
                        showToast("비밀번호 변경 성공")
                        navigateToMypageFragment()
                    }
                    .addOnFailureListener { e ->
                        showToast("비밀번호 변경 실패: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                showToast("현재 비밀번호가 일치하지 않습니다.")
            }
    }

    // MyPageFragment로 이동
    private fun navigateToMypageFragment() {
        val fragment = MypageFragment()
        requireActivity()
            .supportFragmentManager
            .beginTransaction()
            .replace(R.id.lyFrameLayout, fragment)
            .commit()
    }

    // Toast 메시지 짧게 보이기
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}