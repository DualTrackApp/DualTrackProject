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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class AthleteHomeFragment : Fragment() {

    private var _b: FragmentHomeAthleteBinding? = null
    private val b get() = _b!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var announcementsReg: com.google.firebase.firestore.ListenerRegistration? = null
    private var eventsReg: com.google.firebase.firestore.ListenerRegistration? = null
    private var announcementsCount: Int = 0
    private var eventsCount: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _b = FragmentHomeAthleteBinding.inflate(inflater, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupWelcome()
        loadTeamStatus()
        listenForAnnouncementsRow()
        setupQuickAdd()
        setupRecyclerViews()

        b.tvWelcomeTitle.setOnLongClickListener {
            findNavController().navigate(R.id.accountFragment)
            true
        }

        b.includeLogo.root.setOnClickListener {
            findNavController().navigate(R.id.accountFragment)
        }

        return b.root
    }

    private fun setupWelcome() {
        val email = auth.currentUser?.email
        val name = email?.substringBefore("@").orEmpty().ifBlank { "Athlete" }
        b.tvWelcomeTitle.text = "Welcome, $name!"
    }

    private fun loadTeamStatus() {
        val uid = auth.currentUser?.uid
        val email = auth.currentUser?.email

        if (uid.isNullOrBlank()) {
            b.tvTeamStatus.text = "No Team Assigned"
            return
        }

        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { snap ->
                val teamName = snap.getString("teamName")
                val teamId = snap.getString("teamId")

                if (!teamName.isNullOrBlank()) {
                    b.tvTeamStatus.text = "Team: $teamName"
                    return@addOnSuccessListener
                }

                if (!teamId.isNullOrBlank()) {
                    db.collection("teams").document(teamId)
                        .get()
                        .addOnSuccessListener { teamSnap ->
                            val resolvedName = teamSnap.getString("teamName")
                            b.tvTeamStatus.text =
                                if (!resolvedName.isNullOrBlank()) "Team: $resolvedName" else "Team Assigned"
                        }
                        .addOnFailureListener {
                            b.tvTeamStatus.text = "Team Assigned"
                        }
                    return@addOnSuccessListener
                }

                if (!email.isNullOrBlank()) {
                    db.collection("teamInvites")
                        .whereEqualTo("email", email)
                        .whereEqualTo("status", "pending")
                        .limit(1)
                        .get()
                        .addOnSuccessListener { qs ->
                            val invite = qs.documents.firstOrNull()
                            val invitedTeamName = invite?.getString("teamName")
                            b.tvTeamStatus.text = if (!invitedTeamName.isNullOrBlank()) {
                                "Invitation pending: $invitedTeamName"
                            } else {
                                "Invitation pending"
                            }
                        }
                        .addOnFailureListener {
                            b.tvTeamStatus.text = "No Team Assigned"
                        }
                } else {
                    b.tvTeamStatus.text = "No Team Assigned"
                }
            }
            .addOnFailureListener {
                b.tvTeamStatus.text = "No Team Assigned"
            }
    }


    private fun setupQuickAdd() {
        b.btnQuickAdd.setOnClickListener {
            Toast.makeText(requireContext(), "Quick Add tapped", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerViews() {
        setupHorizontalList(b.rvForms, mockForms())

        setupHorizontalList(b.rvAtRisk, mockAtRiskAlerts()) {
            findNavController().navigate(R.id.action_athleteHome_to_atRiskForm)
        }

        b.rvCalendar.isNestedScrollingEnabled = false
        b.rvCalendar.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        b.rvCalendar.adapter = HomeCardAdapter(mockCalendar()) { card ->
            val dayMillis = dayMillisForLabel(card.title)
            val args = Bundle().apply { putLong("dayMillis", dayMillis) }
            findNavController().navigate(R.id.action_athleteHome_to_dayEvents, args)
        }

        setupHorizontalList(b.rvWellness, mockWellness())
    }

    private fun setupHorizontalList(
        recyclerView: RecyclerView,
        items: List<HomeCard>,
        onCardClickOverride: (() -> Unit)? = null
    ) {
        recyclerView.isNestedScrollingEnabled = false
        recyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        recyclerView.adapter = HomeCardAdapter(items) { card ->
            if (onCardClickOverride != null) {
                onCardClickOverride.invoke()
            } else {
                handleCardClick(card)
            }
        }
    }

    private fun handleCardClick(card: HomeCard) {
        when (card.title) {
            "Absence Form" -> findNavController().navigate(R.id.absenceFormFragment)
            "Injury Report" -> findNavController().navigate(R.id.injuryFormFragment)
            "Wellness Check" -> findNavController().navigate(R.id.wellnessFormFragment)
            "Academic Check" -> findNavController().navigate(R.id.academicFormFragment)
            "My Submissions" -> findNavController().navigate(R.id.athleteSubmissionsFragment)

            "Completion Chart", "Wellness Diaries", "Eligibility Flags" -> {
                val bundle = Bundle().apply { putString("cardTitle", card.title) }
                findNavController().navigate(R.id.progressWellnessFormFragment, bundle)
            }

            "Injury Overview" -> findNavController().navigate(R.id.injuryFormFragment)

            else -> Toast.makeText(requireContext(), card.title, Toast.LENGTH_SHORT).show()
        }
    }

    private fun dayMillisForLabel(label: String): Long {
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val dow = when (label) {
            "Mon" -> Calendar.MONDAY
            "Tue" -> Calendar.TUESDAY
            "Wed" -> Calendar.WEDNESDAY
            "Thu" -> Calendar.THURSDAY
            "Fri" -> Calendar.FRIDAY
            "Sat" -> Calendar.SATURDAY
            "Sun" -> Calendar.SUNDAY
            else -> Calendar.MONDAY
        }

        cal.set(Calendar.DAY_OF_WEEK, dow)
        return cal.timeInMillis
    }

    private fun listenForAnnouncementsRow() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid).get()
            .addOnSuccessListener { snap ->
                val teamId = snap.getString("teamId")
                if (teamId.isNullOrBlank()) {
                    renderAnnouncementsRow(0, 0)
                    return@addOnSuccessListener
                }

                announcementsReg?.remove()
                eventsReg?.remove()

                announcementsReg = db.collection("announcements")
                    .whereEqualTo("teamId", teamId)
                    .addSnapshotListener { qs, _ ->
                        announcementsCount = qs?.size() ?: 0
                        renderAnnouncementsRow(announcementsCount, eventsCount)
                    }

                eventsReg = db.collection("teamEvents")
                    .whereEqualTo("teamId", teamId)
                    .addSnapshotListener { qs, _ ->
                        eventsCount = qs?.size() ?: 0
                        renderAnnouncementsRow(announcementsCount, eventsCount)
                    }
            }
            .addOnFailureListener {
                renderAnnouncementsRow(0, 0)
            }
    }

    private fun renderAnnouncementsRow(aCount: Int, eCount: Int) {
        val announcementsCard = HomeCard(
            title = "Announcements",
            subtitle = if (aCount == 1) "1 announcement" else "$aCount announcements"
        )
        val eventsCard = HomeCard(
            title = "Events",
            subtitle = if (eCount == 1) "1 new event" else "$eCount new events"
        )

        b.rvTasks.isNestedScrollingEnabled = false
        b.rvTasks.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        b.rvTasks.adapter = HomeCardAdapter(listOf(announcementsCard, eventsCard)) {
            // any tap goes to the list screen
            findNavController().navigate(R.id.teamUpdatesFragment)
        }
    }

    private fun mockForms(): List<HomeCard> = listOf(
        HomeCard("Absence Form", "Submit"),
        HomeCard("Injury Report", "Submit"),
        HomeCard("Wellness Check", "Submit"),
        HomeCard("Academic Check", "Bi-weekly"),
        HomeCard("My Submissions", "View status")
    )

    private fun mockAtRiskAlerts(): List<HomeCard> = listOf(
        HomeCard("Log At-Risk Alert", "Tap to submit")
    )

    private fun mockCalendar(): List<HomeCard> = listOf(
        HomeCard("Mon", "Add events"),
        HomeCard("Tue", "Add events"),
        HomeCard("Wed", "Add events"),
        HomeCard("Thu", "Add events")
    )

    private fun mockTasks(): List<HomeCard> = listOf(
        HomeCard("No tasks yet", "Tap Quick Add")
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
        announcementsReg?.remove()
        eventsReg?.remove()
        announcementsReg = null
        eventsReg = null
    }
}