package com.nullpointerexception.cityeye.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.nullpointerexception.cityeye.R
import com.nullpointerexception.cityeye.databinding.UserItemBinding
import com.nullpointerexception.cityeye.entities.User

class RecyclerViewLeaderboard(
    val context: Context,
    val users: List<User>
) :
    RecyclerView.Adapter<RecyclerViewLeaderboard.UserViewHolder>() {

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = UserItemBinding.bind(view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        return UserViewHolder(
            LayoutInflater.from(context).inflate(R.layout.user_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]

        with(holder.binding) {
            place.text = (position + 4).toString()

            if (user.photoUrl != null) {
                image.load(user.photoUrl) {
                    transformations(CircleCropTransformation())
                    error(context.getDrawable(R.drawable.userimage))
                    placeholder(context.getDrawable(R.drawable.userimage))
                }
            } else {
                image.setImageDrawable(context.getDrawable(R.drawable.userimage))
            }

            username.text = user.displayName
            points.text = "${(user.problems?.size?.times(100)).toString()}pts"
        }


    }

    override fun getItemCount() = users.size

}