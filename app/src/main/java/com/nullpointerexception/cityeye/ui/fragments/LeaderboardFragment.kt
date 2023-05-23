package com.nullpointerexception.cityeye.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import coil.transform.CircleCropTransformation
import com.nullpointerexception.cityeye.R
import com.nullpointerexception.cityeye.data.SharedViewModel
import com.nullpointerexception.cityeye.databinding.FragmentLeaderboardBinding
import com.nullpointerexception.cityeye.ui.adapters.RecyclerViewLeaderboard

class LeaderboardFragment : Fragment() {

    private lateinit var binding: FragmentLeaderboardBinding
    private lateinit var viewModel: SharedViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        viewModel = ViewModelProvider(requireActivity())[SharedViewModel::class.java]

        binding = FragmentLeaderboardBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.getAllUsers()

        viewModel.getUsers().observe(viewLifecycleOwner) {
            binding.pullToRefresh.isRefreshing = false

            val sortedUsers = it.sortedBy { user -> user.problems?.size }
                .filter { user -> user.problems?.isNotEmpty()!! }.reversed()

            val adapter =
                RecyclerViewLeaderboard(requireContext(), sortedUsers.subList(3, sortedUsers.size))

            binding.recyclerView.adapter = adapter
            binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
            binding.recyclerView.itemAnimator = null

            binding.firstPlaceImage.load(sortedUsers[0].photoUrl) {
                transformations(CircleCropTransformation())
                placeholder(requireContext().getDrawable(R.drawable.userimage))
                error(requireContext().getDrawable(R.drawable.userimage))
            }
            binding.secondPlace.load(sortedUsers[1].photoUrl) {
                transformations(CircleCropTransformation())
                placeholder(requireContext().getDrawable(R.drawable.userimage))
                error(requireContext().getDrawable(R.drawable.userimage))
            }
            binding.thirdPlace.load(sortedUsers[2].photoUrl) {
                transformations(CircleCropTransformation())
                placeholder(requireContext().getDrawable(R.drawable.userimage))
                error(requireContext().getDrawable(R.drawable.userimage))
            }

            binding.thirdName.text = sortedUsers[2].displayName
            binding.thirdPoints.text = "${sortedUsers[2].problems?.size!! * 100}pts"

            binding.secondName.text = sortedUsers[1].displayName
            binding.secondPoints.text = "${sortedUsers[1].problems?.size!! * 100}pts"

            binding.firstName.text = sortedUsers[0].displayName
            binding.firstPoints.text = "${sortedUsers[0].problems?.size!! * 100}pts"

        }

        binding.pullToRefresh.setOnRefreshListener {
            viewModel.getAllUsers()
        }

    }


}