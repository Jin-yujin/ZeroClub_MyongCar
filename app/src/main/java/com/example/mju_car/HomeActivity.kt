package com.example.mju_car

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.mju_car.databinding.ActivityHomeBinding
import com.example.mju_car.data.FirebaseUtil
import com.example.mju_car.data.vo.RoomVO
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeActivity : AppCompatActivity(), FirebaseUtil.DataCallback {

    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 상태바 색상을 변경
        window.statusBarColor = ContextCompat.getColor(this, R.color.navy)
        // 액션바를 숨김
        supportActionBar?.hide()

        // 바인딩 객체를 초기화
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //네비게이션 바 설정
        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home,
                R.id.navigation_dashboard,
                R.id.navigation_register,
                R.id.navigation_recepit,
                R.id.navigation_mypage
            )
        )

        // 네비게이션 컨트롤러와 앱바를 설정
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // 파이어베이스에서 데이터 가져오기
        FirebaseUtil.fetchRooms(this)
    }

    override fun onDataReceived(rooms: List<RoomVO>) {
    }

    override fun onError(exception: Exception) {
        Log.w("HomeActivity", "Error getting documents: $exception")
    }
}