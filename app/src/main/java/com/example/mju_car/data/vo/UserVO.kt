package com.example.mju_car.data.vo

data class UserVO(
    val email           : String    = "",
    val nickName        : String    = "",
    val studentId       : String    = "",
    val addedByUser     : String    = "",
    val authorToken     : String?   = null, // 등록 유저 토큰
    val participatedRoomIds: MutableList<String> = mutableListOf(), // 참여한 예약 ID 리스트
    val reportCount     : Int       = 0,    // 신고 횟수
    val score           : String    = "A+", // 학점
    var bankName        : String    = "",   // 은행명
    var accountHolder   : String    = "",   // 예금주
    var accountNumber   : String    = "",   // 계좌번호
    var kakaoPayUrl     : String    = ""    // 카카오페이 URL
)