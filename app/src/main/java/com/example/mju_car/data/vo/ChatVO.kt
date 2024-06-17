package com.example.mju_car.data.vo

data class ChatVO(
    val message: String,          // 메시지 내용
    val nickName: String,         // 닉네임 필드(userId로 사용)
    val departureTimeStamp: Long, // UTC 타임스탬프
    val roomId: String            // 채팅방 ID
) {
    constructor() : this("", "", 0, "")
}
