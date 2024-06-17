package com.example.mju_car.ui.register

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

// 시간 정보를 저장하는 객체
data class TimeInfo(
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int
) {
    companion object {
        // 디폴트 현재 날짜 설정
        val now = LocalDate.now()
        // 초기 시간을 현재 날짜와 00:00으로 설정
        val EMPTY = TimeInfo(now.year, now.monthValue, now.dayOfMonth, 0, 0) // 초기 시간을 00:00으로 설정
    }

    // 유효성 체크 TRUE 여야 시간 날짜 데이터입력
    fun isValid(): Boolean {
        return year > 0 && month in 1..12 && day in 1..31 && hour in 0..23 && minute in 0..59
    }

    // UTC 시간으로 변환하는 함수
    fun toMillis(): Long {
        if (!isValid()) throw IllegalStateException("Invalid TimeInfo: $this") // 유효하지 않으면 예외 발생
        val dateTime = LocalDateTime.of(year, month, day, hour, minute) // LocalDateTime 객체 생성
        val zonedDateTime = dateTime.atZone(ZoneId.systemDefault()) // 시스템 기본 시간대로 ZonedDateTime 객체 생성
        return zonedDateTime.toInstant().toEpochMilli() // UTC 밀리초로 변환하여 반환
    }
}
