package com.example.mju_car.ui.shuttle

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.mju_car.databinding.StopTaxiBinding

class TaxiFragment : Fragment() {

    private var _binding: StopTaxiBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = StopTaxiBinding.inflate(inflater, container, false)


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