package com.example.mju_car

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.mju_car.data.AuthService
import com.example.mju_car.databinding.LoginBinding
import com.skydoves.bindables.BindingActivity
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import androidx.core.content.ContextCompat
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat

class MainActivity : BindingActivity<LoginBinding>(R.layout.login) {

    //Firebase login Service 객체
    private val authService: AuthService by inject()
    private val sharedPreferences: SharedPreferences by lazy {
        getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    }
    private val NOTIFICATION_PERMISSION_REQUEST_CODE = 1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 상태바 색상 변경
        window.statusBarColor = ContextCompat.getColor(this, R.color.navy)      // 상태바 색상 변경

        // 자동 로그인 체크
        val email = sharedPreferences.getString("email", null)
        val uid = sharedPreferences.getString("uid", null)

        if (email != null && uid != null) {
            // 자동 로그인 처리
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }

        // 로그인 버튼 클릭 리스너 설정
        binding.buttonLogin.setOnClickListener {
            val email = binding.editTextTextEmailAddress.text.toString()
            val password = binding.editTextTextPassword.text.toString()


            lifecycleScope.launch {
                if(email.isNotEmpty() && password.isNotEmpty()) {
                    try{
                        //로그인 시도
                        authService.join(email, password)
                        Log.e("MainActivity", "로그인 성공")
                        //로그인 성공하면 HomeActivity 진입
                        startActivity(Intent(this@MainActivity, HomeActivity::class.java))

                        // 앱을 실행할 때마다 FCM 토큰을 업데이트하여 Firestore에 저장
                        updateFCMToken()


                        finish()
                        // 로그인 성공 시 사용자 데이터 저장
                        saveUserData(email, authService.uid())
                    }catch (e: Exception){
                        Log.e("MainActivity", "로그인 실패", e)
                        Toast.makeText(this@MainActivity, "아이디나 패스워드가 틀렸습니다.", Toast.LENGTH_SHORT).show()
                    }
                }else{
                    Toast.makeText(this@MainActivity, "양식을 입력해주세요", Toast.LENGTH_SHORT).show()
                }
            }
        }
        // 회원가입 버튼 클릭 리스너 설정
        binding.textView2.setOnClickListener {
            startActivity(Intent(this@MainActivity, RegisterActivity::class.java))
        }
        // 알림 권한이 허용되지 않은 경우 권한 요청
        if (!isNotificationPermissionGranted()) {
            requestNotificationPermission()
        }

    }

    // 알림 권한이 허용되었는지 확인하는 함수
    private fun isNotificationPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    // 알림 권한 요청하는 함수
    private fun requestNotificationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            )
        ) {
            // 권한이 필요한 이유를 설명하는 대화상자 표시
            AlertDialog.Builder(this)
                .setTitle("알림 권한 필요")
                .setMessage("이 앱에서는 예약에 대한 참여 알림을 받기 위해 알림 권한이 필요합니다. 알림을 받으시겠습니까?")
                .setPositiveButton("허용") { _, _ ->
                    // 권한 요청
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                        NOTIFICATION_PERMISSION_REQUEST_CODE
                    )
                }
                .setNegativeButton("거부", null)
                .show()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    // 권한 요청 결과 처리
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 권한이 허용된 경우 처리
                Toast.makeText(this, "알림 권한이 허용되었습니다.", Toast.LENGTH_SHORT).show()
            } else {
                // 권한이 거부된 경우 처리
                Toast.makeText(this, "알림 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 사용자 데이터를 SharedPreferences에 저장하는 함수
    private fun saveUserData(email: String, uid: String?) {
        val editor = sharedPreferences.edit()
        editor.putString("email", email)
        editor.putString("uid", uid)
        editor.apply()
    }

    // FCM 토큰을 업데이트하는 함수
    private fun updateFCMToken() {
        lifecycleScope.launch {
            val currentUser = Firebase.auth.currentUser
            if (currentUser != null) {
                try {
                    val newToken = FirebaseMessaging.getInstance().token.await()
                    val userDocRef = Firebase.firestore.collection("USERS").document(currentUser.uid)

                    // Firestore에 FCM 토큰 업데이트
                    userDocRef.update("authorToken", newToken)
                } catch (e: Exception) {
                    Log.e("RegisterActivity", "Failed to update FCM token", e)
                }
            }
        }
    }
}