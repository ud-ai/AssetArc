package com.example.assetarc

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(private val messages: List<ChatMessage>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        private const val VIEW_TYPE_USER = 0
        private const val VIEW_TYPE_GEMINI = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isUser) VIEW_TYPE_USER else VIEW_TYPE_GEMINI
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_USER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_user, parent, false)
            UserViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_gemini, parent, false)
            GeminiViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        if (holder is UserViewHolder) {
            holder.bind(message)
        } else if (holder is GeminiViewHolder) {
            holder.bind(message)
        }
    }

    override fun getItemCount(): Int = messages.size

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        super.onViewAttachedToWindow(holder)
        holder.itemView.alpha = 0f
        holder.itemView.animate().alpha(1f).setDuration(250).start()
    }

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvUserMessage)
        private val ivAvatar: ImageView = itemView.findViewById(R.id.ivUserAvatar)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tvUserTimestamp)
        fun bind(message: ChatMessage) {
            tvMessage.text = message.text
            tvTimestamp.text = android.text.format.DateFormat.format("hh:mm a", message.timestamp)
            // Optionally set avatar image here (already circular)
        }
    }

    class GeminiViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvGeminiMessage)
        private val ivAvatar: ImageView = itemView.findViewById(R.id.ivGeminiAvatar)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tvGeminiTimestamp)
        fun bind(message: ChatMessage) {
            tvMessage.text = message.text
            tvTimestamp.text = android.text.format.DateFormat.format("hh:mm a", message.timestamp)
            // Optionally set avatar image here (already circular)
        }
    }
} 