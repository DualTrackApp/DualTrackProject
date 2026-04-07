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
import com.google.firebase.firestore.ListenerRegistration
import java.util.Calendar

class AthleteHomeFragment : Fragment() {

    private var _b: FragmentHomeAthleteBinding? = null
    private val b get() = _b!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var announcementsReg: ListenerRegistration? = null
    private var eventsReg: ListenerRegistration? = null
    private var requestedFormsReg: ListenerRegistration? = null
    private var submissionsReg: ListenerRegistration? = null
    private var calendarReg: ListenerRegistration? = null

    private var reviewedCount: Int = 0
    private var needsAttentionCount: Int = 0

    private var newAnnouncementCount: Int = 0
    private var newEventCount: Int = 0

    private var announcementCards: List<HomeCard> = emptyList()
    private var eventCards: List<HomeCard> = emptyList()

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
        setupRecyclerViews()
        listenForRequestedFormsRow()
        listenForSubmissionStatuses()
        listenForTeamContent()
        listenForWeeklyCalendar()

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
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            b.tvWelcomeTitle.text = "Welcome, Athlete!"
            return
        }

        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { snap ->
                val firstName = snap.getString("firstName").orEmpty()
                val lastName = snap.getString("lastName").orEmpty()
                val fullName = "$firstName $lastName".trim()

                val fallback = auth.currentUser?.email?.substringBefore("@").orEmpty().ifBlank { "Athlete" }
                val displayName = if (fullName.isNotBlank()) fullName else fallback

                b.tvWelcomeTitle.text = "Welcome, $displayName!"
            }
            .addOnFailureListener {
                val fallback = auth.currentUser?.email?.substringBefore("@").orEmpty().ifBlank { "Athlete" }
                b.tvWelcomeTitle.text = "Welcome, $fallback!"
            }
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

    private fun setupRecyclerViews() {
        renderAnnouncementsRow()
        renderFormsRow()

        b.rvRequestedForms.isNestedScrollingEnabled = false
        b.rvRequestedForms.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        b.rvRequestedForms.adapter = HomeCardAdapter(
            listOf(HomeCard("No requested forms", "Coach-assigned forms will appear here"))
        ) { card ->
            handleCardClick(card)
        }

        b.rvTasks.isNestedScrollingEnabled = false
        b.rvTasks.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        b.rvTasks.adapter = HomeCardAdapter(
            listOf(HomeCard("No events yet", "Upcoming team events will appear here"))
        ) { card ->
            handleCardClick(card)
        }

        b.rvCalendar.isNestedScrollingEnabled = false
        b.rvCalendar.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        b.rvCalendar.adapter = HomeCardAdapter(buildWeeklyCalendarCards(emptyList())) { card ->
            val dayMillis = dayMillisForLabel(card.title)
            val args = Bundle().apply { putLong("dayMillis", dayMillis) }
            findNavController().navigate(R.id.action_athleteHome_to_dayEvents, args)
        }

        setupHorizontalList(b.rvWellness, mockWellness())
    }

    private fun renderAnnouncementsRow() {
        val items = if (announcementCards.isEmpty()) {
            listOf(HomeCard("Announcements", "No announcements yet"))
        } else {
            announcementCards
        }

        b.rvAnnouncements.isNestedScrollingEnabled = false
        b.rvAnnouncements.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        b.rvAnnouncements.adapter = HomeCardAdapter(items) { card ->
            handleCardClick(card)
        }
    }

    private fun renderFormsRow() {
        setupHorizontalList(b.rvForms, buildFormsCards())
    }

    private fun renderEventsRow() {
        val items = if (eventCards.isEmpty()) {
            listOf(HomeCard("No events yet", "Upcoming team events will appear here"))
        } else {
            eventCards
        }

        b.rvTasks.adapter = HomeCardAdapter(items) { card ->
            handleCardClick(card)
        }
    }

    private fun buildFormsCards(): List<HomeCard> {
        val mySubmissionsSubtitle = when {
            needsAttentionCount > 0 -> {
                if (needsAttentionCount == 1) "1 needs attention" else "$needsAttentionCount need attention"
            }
            reviewedCount > 0 -> {
                if (reviewedCount == 1) "1 reviewed" else "$reviewedCount reviewed"
            }
            else -> "View status"
        }

        return listOf(
            HomeCard("Absence Form", "Submit"),
            HomeCard("Injury Report", "Submit"),
            HomeCard("My Submissions", mySubmissionsSubtitle)
        )
    }

    private fun listenForSubmissionStatuses() {
        val uid = auth.currentUser?.uid ?: return

        submissionsReg?.remove()
        submissionsReg = db.collection("forms")
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, e ->
                if (_b == null) return@addSnapshotListener

                if (e != null) {
                    Toast.makeText(
                        requireContext(),
                        "Submission status error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addSnapshotListener
                }

                var reviewed = 0
                var needsAttention = 0

                snapshot?.documents?.forEach { doc ->
                    val status = doc.getString("status").orEmpty()
                    val seen = doc.getBoolean("athleteStatusSeen") == true

                    if (!seen) {
                        when (status) {
                            "approved" -> reviewed += 1
                            "needs_attention" -> {
                                reviewed += 1
                                needsAttention += 1
                            }
                        }
                    }
                }

                reviewedCount = reviewed
                needsAttentionCount = needsAttention
                renderFormsRow()
            }
    }

    private fun setupHorizontalList(
        recyclerView: RecyclerView,
        items: List<HomeCard>
    ) {
        recyclerView.isNestedScrollingEnabled = false
        recyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        recyclerView.adapter = HomeCardAdapter(items) { card ->
            handleCardClick(card)
        }
    }

    private fun handleCardClick(card: HomeCard) {
        when {
            card.docId != null && card.formType != null -> {
                val args = Bundle().apply {
                    putString("requestId", card.docId)
                }

                when (card.formType) {
                    "academic" -> findNavController().navigate(R.id.academicFormFragment, args)
                    "wellness" -> findNavController().navigate(R.id.wellnessFormFragment, args)
                    "atRisk" -> findNavController().navigate(R.id.atRiskFormFragment, args)
                }
            }

            card.title == "Absence Form" -> findNavController().navigate(R.id.absenceFormFragment)
            card.title == "Injury Report" -> findNavController().navigate(R.id.injuryFormFragment)
            card.title == "My Submissions" -> findNavController().navigate(R.id.athleteSubmissionsFragment)

            card.title == "Completion Chart" || card.title == "Wellness Diaries" || card.title == "Eligibility Flags" -> {
                val bundle = Bundle().apply { putString("cardTitle", card.title) }
                findNavController().navigate(R.id.progressWellnessFormFragment, bundle)
            }

            card.title == "Injury Overview" -> findNavController().navigate(R.id.injuryFormFragment)

            card.title == "Announcements" || card.title == "Events" || card.title == "No events yet" -> {
                findNavController().navigate(R.id.teamUpdatesFragment)
            }

            card.title == "No requested forms" -> {
                Toast.makeText(requireContext(), "No requested forms right now.", Toast.LENGTH_SHORT).show()
            }

            else -> {
                findNavController().navigate(R.id.teamUpdatesFragment)
            }
        }
    }

    private fun listenForRequestedFormsRow() {
        val uid = auth.currentUser?.uid ?: return

        requestedFormsReg?.remove()
        requestedFormsReg = db.collection("forms")
            .whereEqualTo("userId", uid)
            .whereEqualTo("status", "requested")
            .addSnapshotListener { snapshot, e ->
                if (_b == null) return@addSnapshotListener

                if (e != null) {
                    Toast.makeText(
                        requireContext(),
                        "Requested forms error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addSnapshotListener
                }

                val items = snapshot?.documents?.mapNotNull { doc ->
                    val formType = doc.getString("formType") ?: return@mapNotNull null
                    val title = when (formType) {
                        "academic" -> "Academic Check"
                        "wellness" -> "Wellness Check"
                        "atRisk" -> "At-Risk Alert"
                        else -> return@mapNotNull null
                    }

                    val dueDate = doc.getString("dueDate").orEmpty()
                    val instructions = doc.getString("requestInstructions").orEmpty()

                    val subtitle = when {
                        dueDate.isNotBlank() -> "Due: $dueDate"
                        instructions.isNotBlank() -> instructions
                        else -> "Requested by coach"
                    }

                    HomeCard(
                        title = title,
                        subtitle = subtitle,
                        docId = doc.id,
                        formType = formType
                    )
                }.orEmpty()

                renderRequestedFormsRow(items)
            }
    }

    private fun renderRequestedFormsRow(items: List<HomeCard>) {
        val finalItems = if (items.isEmpty()) {
            listOf(HomeCard("No requested forms", "Coach-assigned forms will appear here"))
        } else {
            items
        }

        b.rvRequestedForms.adapter = HomeCardAdapter(finalItems) { card ->
            handleCardClick(card)
        }
    }

    private fun listenForTeamContent() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid).get()
            .addOnSuccessListener { snap ->
                val teamId = snap.getString("teamId")
                if (teamId.isNullOrBlank()) {
                    announcementCards = emptyList()
                    eventCards = emptyList()
                    renderAnnouncementsRow()
                    renderEventsRow()
                    return@addOnSuccessListener
                }

                announcementsReg?.remove()
                eventsReg?.remove()

                announcementsReg = db.collection("announcements")
                    .whereEqualTo("teamId", teamId)
                    .addSnapshotListener { qs, e ->
                        if (_b == null) return@addSnapshotListener

                        if (e != null) {
                            Toast.makeText(
                                requireContext(),
                                "Announcements error: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                            return@addSnapshotListener
                        }

                        val docs = qs?.documents.orEmpty()

                        newAnnouncementCount = docs.count { d ->
                            val seenBy = d.get("seenBy") as? List<*>
                            seenBy?.contains(uid) != true
                        }

                        val newestAnnouncement = docs
                            .sortedByDescending { it.getTimestamp("createdAt")?.toDate()?.time ?: 0L }
                            .firstOrNull()

                        val newestTitle = newestAnnouncement?.getString("title").orEmpty()

                        val announcementSubtitle = when {
                            newAnnouncementCount > 0 -> {
                                if (newAnnouncementCount == 1) "1 new announcement"
                                else "$newAnnouncementCount new announcements"
                            }
                            newestTitle.isNotBlank() -> newestTitle
                            else -> "View announcements"
                        }

                        announcementCards = listOf(
                            HomeCard(
                                title = "Announcements",
                                subtitle = announcementSubtitle
                            )
                        )

                        b.tvAnnouncementsHeader.text = "Announcements"
                        renderAnnouncementsRow()
                    }

                eventsReg = db.collection("teamEvents")
                    .whereEqualTo("teamId", teamId)
                    .addSnapshotListener { qs, e ->
                        if (_b == null) return@addSnapshotListener

                        if (e != null) {
                            Toast.makeText(
                                requireContext(),
                                "Events error: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                            return@addSnapshotListener
                        }

                        val docs = qs?.documents.orEmpty()
                            .sortedByDescending { it.getTimestamp("createdAt")?.toDate()?.time ?: 0L }

                        newEventCount = docs.count { d ->
                            val seenBy = d.get("seenBy") as? List<*>
                            seenBy?.contains(uid) != true
                        }

                        eventCards = docs.mapNotNull { d ->
                            val title = d.getString("title") ?: return@mapNotNull null
                            val eventDate = d.getString("eventDate").orEmpty()
                            val eventTime = d.getString("eventTime").orEmpty()
                            val location = d.getString("location").orEmpty()

                            val subtitle = listOf(eventDate, eventTime, location)
                                .filter { it.isNotBlank() }
                                .joinToString(" • ")
                                .ifBlank { "Tap to view event" }

                            HomeCard(
                                title = title,
                                subtitle = subtitle
                            )
                        }

                        b.tvEventsHeader.text =
                            if (newEventCount > 0) {
                                if (newEventCount == 1) "Events • 1 new event"
                                else "Events • $newEventCount new events"
                            } else {
                                "Events"
                            }

                        renderEventsRow()
                    }
            }
            .addOnFailureListener {
                announcementCards = emptyList()
                eventCards = emptyList()
                renderAnnouncementsRow()
                renderEventsRow()
            }
    }

    private fun listenForWeeklyCalendar() {
        val uid = auth.currentUser?.uid ?: return

        calendarReg?.remove()
        calendarReg = db.collection("users")
            .document(uid)
            .collection("events")
            .addSnapshotListener { snapshot, e ->
                if (_b == null) return@addSnapshotListener

                if (e != null) {
                    b.rvCalendar.adapter = HomeCardAdapter(buildWeeklyCalendarCards(emptyList())) { card ->
                        val dayMillis = dayMillisForLabel(card.title)
                        val args = Bundle().apply { putLong("dayMillis", dayMillis) }
                        findNavController().navigate(R.id.action_athleteHome_to_dayEvents, args)
                    }
                    return@addSnapshotListener
                }

                val docs = snapshot?.documents.orEmpty()

                b.rvCalendar.adapter = HomeCardAdapter(buildWeeklyCalendarCards(docs)) { card ->
                    val dayMillis = dayMillisForLabel(card.title)
                    val args = Bundle().apply { putLong("dayMillis", dayMillis) }
                    findNavController().navigate(R.id.action_athleteHome_to_dayEvents, args)
                }
            }
    }

    private fun buildWeeklyCalendarCards(
        docs: List<com.google.firebase.firestore.DocumentSnapshot>
    ): List<HomeCard> {
        val startCal = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }

        val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

        return labels.mapIndexed { index, label ->
            val dayCal = startCal.clone() as Calendar
            dayCal.add(Calendar.DAY_OF_MONTH, index)
            val currentDayMillis = dayCal.timeInMillis

            val matching = docs.filter { doc ->
                val storedDayMillis = doc.getLong("dayMillis")
                storedDayMillis == currentDayMillis
            }.sortedBy { it.getString("time").orEmpty() }

            val subtitle = when {
                matching.isEmpty() -> "Add events"
                matching.size == 1 -> {
                    val first = matching.first()
                    val time = first.getString("time").orEmpty()
                    val title = first.getString("title").orEmpty()
                    buildString {
                        if (time.isNotBlank()) append("$time - ")
                        append(title)
                    }
                }
                else -> {
                    val first = matching.first()
                    val time = first.getString("time").orEmpty()
                    val title = first.getString("title").orEmpty()
                    buildString {
                        append("${matching.size} events")
                        if (title.isNotBlank()) {
                            append(" • ")
                            if (time.isNotBlank()) append("$time - ")
                            append(title)
                        }
                    }
                }
            }

            HomeCard(
                title = label,
                subtitle = subtitle
            )
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

    private fun mockWellness(): List<HomeCard> = listOf(
        HomeCard("Completion Chart", "This week"),
        HomeCard("Wellness Diaries", "Log today"),
        HomeCard("Eligibility Flags", "All clear"),
        HomeCard("Injury Overview", "No active injuries")
    )

    override fun onDestroyView() {
        announcementsReg?.remove()
        eventsReg?.remove()
        requestedFormsReg?.remove()
        submissionsReg?.remove()
        calendarReg?.remove()
        announcementsReg = null
        eventsReg = null
        requestedFormsReg = null
        submissionsReg = null
        calendarReg = null
        _b = null
        super.onDestroyView()
    }
}



