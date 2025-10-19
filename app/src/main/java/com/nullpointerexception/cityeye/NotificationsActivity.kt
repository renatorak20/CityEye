package com.nullpointerexception.cityeye

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.nullpointerexception.cityeye.data.NotificationsViewModel
import com.nullpointerexception.cityeye.databinding.ActivityNotificationsBinding

class NotificationsActivity : AppCompatActivity() {

    private lateinit var viewModel: NotificationsViewModel
    private lateinit var binding: ActivityNotificationsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        viewModel = ViewModelProvider(this)[NotificationsViewModel::class.java]

        Firebase.auth.currentUser?.let { viewModel.getUserFromDatabase(it.uid) }

        binding.backButton.setOnClickListener {
            finish()
        }

    }

    override fun onStart() {
        super.onStart()

        Firebase.auth.currentUser?.let { viewModel.getUserFromDatabase(it.uid) }
    }
}