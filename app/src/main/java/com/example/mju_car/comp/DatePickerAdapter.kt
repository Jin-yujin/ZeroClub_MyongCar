package com.example.mju_car.comp

import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.app.Dialog
import android.os.Bundle
import android.widget.DatePicker
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.example.mju_car.ui.viewall.ViewAllFragment
import java.util.Calendar

class DatePickerAdapter(val parent:Fragment) : DialogFragment(), OnDateSetListener {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // 현재 날짜로 DatePickerDialog 초기화
        val calendar = Calendar.getInstance()
        val year = calendar[Calendar.YEAR]
        val month = calendar[Calendar.MONTH]
        val day = calendar[Calendar.DAY_OF_MONTH]
        return DatePickerDialog(requireActivity(), this, year, month, day)
    }

    override fun onDateSet(view: DatePicker, year: Int, month: Int, dayOfMonth: Int) {
        // 선택된 날짜를 문자열로 형식화 (예: "2024-6-15")
        val selectedDate = year.toString() + "-" + (month + 1) + "-" + dayOfMonth
        (parent as ViewAllFragment).onDateSet(selectedDate)
    }
}