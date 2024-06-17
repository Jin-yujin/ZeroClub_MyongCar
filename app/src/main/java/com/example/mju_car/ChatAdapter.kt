package com.example.mju_car

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.mju_car.data.vo.ChatVO
import com.example.mju_car.databinding.MyMsgboxBinding
import com.example.mju_car.databinding.OtherMsgboxBinding
import com.example.mju_car.databinding.SystemMessageItemBinding
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(private val chatList: List<ChatVO>, private val currentUserNickName: String) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_LEFT = 0
        private const val VIEW_TYPE_RIGHT = 1
        private const val VIEW_TYPE_SYSTEM = 2
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_LEFT -> {
                val binding = OtherMsgboxBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                LeftViewHolder(binding)
            }
            VIEW_TYPE_RIGHT -> {
                val binding = MyMsgboxBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                RightViewHolder(binding)
            }
            VIEW_TYPE_SYSTEM -> {
                val binding = SystemMessageItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                SystemViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val chat = chatList[position]
        when (holder) {
            is LeftViewHolder -> holder.bind(chat)
            is RightViewHolder -> holder.bind(chat)
            is SystemViewHolder -> holder.bind(chat)
        }
    }

    // 항목의 수를 반환하는 메소드
    override fun getItemCount() = chatList.size

    // 항목의 뷰 타입을 결정하는 메소드
    override fun getItemViewType(position: Int): Int {
        val chat = chatList[position]
        return when {
            chat.nickName == "SYSTEM_MESSAGE" -> VIEW_TYPE_SYSTEM
            chat.nickName == currentUserNickName -> VIEW_TYPE_RIGHT
            else -> VIEW_TYPE_LEFT
        }
    }

    inner class LeftViewHolder(private val binding: OtherMsgboxBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(chat: ChatVO) {
            binding.tvName.text = chat.nickName
            binding.tvMsg.text = chat.message
            binding.tvTime.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(chat.departureTimeStamp))

            // 메시지에 포함된 URL을 자동으로 하이퍼링크로 인식
            Linkify.addLinks(binding.tvMsg, Linkify.WEB_URLS)

            // 메시지 텍스트뷰 길게 클릭 시 복사 기능 구현
            binding.tvMsg.setOnLongClickListener {
                copyText(chat.message)
                true
            }
        }

        private fun copyText(text: String) {
            val clipboardManager = itemView.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText("copied_text", text)
            clipboardManager.setPrimaryClip(clipData)
            Toast.makeText(itemView.context, "메시지가 복사되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    // 본인의 메시지를 표시하는 뷰 홀더 클래스
    inner class RightViewHolder(private val binding: MyMsgboxBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(chat: ChatVO) {
            binding.tvName.text = ""// -> 사용자 본인 닉네임은 출력 안함
            binding.tvMsg.text = chat.message
            binding.tvTime.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(chat.departureTimeStamp))

            // 메시지에 포함된 URL을 자동으로 하이퍼링크로 인식
            Linkify.addLinks(binding.tvMsg, Linkify.WEB_URLS)

            // 메시지 텍스트뷰 길게 클릭 시 복사 기능 구현
            binding.tvMsg.setOnLongClickListener {
                copyText(chat.message)
                true
            }
        }

        private fun copyText(text: String) {
            val clipboardManager = itemView.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText("copied_text", text)
            clipboardManager.setPrimaryClip(clipData)
            Toast.makeText(itemView.context, "메시지가 복사되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    // 시스템 메시지를 표시하는 뷰 홀더 클래스
    inner class SystemViewHolder(private val binding: SystemMessageItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(chat: ChatVO) {
            binding.systemMessage.text = chat.message
        }
    }
}