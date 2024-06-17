package com.example.mju_car.ui.shuttle

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.mju_car.R
import com.github.chrisbanes.photoview.PhotoView

class ShuttleimageViewerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shuttleimage_viewer)

        val photoView: PhotoView = findViewById(R.id.photoView)
        val imageResId = intent.getIntExtra("imageResId", R.drawable.shuttle_downtown)
        photoView.setImageResource(imageResId)
        window.statusBarColor = ContextCompat.getColor(this, R.color.navy)      // 상태바 색상 변경 
    }

}