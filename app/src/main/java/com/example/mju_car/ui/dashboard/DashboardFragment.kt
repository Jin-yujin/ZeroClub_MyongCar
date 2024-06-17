package com.example.mju_car.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.mju_car.R
import com.example.mju_car.databinding.FragmentDashboardBinding
import com.example.mju_car.ui.shuttle.ShuttleDowntown
import com.example.mju_car.ui.shuttle.ShuttleGiheung
import com.google.android.material.tabs.TabLayout

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    // 프래그먼트 인스턴스를 저장할 변수.
    private val downtownFragment = ShuttleDowntown()
    private val giheungFragment = ShuttleGiheung()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val tabLayout = binding.tabLayout

        // 초기에 downtownFragment를 표시
        childFragmentManager.beginTransaction()
            .replace(R.id.tab_layout_container, downtownFragment).commit()

        // 탭 선택 리스너 설정
        tabLayout.addOnTabSelectedListener(
            object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    when (tab.position) {
                        0 -> {
                            // downtownFragment로 교체
                            replaceFragment(downtownFragment)
                        }
                        1 -> {
                            // giheungFragment로 교체
                            replaceFragment(giheungFragment)
                        }
                    }
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {}
                override fun onTabReselected(tab: TabLayout.Tab?) {}
            }
        )
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // 프래그먼트 교체 함수
    private fun replaceFragment(fragment: Fragment) {
        childFragmentManager.beginTransaction()
            .replace(R.id.tab_layout_container, fragment)
            .commit()
    }
}
