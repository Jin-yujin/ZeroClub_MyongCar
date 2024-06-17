package com.example.mju_car.data

import com.example.mju_car.data.vo.ReportVO
import com.example.mju_car.data.vo.RoomVO
import com.example.mju_car.data.vo.UserVO

interface StorageService {
    suspend fun save(userVO: UserVO)
    suspend fun save(roomVO: RoomVO)

    suspend fun save(reportVO: ReportVO)
    suspend fun delete(studentId: String)
    suspend fun findUser(email: String):UserVO? // 해당 이메일의 회원 찾기
}