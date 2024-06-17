package com.example.mju_car.data.vo

//예약 등록 저장 테이블
data class RoomVO(
    val departureLocation: String = "", //출발지
    val destinationLocation: String= "", //목적지
    val desiredNumberOfPeople: Int = 0, //희망인원
    var currentNumberOfPeople: Int = 0, //인원
    val departureTimeStamp: Long =0 , //UTC 타임스탬프
    var owner: String= "", //등록 유저
    val idOwner :String ="",
    val authorToken: String?=null, //등록 유저 토큰
    val identifierID: String= "", // 게시글의 문서 ID
    val timeTake: Int = 0,
    val priceTake: Int=0,
    var participants: MutableList<ParticipantVO> = mutableListOf(), // 참여자 정보 리스트
)