package com.example.mju_car.ui.mypage

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.mju_car.R
import com.example.mju_car.data.AuthService
import com.example.mju_car.data.StorageService
import com.example.mju_car.data.vo.UserVO
import com.example.mju_car.databinding.FragmentModAccountBinding
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class ModAccountFragment : Fragment() {
    // AuthService와 StorageService를 Koin을 통해 주입받음
    private val authService     : AuthService by inject()
    private val storageService  : StorageService by inject()

    private lateinit var user   : UserVO
    private var _binding: FragmentModAccountBinding? = null

    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentModAccountBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Firebase Authentication 인스턴스 초기화
        // 데이터 세팅
        val email = authService.email()
        lifecycleScope.launch {
            // 이메일이 null이 아닌 경우
            if (email != null) {
                // Firestore에서 사용자 데이터 가져오기
                val data = storageService.findUser(email)
                if (data != null) {
                    user = data
                    Log.d("USER >> ", user.toString())
                    binding.etBank.setSelection(resources.getStringArray(R.array.banks).indexOf(user.bankName)) // 은행 선택
                    binding.etAccountNumber.setText(user.accountNumber) // 계좌번호 설정
                    binding.etAccountHolder.setText(user.accountHolder) // 예금주 설정
                    binding.editKakaoPayUrl.setText(user.kakaoPayUrl)   // 카카오페이 URL 설정
                }
            }
        }

        //저장 버튼 클릭 시
        binding.btnSave.setOnClickListener {
            // 입력된 계좌 정보를 가져옴
            val bankName        = binding.etBank.selectedItem.toString().trim()
            val accountNumber   = binding.etAccountNumber.text.toString().trim()
            val accountHolder   = binding.etAccountHolder.text.toString().trim()
            val kakaoPayUrl     = binding.editKakaoPayUrl.text.toString().trim()
            Log.d("account info", "$bankName, $accountNumber, $accountHolder, $kakaoPayUrl")
            // UserVO 객체에 저장
            user.bankName       = bankName      // UserVO의 bankName 필드에 입력 받은 은행명 저장
            user.accountNumber  = accountNumber // UserVO의 accountNumber 필드에 입력 받은 계좌번호 저장
            user.accountHolder  = accountHolder // UserVO의 accountHolder 필드에 입력 받은 예금주 저장
            user.kakaoPayUrl    = kakaoPayUrl   // UserVO의 kakaoPayUrl 필드에 입력 받은 카카오페이 QR URL 저장
            try {
                // FireStore에 회원정보 저장
                lifecycleScope.launch {
                    storageService.save(user)
                }
                // 토스트 메시지 생성 후
                toastShort("계좌정보가 저장되었습니다.")
                Log.e("ModAccountFragment", "success")
                navigateToMypageFragment()
            } catch (e: Exception) {
                // 회원가입 중 에러 발생 시
                Log.e("ModAccountFragment", "Failed to register", e)
                toastShort("저장에 실패하였습니다.")
            }
        }

        // 뒤로가기 버튼 처리
        requireActivity().onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateToMypageFragment()  // MyPageFragment로 이동
            }
        })
        return root
    }

    //Toast 메시지 짧게 보이기 (RegisterActivity 공통 메서드)
    private fun toastShort(message:String){
        Toast.makeText(
            requireActivity(),
            message,
            Toast.LENGTH_SHORT
        ).show()
    }

    //MypageFragment로 이동
    private fun navigateToMypageFragment() {
        val fragment = MypageFragment()
        requireActivity()
            .supportFragmentManager
            .beginTransaction()
            .replace(R.id.lyFrameLayout, fragment)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}