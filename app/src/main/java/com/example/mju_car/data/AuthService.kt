package com.example.mju_car.data

interface AuthService {
    suspend fun create(email: String, password: String)
    suspend fun join(email: String, password: String)
    fun email():String? // 현재 로그인한 사용자 이메일 주소 조회
    suspend fun delete()
    fun logout()
    fun uid(): String? // 현재 로그인한 사용자 uid 조회
}