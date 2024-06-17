package com.example.mju_car

import androidx.appcompat.app.AppCompatActivity

//이메일 인증

class SendMailActivity : AppCompatActivity() {

}
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        val binding = LoginBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        binding.btnMail.setOnClickListener { // '메일 전송' 버튼 클릭 시 메일 전송하기
//            sendEmail("테스트 뇸뇸")
//        }
//    }
//
//
//    // 메일 전송 메소드
//    @SuppressLint("QueryPermissionsNeeded")
//    private fun sendEmail(content: String) {
//        val emailAddress = "wopelt8@naver.com"
//        val title = "메일 제목입니다"
//
//        val intent = Intent(Intent.ACTION_SENDTO) // 메일 전송 설정
//            .apply {
//                type = "text/plain" // 데이터 타입 설정
//                data = Uri.parse("mailto:") // 이메일 앱에서만 인텐트 처리되도록 설정
//
//                putExtra(Intent.EXTRA_EMAIL, arrayOf<String>(emailAddress)) // 메일 수신 주소 목록
//                putExtra(Intent.EXTRA_SUBJECT, title) // 메일 제목 설정
//                putExtra(Intent.EXTRA_TEXT, content) // 메일 본문 설정
//            }
//
//        if (intent.resolveActivity(packageManager) != null) {
//            startActivity(Intent.createChooser(intent, "메일 전송하기"))
//        } else {
//            Toast.makeText(this, "메일을 전송할 수 없습니다", Toast.LENGTH_LONG).show()
//        }
//    }
//}