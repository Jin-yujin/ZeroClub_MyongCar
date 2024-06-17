package com.example.mju_car

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mju_car.data.AuthService
import com.example.mju_car.data.StorageService
import com.example.mju_car.data.vo.ChatVO
import com.example.mju_car.data.vo.RoomVO
import com.example.mju_car.data.vo.UserVO
import com.example.mju_car.databinding.ActivityChatBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.text.DecimalFormat

class ChatActivity : AppCompatActivity() {
    // DI로 주입된 AuthService와 StorageService 객체
    private val authService     : AuthService by inject()
    private val storageService  : StorageService by inject()
    private lateinit var binding: ActivityChatBinding
    private val chatList = ArrayList<ChatVO>()  // 채팅 메시지 리스트
    private lateinit var chatAdapter: ChatAdapter
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private var chatListenerRegistration: ListenerRegistration? = null
    private var roomListenerRegistration: ListenerRegistration? = null
    private lateinit var currentUser: FirebaseUser
    private lateinit var roomId: String
    private lateinit var currentUserNickName: String
    val db = Firebase.firestore
    private lateinit var user: UserVO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 이메일로 사용자 정보를 가져옴
        val email = authService.email()
        lifecycleScope.launch {
            if (email != null) {
                val data = storageService.findUser(email)
                if (data != null) {
                    user = data
                    Log.d("USER >> ", user.toString())
                }
            }
        }

        // comp_list_item.xml의 뷰들 참조
        val tvFrom = binding.root.findViewById<TextView>(R.id.tvFrom)
        val tvTo = binding.root.findViewById<TextView>(R.id.tvTo)
        val tvDate = binding.root.findViewById<TextView>(R.id.tvDate)
        val tvWriter = binding.root.findViewById<TextView>(R.id.tvWriter)
        val tvPartner = binding.root.findViewById<TextView>(R.id.tvPartner)

        // 전달받은 데이터를 뷰에 설정
        val extras = intent.extras ?: return
        tvFrom.text = extras.getString("from")
        tvTo.text = extras.getString("to")
        tvDate.text = extras.getString("dateInfo")
        tvWriter.text = extras.getString("owner")
        val initialParticipants = "${extras.getInt("curPartner")} / ${extras.getInt("maxPartner")}"
        tvPartner.text = initialParticipants
        val departureTimeStamp = extras.getLong("departureTimeStamp")
        val identifierID = extras.getString("identifierID")

        // 상태바 색상 변경
        window.statusBarColor = ContextCompat.getColor(this, R.color.navy)

        // 현재 사용자 정보 가져오기
        currentUser = FirebaseAuth.getInstance().currentUser
            ?: throw IllegalStateException("User not logged in")    // 현재 사용자의 닉네임을 가져옴
        getUserNickName(currentUser.uid) { nickName ->
            currentUserNickName = nickName
            initRecyclerView()
            initSendButton()
            loadChatsFromFirestore()
            addRoomListener(identifierID!!, tvPartner) // 이용자 수 업데이트
        }

        // 카카오 택시 이용하기 버튼 클릭 시
        binding.btnKakaoT.setOnClickListener {
            openKakaoT(this)
        }

        // 채팅방 ID 받아오기
        roomId = intent?.getStringExtra("roomId") ?: ""
        if (roomId.isEmpty()) {
            // roomID오류 시 종료
            Log.e("ChatActivity", "roomId is empty")
            finish()
            return
        }

        // 사용자 닉네임 받기
        val userNickName = intent.getStringExtra("userNickName") ?: ""
        if (userNickName.isNotEmpty()) {
            sendSystemMessage(userNickName, true)
        }
        val cancelButton = binding.cancelButton
        val currentTime = System.currentTimeMillis()

        if (currentTime <= departureTimeStamp) {
            cancelButton.text = "참여 취소"

            // cancelButton 클릭 시 처리
            cancelButton.setOnClickListener {

                if (departureTimeStamp - System.currentTimeMillis() <= 600000) {
                    Toast.makeText(
                        this,
                        "출발 시간 10분 전엔 취소가 불가능합니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    db.collection("ROOMS").document(identifierID!!)
                        .get()
                        .addOnSuccessListener { document ->
                            if (document != null && document.exists()) {
                                val room = document.toObject(RoomVO::class.java)
                                Log.d("Chat","$room")

                                db.collection("USERS")
                                    .whereEqualTo("addedByUser", currentUser.uid)
                                    .get()
                                    .addOnSuccessListener { documents ->
                                        for (document in documents) {
                                            val user = document.toObject(UserVO::class.java)
                                            val userNickName = user.nickName
                                            val currentUserScore = user.score

                                            if (room?.owner == "[$currentUserScore] $userNickName") {
                                                if (room?.currentNumberOfPeople == 1 || room?.currentNumberOfPeople == 0) {
                                                    AlertDialog.Builder(this)
                                                        .setMessage("예약을 취소하시겠습니까?")
                                                        .setPositiveButton("확인") { _, _ ->
                                                            //room 삭제
                                                            val roomDocRef = db.collection("ROOMS").document(identifierID)
                                                            roomDocRef.delete()
                                                            updateUser(db)
                                                            Toast.makeText(
                                                                this,
                                                                "참여가 취소되었습니다",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                            navigateToHome()
                                                        }
                                                        .setNegativeButton("취소", null)
                                                        .show()
                                                } else {
                                                    Toast.makeText(
                                                        this,
                                                        "방장은 방을 나갈 수 없습니다.",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            } else {
                                                cancelPlan()
                                            }
                                        }
                                    }
                            } else {
                                // room 문서가 존재하지 않을 경우 처리
                                Log.d("ChatActivity", "No such document")
                            }
                        }
                        .addOnFailureListener { exception ->
                            // 예외 처리
                            Log.d("ChatActivity", "get failed with ", exception)
                        }
                }
            }
        } else {
            cancelButton.text = "정산하기"
            cancelButton.setOnClickListener {
                // 정산하기 버튼 클릭 시 처리할 로직
                val etPrice = EditText(this)
                etPrice.inputType = InputType.TYPE_CLASS_NUMBER
                val dialog = AlertDialog.Builder(this)
                    .setTitle("정산하기")
                    .setMessage("금액을 입력하세요")
                    .setView(etPrice)
                    .setPositiveButton("정산하기", null) // 리스너를 나중에 설정하기 위해 null로 설정
                    .setNegativeButton("취소", null)
                    .create()
                // 대화상자 표시 후에 PositiveButton의 리스너 설정
                dialog.setOnShowListener {
                    val button: Button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    button.setOnClickListener {
                        val priceInput = etPrice.text.toString()
                        if (priceInput.isEmpty()) {
                            Toast.makeText(this, "금액을 입력하세요", Toast.LENGTH_SHORT).show()
                        } else {
                            try {
                                val totPrice = Integer.parseInt(priceInput)
                                var curPartner = extras.getInt("curPartner")
                                if (curPartner < 1) curPartner = 1
                                val divPrice = totPrice / curPartner

                                // user 객체가 null인지 확인
                                if (::user.isInitialized) {
                                    val bankName = user.bankName
                                    val accountNumber = user.accountNumber
                                    val accountHolder = user.accountHolder
                                    val kakaoPayUrl = user.kakaoPayUrl
                                    val dec = DecimalFormat("#,##0")
                                    Log.d("divPrice >>>> ", "$divPrice / $totPrice")
                                    val msg = """
                                        정산해요🚕 
                                        총 금액: ${dec.format(totPrice)}원
                                        보낼 금액: ${dec.format(divPrice)}원
                                        
                                        예금주: $accountHolder 
                                        $bankName $accountNumber 
                                        
                                        카카오페이 URL
                                        $kakaoPayUrl
                                        
                                        정산 후 확인 채팅 부탁드립니다~!
                                    """.trimIndent()

                                    // chatVO 객체 생성 및 Firestore에 저장
                                    val chatVO = ChatVO(
                                        message = msg,
                                        nickName = currentUserNickName,
                                        departureTimeStamp = System.currentTimeMillis(),
                                        roomId = roomId // 채팅방 ID 추가
                                    )
                                    saveChatToFirestore(chatVO)
                                    dialog.dismiss() // 대화상자 닫기
                                } else {
                                    Toast.makeText(this, "사용자 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: NumberFormatException) {
                                Toast.makeText(this, "유효한 금액을 입력하세요", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                dialog.show()
            }
        }
    }


    // Firestore에서 방의 금액 업데이트하는 함수
    private fun updateRoomPrice(identifierID: String, totPrice: Int) {
        val roomRef = db.collection("ROOMS").document(identifierID)
        roomRef.update("priceTake", totPrice)
            .addOnSuccessListener {
                Log.d("ChatActivity", "Room price updated successfully")
            }
            .addOnFailureListener { e ->
                Log.e("ChatActivity", "Error updating room price", e)
            }
    }

    private fun cancelPlan() {
        val extras = intent.extras ?: return
        val identifierID = extras.getString("identifierID")

        AlertDialog.Builder(this)
            .setMessage("예약을 취소하시겠습니까?")
            .setPositiveButton("확인") { _, _ ->
                updateRoomAndUser(identifierID!!)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun updateRoomAndUser(identifierID: String) {
        val db = Firebase.firestore

        // 방 정보 가져오기
        db.collection("ROOMS").document(identifierID)
            .get()
            .addOnSuccessListener { document ->
                val room = document.toObject(RoomVO::class.java)

                if (document.exists() && room != null) {
                    // 현재 방의 참여자 수 감소 및 방 정보 업데이트
                    updateRoom(document, room)

                    // 사용자 정보 업데이트
                    updateUser(db)
                } else {
                    Log.e("ChatActivity", "Room document does not exist or RoomVO object is null")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("ChatActivity", "Error getting room document: $exception")
                Toast.makeText(this, "룸 정보 로딩에 실패했습니다", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateRoom(document: DocumentSnapshot, room: RoomVO) {
        // 현재 방의 참여자 수 감소
        room.currentNumberOfPeople -= 1

        // 방 정보 업데이트
        val updatedParticipants = room.participants.filter { participant -> participant.addedByUser != currentUser.uid }
        document.reference.update(
            mapOf(
                "currentNumberOfPeople" to room.currentNumberOfPeople,
                "participants" to updatedParticipants
            )
        )
            .addOnSuccessListener {
                Log.d("ChatActivity", "Room updated successfully")
            }
            .addOnFailureListener { exception ->
                Log.e("ChatActivity", "Error updating room: $exception")
                Toast.makeText(this, "룸 정보 업데이트에 실패했습니다", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateUser(db: FirebaseFirestore) {
        currentUser.let { user ->
            db.collection("USERS").document(user.uid)
                .update("participatedRoomIds", FieldValue.arrayRemove(user.uid))
                .addOnSuccessListener {
                    sendSystemMessage(currentUserNickName, false)
                    Toast.makeText(this, "참여가 취소되었습니다", Toast.LENGTH_SHORT).show()
                    navigateToHome()
                }
                .addOnFailureListener { exception ->
                    Log.e("ChatActivity", "Error updating user participatedRoomIds: $exception")
                    Toast.makeText(this, "예약 취소에 실패했습니다", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

    private fun getUserNickName(uid: String, callback: (String) -> Unit) {
        val db = Firebase.firestore
        db.collection("USERS")
            .whereEqualTo("addedByUser", uid)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val userVO = document.toObject(UserVO::class.java)
                    callback(userVO.nickName)
                    return@addOnSuccessListener
                }
                // 사용자를 찾지 못한 경우 기본값으로 uid를 사
                callback(uid)
            }
            .addOnFailureListener { exception ->
                Log.w("ChatActivity", "Error getting documents: $exception")
                // 에러 발생 시 기본값 uid
                callback(uid)
            }
    }


  //카카오T 실행
    private fun openKakaoT(context: Context) {
        val packageName = "com.kakao.taxi"
        var intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if(intent == null) {
            val link = "https://play.google.com/store/apps/details?id=$packageName"
            intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(link)
            }
            context.startActivity(intent)
            return
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }


    // 리사이클러뷰 초기화
    private fun initRecyclerView() {
        chatAdapter = ChatAdapter(chatList, currentUserNickName)
        binding.chatListview.adapter = chatAdapter
        binding.chatListview.layoutManager = LinearLayoutManager(this)
    }

    // 전송 버튼 초기화
    private fun initSendButton() {
        binding.sendButton.setOnClickListener {
            val message = binding.textSendBox.text.toString()
            if (message.isNotEmpty()) {
                val chatVO = ChatVO(
                    message = message,
                    nickName = currentUserNickName,
                    departureTimeStamp = System.currentTimeMillis(),
                    roomId = roomId // 채팅방 ID 추가
                )
                saveChatToFirestore(chatVO)
                binding.textSendBox.text.clear()
            }
        }
    }

    // 채팅 메시지를 Firestore에 저장
    private fun saveChatToFirestore(chat: ChatVO) {
        firestore.collection("CHATS")
            .document(roomId) // 채팅방 ID로 문서 생성
            .collection("messages") // 메시지 컬렉션
            .add(chat)
            .addOnSuccessListener {
                // Firestore에 저장되면 UI 업데이트는 SnapshotListener를 통해 처리
            }
            .addOnFailureListener { e ->
            // 에러 처리
            }
    }

    // Firestore에서 채팅 메시지를 로드
    private fun loadChatsFromFirestore() {
        chatListenerRegistration = firestore.collection("CHATS")
            .document(roomId) // 채팅방 ID로 문서 선택
            .collection("messages") // 메시지 컬렉션
            .orderBy("departureTimeStamp") // 시간순으로 정렬
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    // 에러 처리
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    chatList.clear()  // 기존 메시지 초기화
                    for (document in snapshot.documents) {
                        val chat = document.toObject(ChatVO::class.java)
                        if (chat != null) {
                            chatList.add(chat)  // 메시지 추가
                        }
                    }
                    chatAdapter.notifyDataSetChanged()  //어댑터 갱신
                    binding.chatListview.scrollToPosition(chatList.size - 1) //마지막 메세지로 스크롤
                }
            }
    }

    // Firestore에서 방 정보 변경 감지
    private fun addRoomListener(identifierID: String, tvPartner: TextView) {
        roomListenerRegistration = db.collection("ROOMS").document(identifierID)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("ChatActivity", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val room = snapshot.toObject(RoomVO::class.java)
                    Log.d("ChatActivity", "Room data: $room")
                    if (room != null) {
                        val currentNumberOfPeople = room.currentNumberOfPeople
                        tvPartner.text = "$currentNumberOfPeople / ${room.desiredNumberOfPeople}"
                        Log.d("ChatActivity", "Updated participants: $currentNumberOfPeople / ${room.desiredNumberOfPeople}")
                    } else {
                        Log.d("ChatActivity", "Room is null after converting to RoomVO")
                    }
                } else {
                    Log.d("ChatActivity", "Current data: null")
                }
            }
    }

    // 시스템 메시지 전송
    private fun sendSystemMessage(userNickName: String, isJoined: Boolean) {
        val systemMessage = if (isJoined) {
            "[$userNickName]님이 참여하였습니다."
        } else {
            "[$userNickName]님이 참여를 취소하였습니다."
        }
        val chatVO = ChatVO(
            message = systemMessage,
            nickName = "SYSTEM_MESSAGE",
            departureTimeStamp = System.currentTimeMillis(),
            roomId = roomId
        )
        saveChatToFirestore(chatVO)
    }

    override fun onDestroy() {
        super.onDestroy()
        chatListenerRegistration?.remove()
        roomListenerRegistration?.remove()
    }
}