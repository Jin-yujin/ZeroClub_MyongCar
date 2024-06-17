package com.example.mju_car.data

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

// AuthService 인터페이스 구현
class AuthRepository(private val context: Context) : AuthService {
    // SharedPreferences 인스턴스 초기화
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    // 사용자를 생성하는 함수
    override suspend fun create(email: String, password: String) {
        Firebase.auth.createUserWithEmailAndPassword(email, password).await()
    }

    // 사용자를 로그인하는 함수
    override suspend fun join(email: String, password: String) {
        Firebase.auth.signInWithEmailAndPassword(email, password).await()
    }

    // 현재 로그인된 사용자의 이메일을 반환하는 함수
    override fun email() : String? {
        val user = Firebase.auth.currentUser
        if (user != null) {
            return user.email
        }
        return null
    }

    // 현재 사용자 계정을 삭제하는 함수
    override suspend fun delete() {
        val user = Firebase.auth.currentUser
        user?.let {
            try {
                user.delete().await()
                clearUserData()
            } catch (e: Exception) {
                // 필요한 경우 재인증을 처리
                // 재인증 후 delete() 메서드를 다시 호출
            }
        }
    }

    // 로그아웃 함수
    override fun logout() {
            Firebase.auth.signOut()
            clearUserData()
    }

    // 현재 로그인된 사용자의 UID를 반환하는 함수
    override fun uid(): String? {
        val user = Firebase.auth.currentUser
        return user?.uid
    }

    // 사용자 데이터를 SharedPreferences에 저장하는 함수
    private fun saveUserData(user: FirebaseUser?) {
        val editor = sharedPreferences.edit()
        editor.putString("email", user?.email)
        editor.putString("uid", user?.uid)
        editor.apply()
    }

    // 사용자 데이터를 SharedPreferences에서 지우는 함수
    private fun clearUserData() {
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
    }

    // 초기화 블록, 인증 상태 변경 리스너 추가
    init {
        Firebase.auth.addAuthStateListener { auth ->
            if (auth.currentUser != null) {
                // 사용자가 로그인된 경우
                val user = auth.currentUser
                saveUserData(user)
            } else {
                // 사용자가 로그아웃된 경우
                clearUserData()
            }
        }
    }
}