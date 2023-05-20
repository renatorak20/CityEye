package com.nullpointerexception.cityeye

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import coil.Coil
import coil.request.ImageRequest
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.nullpointerexception.cityeye.data.ProblemDetailViewModel
import com.nullpointerexception.cityeye.databinding.ActivityProblemDetailBinding
import com.nullpointerexception.cityeye.entities.Message
import com.nullpointerexception.cityeye.ui.adapters.MessagesAdapter
import com.nullpointerexception.cityeye.util.ButtonObserver
import com.nullpointerexception.cityeye.util.OtherUtilities

class ProblemDetailActivity : AppCompatActivity() {

    private lateinit var viewModel: ProblemDetailViewModel
    private lateinit var binding: ActivityProblemDetailBinding
    private lateinit var chatAdapter: MessagesAdapter
    private lateinit var manager: LinearLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityProblemDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        viewModel = ViewModelProvider(this).get(ProblemDetailViewModel::class.java)

        viewModel.getProblem(intent.getStringExtra("problemID")!!)
        viewModel.getAnswer(intent.getStringExtra("problemID")!!)

        viewModel.getProblem().observe(this) { problem ->
            problem?.let {
                binding.title.text = it.title
                binding.description.text = it.description
                binding.address.text = it.address
                binding.date.text = it.epoch?.let { it1 -> OtherUtilities().getDateFromEpoch(it1) }

                Firebase.storage.reference.child("images/${it.imageName}").downloadUrl.addOnSuccessListener { url ->
                    val request = ImageRequest.Builder(this)
                        .data(url)
                        .target { drawable ->
                            binding.image.setImageDrawable(drawable)
                            binding.image.visibility = View.VISIBLE
                        }
                        .build()
                    Coil.imageLoader(this).enqueue(request)
                }

                binding.imageLoadingIndicator.hide()
            }

            if (problem.uid == Firebase.auth.uid) {
                manager = LinearLayoutManager(this)
                manager.reverseLayout = true
                manager.stackFromEnd = true
                binding.messageRecyclerView.adapter = chatAdapter
                binding.messageRecyclerView.layoutManager = manager
                binding.fab.visibility = View.VISIBLE
            }

        }

        val query = FirebaseFirestore.getInstance().collection("messages")
            .document(intent.getStringExtra("problemID")!!)
            .collection("problemMessages")
        val options: FirestoreRecyclerOptions<Message> = FirestoreRecyclerOptions.Builder<Message>()
            .setQuery(query, Message::class.java)
            .build()
        chatAdapter = MessagesAdapter(options, Firebase.auth.currentUser?.displayName)

        viewModel.getAnswer().observe(this) { answer ->
            if (answer == null) {
                binding.answer.text = getString(R.string.answer, getString(R.string.no_answer))
            } else {
                binding.answer.text = getString(R.string.answer, answer.response)
            }
        }

        BottomSheetBehavior.from(binding.standardBottomSheet).state =
            BottomSheetBehavior.STATE_COLLAPSED

        BottomSheetBehavior.from(binding.standardBottomSheet)
            .addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {

                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    if (slideOffset == 0.0f) {
                        binding.fab.show()
                    }
                }
            })

        binding.fab.setOnClickListener {
            BottomSheetBehavior.from(binding.standardBottomSheet).state =
                BottomSheetBehavior.STATE_EXPANDED
            binding.fab.hide()
        }

        binding.back.setOnClickListener {
            finish()
        }

        binding.messageEditText.addTextChangedListener(ButtonObserver(binding.sendButton))

        binding.sendButton.setOnClickListener {
            val text = binding.messageEditText.text.toString().trim()
            if (text.isNotEmpty()) {
                //val message = Message(text, Firebase.auth.currentUser?.displayName, System.currentTimeMillis())
                //viewModel.sendMessage(message, viewModel.getProblem().value?.problemID!!)
                binding.messageEditText.setText("")
            }
        }
    }

    override fun onStart() {
        super.onStart()
        chatAdapter.startListening()
    }

    override fun onStop() {
        super.onStop()
        chatAdapter.stopListening()
    }
}
