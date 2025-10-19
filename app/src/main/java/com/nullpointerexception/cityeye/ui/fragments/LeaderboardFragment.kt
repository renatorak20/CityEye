package com.nullpointerexception.cityeye.ui.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.nullpointerexception.cityeye.data.SharedViewModel
import com.nullpointerexception.cityeye.databinding.FragmentLeaderboardBinding
import java.time.Duration
import java.time.LocalDateTime
import java.time.Year

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

    @SuppressLint("SetTextI18n", "UseCompatLoadingForDrawables")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.getAllUsers()

        viewModel.getUsers().observe(viewLifecycleOwner) {
            binding.pullToRefresh.isRefreshing = false
        }

        binding.pullToRefresh.setOnRefreshListener {
            viewModel.getAllUsers()
        }

        object : CountDownTimer(999000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val currentTime = LocalDateTime.now()
                val year = Year.of(currentTime.year)
                val endOfMonth = currentTime.withDayOfMonth(currentTime.month.length(year.isLeap))
                    .withHour(23).withMinute(59).withSecond(59)
                val duration = Duration.between(currentTime, endOfMonth)

                val days = duration.toDays()
                val hours = duration.minusDays(days).toHours()
                val minutes = duration.minusDays(days).minusHours(hours).toMinutes()

                val countdownText = "$days d : $hours h : $minutes m"
                binding.timeLeft.text = countdownText
            }

            override fun onFinish() {

            }
        }.start()
    }


}