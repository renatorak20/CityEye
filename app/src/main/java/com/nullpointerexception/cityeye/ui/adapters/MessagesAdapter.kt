package com.nullpointerexception.cityeye.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.auth.FirebaseUser
import com.nullpointerexception.cityeye.R
import com.nullpointerexception.cityeye.databinding.MessageBinding
import com.nullpointerexception.cityeye.databinding.MessageMeBinding
import com.nullpointerexception.cityeye.entities.Message
import java.text.SimpleDateFormat
import java.util.Date


const val USER = 0
const val ADMIN = 1

class MessagesAdapter(
    options: FirestoreRecyclerOptions<Message>,
    private val currentUser: FirebaseUser?,
    private val context: Context
) : FirestoreRecyclerAdapter<Message, RecyclerView.ViewHolder>(options) {

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position).userID) {
            currentUser?.uid -> USER
            else -> ADMIN
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        when (viewType) {
            USER -> {
                return MessageMeViewHolder(
                    LayoutInflater.from(parent.context).inflate(R.layout.message_me, parent, false)
                )
            }

            else -> {
                return MessageViewHolder(
                    LayoutInflater.from(parent.context).inflate(R.layout.message, parent, false)
                )
            }
        }

    }

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = MessageBinding.bind(itemView)

        fun bind(message: Message) {
            with(binding) {

                messengerTextView.text = "City admin, ${getFullTime(message.time!!)}"
                messengerImageView.setImageDrawable(context.getDrawable(R.drawable.userimage))
                messageTextView.text = message.text

            }
        }
    }

    inner class MessageMeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = MessageMeBinding.bind(itemView)

        fun bind(message: Message) {
            with(binding) {
                messageTextView.text = message.text
                messengerTextView.text = "Me, ${getFullTime(message.time!!)}"
                Glide.with(context).load(currentUser?.photoUrl).transform(CircleCrop())
                    .into(binding.messengerImageView)
            }
        }

    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, model: Message) {

        when (holder) {
            is MessageViewHolder -> holder.bind(getItem(position))
            is MessageMeViewHolder -> holder.bind(getItem(position))
        }
    }

    fun getFullTime(epoch: Int): String {
        val date = Date(epoch * 1000L)

        val dateFormat = SimpleDateFormat("HH:mm dd.MM.yyyy")
        val formattedDateTime = dateFormat.format(date)
        return formattedDateTime
    }
}