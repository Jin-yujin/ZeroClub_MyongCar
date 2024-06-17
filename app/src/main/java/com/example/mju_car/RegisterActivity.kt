package com.example.mju_car

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.mju_car.data.AuthService
import com.example.mju_car.data.StorageService
import com.example.mju_car.data.vo.UserVO
import com.example.mju_car.databinding.RegisterMainBinding
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.skydoves.bindables.BindingActivity
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.koin.android.ext.android.inject
import java.util.regex.Pattern

class RegisterActivity : BindingActivity<RegisterMainBinding>(R.layout.register_main) {

    // Firebase login Service 객체
    private val authService: AuthService by inject()

    // Firebase DB Service 객체
    private val storageService: StorageService by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 상태바 색상 변경
        window.statusBarColor = ContextCompat.getColor(this, R.color.navy)

        // 회원가입 버튼 클릭 리스너 설정
        binding.btnRegister.setOnClickListener {
            lifecycleScope.launch {
                val email           = binding.textViewEmailValue.text.toString()
                val password        = binding.editTextPassword.text.toString()
                val repassword      = binding.EditConfirmPassword.text.toString()
                val nick            = binding.EditNickname.text.toString().trim()
                val studentId       = binding.editStudentNum.text.toString()
                val bankName        = binding.spinnerBank.selectedItem.toString()
                val accountNumber   = binding.edAccountNumber.text.toString()
                val accountHolder   = binding.editAccountHolder.text.toString()
                val kakaoPayUrl     = binding.editKakaoPayUrl.text.toString().trim()
                val pwPattern       = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@!.,]?)[A-Za-z\\d@!.,]{8,15}$"
                val emailPattern    = "^[\\w.-]+@mju\\.ac\\.kr$"

                // Firestore에서 사용자의 닉네임을 확인하여 중복 여부 검사
                val isNickNameDuplicate = checkNickNameDuplicate(nick)
                // Firestore에서 사용자의 학번을 확인하여 중복 여부 검사
                val isStudentIdDuplicate = checkStudentIdDuplicate(studentId)

                if (email.isNotEmpty() && password.isNotEmpty() && repassword.isNotEmpty() && nick.isNotEmpty() && studentId.isNotEmpty()) {
                    if (!Pattern.matches(emailPattern, email)) {
                        toastShort("명지대학교 이메일만 가능합니다.")
                        return@launch
                    }

                    if (isNickNameDuplicate) {
                        toastShort("이미 사용 중인 닉네임입니다. 다른 닉네임을 입력하세요.")
                        return@launch
                    }

                    // 학번 형식 제한 조건 추가
                    if (!studentId.matches(Regex("^60\\d{6}$"))) {
                        Toast.makeText(
                            this@RegisterActivity,
                            "학번을 올바르게 입력해주세요.",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@launch
                    }

                    if (isStudentIdDuplicate) {
                        toastShort("이미 사용 중인 학번입니다. 다른 학번을 입력하세요.")
                        return@launch
                    }

                    if (Pattern.matches(pwPattern, password)) {
                        if (password == repassword) {
                            try {
                                // Firebase 회원가입 체크
                                authService.create(email, password)

                                // Firebase계정 키값 생성
                                val addedByUser = Firebase.auth.currentUser?.uid ?: ""

                                // FireStore에 회원정보 저장
                                storageService.save(
                                    UserVO(
                                        email           = email,
                                        nickName        = nick,
                                        studentId       = studentId,
                                        addedByUser     = addedByUser,
                                        bankName        = bankName,
                                        accountNumber   = accountNumber,
                                        accountHolder   = accountHolder,
                                        kakaoPayUrl     = kakaoPayUrl,
                                        authorToken     = FirebaseMessaging.getInstance().token.await()
                                    )
                                )

                                // Firebase는 회원가입 즉시 로그인 처리되므로 명시적으로 로그아웃 처리
                                authService.logout()

                                // 토스트 메시지 생성 후 종료
                                toastShort("회원가입에 성공하였습니다.")
                                finish()
                                Log.e("RegisterActivity", "success")
                            } catch (e: FirebaseAuthUserCollisionException) {
                                // 이메일 중복 에러 처리
                                toastLong("이미 가입된 이메일입니다.")
                            } catch (e: Exception) {
                                // 회원가입 중 에러 발생 시
                                Log.e("RegisterActivity", "Failed to register", e)
                                toastShort("회원가입에 실패하였습니다.")
                            }
                        } else {
                            // 암호와 비밀번호 확인 값이 일치하지 않는 경우
                            toastShort("비밀번호가 일치하지 않습니다.")
                        }
                    } else {
                        // 비밀번호 형식이 올바르지 않은 경우
                        toastShort("비밀번호 형식이 옳지 않습니다.")
                    }
                } else {
                    // 회원정보를 모두 입력하지 않은 경우
                    toastShort("회원정보를 모두 입력해주세요.")
                }
            }
        }
    }

    // Toast 메시지 짧게 보이기 (RegisterActivity 공통 메서드)
    private fun toastShort(message:String){
        Toast.makeText(
            this@RegisterActivity,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }

    // Toast 메시지 길게 보이기 (RegisterActivity 공통 메서드)
    private fun toastLong(message:String){
        Toast.makeText(
            this@RegisterActivity,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }

    // Firestore에서 닉네임 중복 여부를 검사하는 함수
    private suspend fun checkNickNameDuplicate(nickName: String): Boolean {
        return try {
            val querySnapshot = Firebase.firestore.collection("USERS")
                .whereEqualTo("nickName", nickName)
                .limit(1) // 일치하는 문서 1개만 가져오기
                .get()
                .await()

            // 닉네임이 중복되는 경우 true를 반환하고, 그렇지 않은 경우 false를 반환합니다.
            !querySnapshot.isEmpty
        } catch (e: Exception) {
            // 오류가 발생한 경우 false를 반환합니다.
            false
        }
    }

    // Firestore에서 학번 중복 여부를 검사하는 함수
    private suspend fun checkStudentIdDuplicate(studentId: String): Boolean {
        return try {
            val querySnapshot = Firebase.firestore.collection("USERS")
                .whereEqualTo("studentId", studentId)
                .limit(1) // 일치하는 문서 1개만 가져오기
                .get()
                .await()

            !querySnapshot.isEmpty
        } catch (e: Exception) {
            // 오류가 발생한 경우 false를 반환합니다.
            false
        }
    }
}