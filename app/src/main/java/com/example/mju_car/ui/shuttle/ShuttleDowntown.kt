package com.example.mju_car.ui.shuttle

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import com.example.mju_car.R
import com.github.chrisbanes.photoview.PhotoView

class ShuttleDowntown : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_shuttle_downtown, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val imgDowntown: PhotoView = view.findViewById(R.id.imgDowntown)
        imgDowntown.setOnClickListener {
            // 이미지 클릭 시 ShuttleimageViewerActivity로 이동
            val intent = Intent(requireContext(), ShuttleimageViewerActivity::class.java)
            intent.putExtra("imageResId", R.drawable.shuttle_downtown)
            startActivity(intent)
        }

        // 뒤로가기 버튼 처리
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // 뒤로가기 버튼 클릭 시 프래그먼트 백 스택에서 제거
                    requireActivity().supportFragmentManager.popBackStack()
                }
            }
        )
    }
}
