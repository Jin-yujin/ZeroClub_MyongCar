package com.example.mju_car.ui.shuttle

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.mju_car.R
import com.example.mju_car.databinding.StopBusBinding
import com.example.mju_car.ui.dashboard.DashboardFragment

class BusFragment : Fragment() {

    private var _binding: StopBusBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = StopBusBinding.inflate(inflater, container, false)

        // Bus 시간표 확인하기 사진 클릭시
        binding.busTime.setOnClickListener {
            val dashboardFragment = DashboardFragment()
            val transaction = requireActivity().supportFragmentManager.beginTransaction()
            transaction.replace(R.id.lyFrameLayout, dashboardFragment)
            transaction.addToBackStack(null) // 뒤로 가기 스택에 추가
            transaction.commit()
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 필요한 초기화 작업 수행
        initViews()
    }

    private fun initViews() {
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}