package com.example.mju_car

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.android.volley.AuthFailureError
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import org.json.JSONObject

class MyBackgroundService : Service() {

    private lateinit var db: FirebaseFirestore
    private lateinit var requestQueue: RequestQueue

    override fun onCreate() {
        super.onCreate()
        db = FirebaseFirestore.getInstance()
        requestQueue = Volley.newRequestQueue(this)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val identifierID = intent?.getStringExtra("identifierID")
        if (identifierID != null) {
            // identifierID가 존재하면 사용자 토큰을 가져옴
            fetchAuthorToken(identifierID)
        }
        return START_STICKY
    }

    // 사용자 토큰 읽어오기
    private fun fetchAuthorToken(identifierID: String) {
        db.document("ROOMS/$identifierID").get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val authorToken = document.getString("authorToken")
                    if (authorToken != null) {
                        // 토큰이 존재하면 푸시 알림을 전송
                        sendPushNotification(authorToken)
                    } else {
                        Log.e("FCM", "Author Token is null")
                    }
                } else {
                    Log.e("FCM", "Document with ID $identifierID does not exist")
                }
            }
            .addOnFailureListener { exception ->
                handleFirestoreException(exception)
            }
    }

    //FCM 메세지 전송
    private fun sendPushNotification(token: String) {
        val data = JSONObject()
        data.put("to", token)
        val notification = JSONObject()
        notification.put("title", "참여 신청 도착\uD83D\uDE95")
        notification.put("body", "함께 택시를 이용하고 싶어하는 멤버가 참여했어요!")
        notification.put("icon", "only_car")


        data.put("notification", notification)

        val sender = object : JsonObjectRequest(
            Method.POST,
            "https://fcm.googleapis.com/fcm/send",
            data,
            Response.Listener<JSONObject> { response ->
                // 성공적으로 알림 전송
                val responseData = response.toString()
                Log.d("FCM", "Notification sent successfully: $responseData")
            },
            Response.ErrorListener { error ->
                // 알림 전송 실패
                val errorMessage = error.message
                Log.e("FCM", "Failed to send notification: $errorMessage")
            }
        ) {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "key=AAAAzRbATJM:APA91bEsNP7w32Bl-fD9M55_Mw3T_0V01qGUWv4iSOyRzHsbxHDwCdxOpQP3CZ4iUxzqhN8sPWsOTHAYrLkhS0wD78_W51tQU0_WQnH3CdQqsOqPFZeUgtzskxT9S0UmTzuDKs2mFbNG" // 서버 키
                headers["Content-Type"] = "application/json"
                return headers
            }
        }
        requestQueue.add(sender)
    }

    // Firestore 예외를 처리
    private fun handleFirestoreException(exception: Exception) {
        when (exception) {
            is FirebaseFirestoreException -> {
                when (exception.code) {
                    FirebaseFirestoreException.Code.PERMISSION_DENIED -> {
                        Log.e("FCM", "Permission denied to access document")
                    }

                    FirebaseFirestoreException.Code.UNAVAILABLE -> {
                        Log.e("FCM", "Firestore unavailable")
                    }

                    else -> {
                        Log.e("FCM", "Unknown Firestore error", exception)
                    }
                }
            }

            else -> {
                Log.e("FCM", "Unknown error", exception)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        requestQueue.stop()
    }
}