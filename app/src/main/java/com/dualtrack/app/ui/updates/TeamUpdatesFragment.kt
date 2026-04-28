package com.dualtrack.app.ui.updates

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.dualtrack.app.databinding.FragmentTeamUpdatesBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TeamUpdatesFragment : Fragment() {

    private var _b: FragmentTeamUpdatesBinding? = null
    private val b get() = _b!!

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private var annReg: ListenerRegistration? = null
    private var evReg: ListenerRegistration? = null

    private val adapter = TeamUpdatesAdapter()

    private var annList: List<TeamUpdate> = emptyList()
    private var evList: List<TeamUpdate> = emptyList()

    private var mode: String = "all"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _b = FragmentTeamUpdatesBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mode = arguments?.getString("mode") ?: "all"

        b.btnBack.setOnClickListener { findNavController().navigateUp() }

        b.rvUpdates.layoutManager = LinearLayoutManager(requireContext())
        b.rvUpdates.adapter = adapter

        setupHeader()
        listen()
    }

    private fun setupHeader() {
        when (mode) {
            "announcements" -> {
                b.tvTitle.text = "Announcements"
                b.tvSubtitle.text = "Latest team messages and updates"
            }
            "events" -> {
                b.tvTitle.text = "Events"
                b.tvSubtitle.text = "Upcoming and past team events"
            }
            else -> {
                b.tvTitle.text = "Team Updates"
                b.tvSubtitle.text = "Latest announcements and events"
            }
        }
    }

    private fun listen() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid).get()
            .addOnSuccessListener { snap ->
                val teamId = snap.getString("teamId") ?: return@addOnSuccessListener

                annReg?.remove()
                evReg?.remove()

                annReg = db.collection("announcements")
                    .whereEqualTo("teamId", teamId)
                    .addSnapshotListener { qs, _ ->
                        val docs = qs?.documents.orEmpty()

                        markDocsSeen("announcements", docs.map { it.id }, uid)

                        annList = docs
                            .sortedByDescending { it.getTimestamp("createdAt")?.toDate()?.time ?: 0L }
                            .mapNotNull { d ->
                                val title = d.getString("title") ?: return@mapNotNull null
                                val message = d.getString("message").orEmpty()

                                TeamUpdate(
                                    id = d.id,
                                    type = "announcement",
                                    title = title,
                                    subtitle = buildAnnouncementStatusSubtitle(
                                        createdAtMillis = d.getTimestamp("createdAt")?.toDate()?.time,
                                        body = message
                                    ),
                                    createdAt = d.getTimestamp("createdAt")
                                )
                            }

                        render()
                    }

                evReg = db.collection("teamEvents")
                    .whereEqualTo("teamId", teamId)
                    .addSnapshotListener { qs, _ ->
                        val docs = qs?.documents.orEmpty()

                        markDocsSeen("teamEvents", docs.map { it.id }, uid)

                        evList = docs.mapNotNull { d ->
                            val title = d.getString("title") ?: return@mapNotNull null
                            val eventType = d.getString("eventType").orEmpty()
                            val eventDate = d.getString("eventDate").orEmpty()
                            val eventTime = d.getString("eventTime").orEmpty()
                            val location = d.getString("location").orEmpty()
                            val attire = d.getString("attire").orEmpty()
                            val details = d.getString("details").orEmpty()

                            val subtitleParts = mutableListOf<String>()
                            if (eventType.isNotBlank()) subtitleParts.add(eventType)
                            if (eventDate.isNotBlank()) subtitleParts.add(eventDate)
                            if (eventTime.isNotBlank()) subtitleParts.add(eventTime)
                            if (location.isNotBlank()) subtitleParts.add(location)
                            if (attire.isNotBlank()) subtitleParts.add("Attire: $attire")
                            if (details.isNotBlank()) subtitleParts.add(details)

                            TeamUpdate(
                                id = d.id,
                                type = "event",
                                title = title,
                                subtitle = subtitleParts.joinToString(" • "),
                                createdAt = d.getTimestamp("createdAt")
                            )
                        }

                        render()
                    }
            }
    }

    private fun markDocsSeen(collection: String, ids: List<String>, uid: String) {
        ids.forEach { id ->
            db.collection(collection)
                .document(id)
                .update("seenBy", FieldValue.arrayUnion(uid))
        }
    }

    private fun render() {
        val items = when (mode) {
            "announcements" -> annList
            "events" -> buildEventDisplayList(evList)
            else -> (annList + buildEventDisplayList(evList))
                .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
        }

        adapter.submit(items)

        val empty = items.isEmpty()
        b.tvEmpty.visibility = if (empty) View.VISIBLE else View.GONE
        b.rvUpdates.visibility = if (empty) View.GONE else View.VISIBLE
    }

    private fun buildAnnouncementStatusSubtitle(
        createdAtMillis: Long?,
        body: String
    ): String {
        if (createdAtMillis == null) return body

        val calendar = Calendar.getInstance()
        val nowMillis = calendar.timeInMillis

        calendar.timeInMillis = createdAtMillis
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)

        val status = if (nowMillis <= calendar.timeInMillis) {
            "ANNOUNCEMENT IN PROGRESS"
        } else {
            "PAST ANNOUNCEMENT"
        }

        return if (body.isBlank()) status else "$status • $body"
    }

    private fun buildEventDisplayList(events: List<TeamUpdate>): List<TeamUpdate> {
        val now = System.currentTimeMillis()

        val upcoming = mutableListOf<Pair<TeamUpdate, Long?>>()
        val past = mutableListOf<Pair<TeamUpdate, Long?>>()

        events.forEach { event ->
            val eventMillis = parseEventMillis(event.subtitle.orEmpty())

            val status = if (eventMillis != null && eventMillis >= now) {
                "UPCOMING EVENT"
            } else {
                "PAST EVENT"
            }

            val updated = event.copy(
                subtitle = if (event.subtitle.isNullOrBlank()) {
                    status
                } else {
                    "$status • ${event.subtitle}"
                }
            )

            if (eventMillis != null && eventMillis >= now) {
                upcoming += updated to eventMillis
            } else {
                past += updated to eventMillis
            }
        }

        val result = mutableListOf<TeamUpdate>()

        if (upcoming.isNotEmpty()) {
            result += TeamUpdate(
                id = "header_upcoming",
                type = "header",
                title = "Upcoming Events",
                subtitle = "",
                createdAt = null
            )
            result += upcoming.sortedBy { it.second ?: Long.MAX_VALUE }.map { it.first }
        }

        if (past.isNotEmpty()) {
            result += TeamUpdate(
                id = "header_past",
                type = "header",
                title = "Past Events",
                subtitle = "",
                createdAt = null
            )
            result += past.sortedByDescending { it.second ?: Long.MIN_VALUE }.map { it.first }
        }

        return result
    }

    private fun parseEventMillis(subtitle: String): Long? {
        val patterns = listOf(
            Regex("""([A-Za-z]{3,9}\s+\d{1,2},\s+\d{4})\s+•\s+([0-9]{1,2}:[0-9]{2}\s*[AaPp][.]?[Mm][.]?)"""),
            Regex("""([A-Za-z]{3,9}\s+\d{1,2},\s+\d{4})"""),
            Regex("""([A-Za-z]{3,9}\s+\d{1,2})\s+•\s+([0-9]{1,2}:[0-9]{2}\s*[AaPp][.]?[Mm][.]?)"""),
            Regex("""([A-Za-z]{3,9}\s+\d{1,2})""")
        )

        for (pattern in patterns) {
            val match = pattern.find(subtitle) ?: continue

            val rawDate = match.groupValues[1]
            val rawTime = match.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() }

            val year = Calendar.getInstance().get(Calendar.YEAR)
            val normalizedDate = if (rawDate.contains(",")) rawDate else "$rawDate, $year"

            val normalizedTime = rawTime
                ?.replace(".", "")
                ?.replace(Regex("""\s+"""), "")
                ?.replace(Regex("""([0-9])([AaPp][Mm])$"""), "$1 $2")
                ?.uppercase(Locale.US)

            val valueToParse = if (normalizedTime != null) {
                "$normalizedDate $normalizedTime"
            } else {
                normalizedDate
            }

            val formats = if (normalizedTime != null) {
                listOf(
                    SimpleDateFormat("MMMM d, yyyy h:mm a", Locale.US),
                    SimpleDateFormat("MMM d, yyyy h:mm a", Locale.US)
                )
            } else {
                listOf(
                    SimpleDateFormat("MMMM d, yyyy", Locale.US),
                    SimpleDateFormat("MMM d, yyyy", Locale.US)
                )
            }

            formats.forEach { format ->
                format.isLenient = true
                runCatching { format.parse(valueToParse)?.time }.getOrNull()?.let { return it }
            }
        }

        return null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        annReg?.remove()
        evReg?.remove()
        annReg = null
        evReg = null
        _b = null
    }
}

