package com.example.mju_car.dbo
data class User(
    val userID: String,
    val currentLocation: Double,
    val nickName: String,
    val password: String,
    val name: String,
    val classNum: Int,
    val picture: String?,
    val email: String,
    val dateTime: String
) {
    init {
        require(userID.isNotBlank()) { "UserID must not be blank" }
    }
}