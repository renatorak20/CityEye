package com.nullpointerexception.cityeye.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.nullpointerexception.cityeye.R
import com.nullpointerexception.cityeye.databinding.MessageBinding
import com.nullpointerexception.cityeye.entities.Message

class MessagesAdapter(
    private val options: FirestoreRecyclerOptions<Message>,
    private val currentUserName: String?
) : FirestoreRecyclerAdapter<Message, MessagesAdapter.MessageViewHolder>(options) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {

        val view: View =
            LayoutInflater.from(parent.context).inflate(R.layout.message, parent, false)

        return MessageViewHolder(view)
    }

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = MessageBinding.bind(itemView)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int, model: Message) {
        with(holder.binding) {
            messageTextView.text = model.text
            messengerTextView.text = model.time.toString()
        }
    }
}