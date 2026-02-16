package com.dualtrack.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dualtrack.app.R
import com.dualtrack.app.databinding.FragmentHomeAthleteBinding

class AthleteHomeFragment : Fragment() {

    private var _b: FragmentHomeAthleteBinding? = null
    private val b get() = _b!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _b = FragmentHomeAthleteBinding.inflate(inflater, container, false)

        setupWelcome()
        setupQuickAdd()
        setupRecyclerViews()

        return b.root
    }

    private fun setupWelcome() {
        b.tvWelcomeTitle.text = "Welcome, User123!"
    }

    private fun setupQuickAdd() {
        b.btnQuickAdd.setOnClickListener {
            Toast.makeText(requireContext(), "Quick Add tapped", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerViews() {
        setupHorizontalList(b.rvForms, mockForms())
        setupHorizontalList(b.rvAtRisk, mockAtRiskAlerts())
        setupHorizontalList(b.rvCalendar, mockCalendar())
        setupHorizontalList(b.rvTasks, mockTasks())
        setupHorizontalList(b.rvWellness, mockWellness())
    }

    private fun setupHorizontalList(recyclerView: RecyclerView, items: List<HomeCard>) {
        recyclerView.isNestedScrollingEnabled = false
        recyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        recyclerView.adapter = HomeCardAdapter(items) { card ->
            handleCardClick(card)
        }
    }

    private fun handleCardClick(card: HomeCard) {
        when (card.title) {

            "Absence Form" -> {
                findNavController().navigate(R.id.absenceFormFragment)
            }

            else -> {
                Toast.makeText(requireContext(), card.title, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mockForms(): List<HomeCard> = listOf(
        HomeCard("Travel Form", "VSU @ Away"),
        HomeCard("Absence Form", "2 pending"),
        HomeCard("Pending", "3 forms awaiting"),
        HomeCard("Approved", "Last 7 days")
    )

    private fun mockAtRiskAlerts(): List<HomeCard> = listOf(
        HomeCard("Football Game", "Today · 23 min"),
        HomeCard("Midterm Exam", "Tomorrow · 19 hours"),
        HomeCard("Study Hall", "GPA watch"),
    )

    private fun mockCalendar(): List<HomeCard> = listOf(
        HomeCard("Monday", "Football Game"),
        HomeCard("Tuesday", "Midterm Exam"),
        HomeCard("Wednesday", "Practice"),
        HomeCard("Thursday", "Team Meeting")
    )

    private fun mockTasks(): List<HomeCard> = listOf(
        HomeCard("Homework", "CSCI 489 · Due Fri"),
        HomeCard("Workout", "Strength & Conditioning"),
        HomeCard("Practice", "VSU Soccer"),
        HomeCard("Study", "Film Review")
    )

    private fun mockWellness(): List<HomeCard> = listOf(
        HomeCard("Completion Chart", "This week"),
        HomeCard("Wellness Diaries", "Log today"),
        HomeCard("Eligibility Flags", "All clear"),
        HomeCard("Injury Overview", "No active injuries")
    )

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
