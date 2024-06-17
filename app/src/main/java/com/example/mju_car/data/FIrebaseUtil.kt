package com.example.mju_car.data

import android.util.Log
import com.example.mju_car.data.vo.RoomVO
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

object FirebaseUtil {
    private const val TAG = "FirebaseUtil"

    interface DataCallback {
        fun onDataReceived(rooms: List<RoomVO>)
        fun onError(exception: Exception)
    }

    fun fetchRooms(callback: DataCallback) {
        val db = Firebase.firestore
        db.collection("ROOMS")
            .get()
            .addOnSuccessListener { result ->
                val rooms = mutableListOf<RoomVO>()
                for (document in result) {
                    val room = document.toObject(RoomVO::class.java)
                    rooms.add(room)
                }
                callback.onDataReceived(rooms)
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents: $exception")
                callback.onError(exception)
            }
    }
}
