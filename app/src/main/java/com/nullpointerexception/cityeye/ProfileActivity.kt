package com.nullpointerexception.cityeye

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import coil.size.Scale
import coil.transform.CircleCropTransformation
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.nullpointerexception.cityeye.data.ProfileViewModel
import com.nullpointerexception.cityeye.databinding.ActivityProfileBinding
import com.nullpointerexception.cityeye.ui.adapters.RecyclerViewProfileAdapter

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val viewModel: ProfileViewModel by lazy { ViewModelProvider(this)[ProfileViewModel::class.java] }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setImage()

        viewModel.getUser().observe(this) {
            setInfo()
            viewModel.getUserProblems(viewModel.user.value!!.problems!!)
        }
        viewModel.getProblems().observe(this) {
            setProblems()
            binding.content.problems.pullToRefresh.isRefreshing = false

            if (it.isNotEmpty()) {
                binding.content.header.firstReport.alpha = 1f
            }
            if (it.size > 9) {
                binding.content.header.tenReport.alpha = 1f
            }
            if (it.size > 99) {
                binding.content.header.onehReport.alpha = 1f
            }
        }

        viewModel.getCurrentUser(Firebase.auth.currentUser!!.uid)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        binding.backButton.setOnClickListener {
            onBackPressed()
        }

        binding.content.problems.pullToRefresh.setOnRefreshListener {
            viewModel.getUserProblems(viewModel.getUser().value!!.problems!!)
        }

        binding.content.header.firstReport.setOnClickListener {
            Toast.makeText(this, getString(R.string.firstReport), Toast.LENGTH_SHORT).show()
        }

        binding.content.header.tenReport.setOnClickListener {
            Toast.makeText(this, getString(R.string.tenthReport), Toast.LENGTH_SHORT).show()
        }

        binding.content.header.onehReport.setOnClickListener {
            Toast.makeText(this, getString(R.string.onehReport), Toast.LENGTH_SHORT).show()
        }

        binding.content.header.citizenOfTheMonth.setOnClickListener {
            Toast.makeText(this, getString(R.string.citizenOfTheMonth), Toast.LENGTH_SHORT).show()
        }


    }

    override fun onBackPressed() {
        super.onBackPressed()
        finishAfterTransition()
    }

    fun setImage() {
        val imageUrl = Firebase.auth.currentUser!!.photoUrl
        if (imageUrl.toString() == "null") return
        binding.content.header.userImage.load(imageUrl) {
            transformations(CircleCropTransformation())
            size(1000)
            scale(Scale.FILL)
        }
    }

    fun setInfo() {
        binding.content.header.username.text = viewModel.user.value!!.displayName ?: "User471659"
        binding.content.header.city.text = "Zagreb"
    }

    fun setProblems() {
        val recyclerView = binding.content.problems.problemsRecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        val problemsSorted =
            viewModel.getProblems().value?.sortedWith(compareByDescending { it.timestamp })
        recyclerView.adapter = RecyclerViewProfileAdapter(
            this,
            ArrayList(problemsSorted)
        )
    }
}