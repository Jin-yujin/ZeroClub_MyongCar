package com.example.mju_car.data

import android.util.Log
import com.example.mju_car.data.vo.ReportVO
import com.example.mju_car.data.vo.RoomVO
import com.example.mju_car.data.vo.UserVO
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

const val USERS     = "USERS"
const val ROOMS     = "ROOMS"
const val REPORTS   = "REPORTS"

class StorageRepository : StorageService {
    // UserVO 객체를 Firestore에 저장하는 함수
    override suspend fun save(userVO: UserVO) {
        Firebase.firestore.collection(USERS).document(userVO.addedByUser).set(userVO).await()
    }

    // RoomVO 객체를 Firestore에 저장하는 함수
    override suspend fun save(roomVO: RoomVO) {
        Firebase.firestore.collection(ROOMS).document(roomVO.identifierID).set(roomVO).await()
    }

    // ReportVO 객체를 Firestore에 저장하는 함수
    override suspend fun save(reportVO: ReportVO) {
        Firebase.firestore.collection(REPORTS).document(reportVO.cause).set(reportVO).await()
    }

    // 사용자 ID로 사용자 데이터를 Firestore에서 삭제하는 함수
    override suspend fun delete(studentId: String) {
        Firebase.firestore.collection(USERS).document(studentId).delete()
    }

    // 이메일로 사용자 데이터를 Firestore에서 찾는 함수
    override suspend fun findUser(email: String) :UserVO? {
        val collection = Firebase.firestore.collection(USERS)
        return try {
            // Firestore에서 사용자 데이터 검색
            val result = collection.get().await()
            for (document in result) {
                // val users = document.toObject(UserVO::class.java)
                Log.d("TAG >>> ", "${document.id} => ${document.data}")
                val data = document.data
                if(data["email"].toString() == email){
                    val nickName    = data["nickName"].toString()
                    val studentId   = data["studentId"].toString()
                    val score       = data["score"].toString()
                    val bankName        = data["bankName"].toString()
                    val accountNumber   = data["accountNumber"].toString()
                    val accountHolder   = data["accountHolder"].toString()
                    val kakaoPayUrl     = data["kakaoPayUrl"].toString()
                    return UserVO(
                          email
                        , nickName
                        , studentId
                        , addedByUser   = document.id
                        , authorToken   = null
                        , score         = score
                        , bankName      = bankName
                        , accountNumber = accountNumber
                        , accountHolder = accountHolder
                        , kakaoPayUrl   = kakaoPayUrl
                    )
                }
            }
            null
        } catch (e : FirebaseFirestoreException) {
            Log.e("StorageRepository", "In getUser() -> ", e)
            null
        }
    }
}