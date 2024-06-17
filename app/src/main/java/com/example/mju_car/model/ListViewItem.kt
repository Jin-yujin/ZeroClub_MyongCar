package com.example.mju_car.model

// 카풀 목록 담기는 형식
data class ListViewItem(
   val from        : String,        // 출발지
   val to          : String,        // 도착지
   val dateInfo    : String,        // 일자정보
   val writer      : String,        // 작성자
   val price       : Int,           // 예상 금액
   val time        : Int,           // 예상 혹은 소요 시간
   val curPartner  : Int,           // 현재 참여자 수
   val maxPartner  : Int,           // 최대 참여자 수
   val identifierID: String,        // 게시글 식별자
   val idOwner     : String,        // 작성자 아이디
   val departureTimeStamp:Long      // 출발시각
)