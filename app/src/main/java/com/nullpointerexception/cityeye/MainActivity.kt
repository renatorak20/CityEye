package com.nullpointerexception.cityeye

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.nullpointerexception.cityeye.databinding.ActivityMainBinding
import com.nullpointerexception.cityeye.ui.custom.ToolbarManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkIfUserIsLoggedIn()

        supportActionBar?.hide()

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_capture,
                R.id.navigation_list,
                R.id.navigation_places,
                R.id.navigation_events,
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.navView.setupWithNavController(navController)

        ToolbarManager(binding.mainToolbar, Firebase.auth.currentUser!!, this)

    }

    private fun checkIfUserIsLoggedIn() {
        if (Firebase.auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}