package com.example.mju_car

import android.util.Log
import com.android.volley.AuthFailureError
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.json.JSONException
import org.json.JSONObject

class MyFirebaseMessagingService : FirebaseMessagingService() {
    // 새로운 토큰이 생성될 때 호출되는 메소드
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Refreshed token: \$token")

        // 서버에 토큰을 전송하는 로직 호출
        sendRegistrationToServer(token)
    }

    // 새로운 메시지가 수신될 때 호출되는 메소드
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // 수신된 푸시 알림 데이터 처리
        handleNotification(remoteMessage)
    }

    // 서버에 FCM 토큰을 등록하는 메소드
    private fun sendRegistrationToServer(token: String) {
        // 서버 API 엔드포인트 URL
        val url = "https://your-server.com/register-token"

        // Volley 라이브러리 사용
        val requestQueue = Volley.newRequestQueue(this)

        // JSON 형식의 데이터 생성
        val jsonBody = JSONObject()
        try {
            jsonBody.put("token", token)
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        // POST 요청 생성
        val request: JsonObjectRequest = object : JsonObjectRequest(
            Method.POST, url, jsonBody,
            Response.Listener { response: JSONObject ->
                Log.d(
                    "FCM",
                    "Token registration successful: $response"
                )
            },
            Response.ErrorListener { error: VolleyError ->
                Log.e(
                    "FCM",
                    "Token registration failed: " + error.message
                )
            }
        ) {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                // 필요한 경우 헤더 추가
                return HashMap()
            }
        }

        // 요청 추가
        requestQueue.add(request)
    }

    // 푸시 알림 데이터를 처리하는 메소드
    private fun handleNotification(remoteMessage: RemoteMessage) {
        // 수신된 푸시 알림 데이터 처리 로직 구현
        // 예: 알림 표시, 데이터 저장, 백그라운드 작업 실행 등
        Log.d("FCM", "Notification received: " + remoteMessage.getNotification())

        // 푸시 알림 전송
        sendPushNotification(remoteMessage.getData()["token"])
    }

    // FCM 푸시 알림을 전송하는 메소드
    private fun sendPushNotification(token: String?) {
        // 데이터 생성
        val data = JSONObject()
        try {
            data.put("title", "참여 신청 도착\uD83D\uDE95 ")
            data.put("body", "함께 택시를 이용하고 싶어하는 멤버가 참여했어요 !")
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        // Volley 라이브러리 사용
        val requestQueue = Volley.newRequestQueue(this)

        // POST 요청 생성
        val sender: JsonObjectRequest = object : JsonObjectRequest(
            Method.POST, "https://fcm.googleapis.com/fcm/send", data,
            Response.Listener { response: JSONObject ->
                // 성공적으로 알림 전송
                Log.d("FCM", "Push notification sent successfully: $response")
            },
            Response.ErrorListener { error: VolleyError ->
                // 알림 전송 실패
                Log.e("FCM", "Push notification failed: " + error.message)
            }
        ) {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                val headers: MutableMap<String, String> = HashMap()
                headers["Authorization"] ="key=AAAAzRbATJM:APA91bEsNP7w32Bl-fD9M55_Mw3T_0V01qGUWv4iSOyRzHsbxHDwCdxOpQP3CZ4iUxzqhN8sPWsOTHAYrLkhS0wD78_W51tQU0_WQnH3CdQqsOqPFZeUgtzskxT9S0UmTzuDKs2mFbNG"
                headers["Content-Type"] = "application/json"
                return headers
            }
        }
        // 요청 추가
        requestQueue.add(sender)
    }
}