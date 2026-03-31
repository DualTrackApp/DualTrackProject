package com.dualtrack.app.ui.coach



import android.os.Bundle

import android.view.LayoutInflater

import android.view.View

import android.view.ViewGroup

import android.widget.ArrayAdapter

import android.widget.EditText

import android.widget.LinearLayout

import android.widget.Spinner

import android.widget.Toast

import androidx.fragment.app.Fragment

import androidx.navigation.fragment.findNavController

import androidx.recyclerview.widget.LinearLayoutManager

import com.dualtrack.app.R

import com.dualtrack.app.databinding.FragmentCoachDashboardBinding

import com.google.android.material.dialog.MaterialAlertDialogBuilder

import com.google.firebase.Timestamp

import com.google.firebase.auth.FirebaseAuth

import com.google.firebase.firestore.FirebaseFirestore

import com.google.firebase.firestore.ListenerRegistration

import com.google.firebase.firestore.SetOptions



class CoachDashboardFragment : Fragment() {



    private var _b: FragmentCoachDashboardBinding? = null

    private val b get() = _b!!



    private lateinit var auth: FirebaseAuth

    private lateinit var db: FirebaseFirestore



    private var cachedTeamId: String? = null

    private var cachedTeamName: String? = null



    private var rosterListener: ListenerRegistration? = null



    private lateinit var athletesAdapter: CoachEmailAdapter

    private lateinit var flagsAdapter: CoachEmailAdapter

    private lateinit var wellnessAdapter: CoachEmailAdapter



    private data class RosterAthlete(

        val uid: String,

        val email: String

    )



    override fun onCreateView(

        inflater: LayoutInflater,

        container: ViewGroup?,

        savedInstanceState: Bundle?

    ): View {

        _b = FragmentCoachDashboardBinding.inflate(inflater, container, false)

        auth = FirebaseAuth.getInstance()

        db = FirebaseFirestore.getInstance()

        return b.root

    }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)



        setupLists()



        b.logo.root.setOnClickListener {

            auth.signOut()

            findNavController().navigate(R.id.action_global_loginFragment)

        }



        b.btnManageTeam.setOnClickListener { showManageTeamDialog() }



        b.btnTeamForms.setOnClickListener {

            findNavController().navigate(R.id.action_coachHome_to_coachForms)

        }



        b.btnAddAnnouncement.setOnClickListener { showCreateAnnouncementDialog() }

        b.btnAddEvent.setOnClickListener { showCreateEventDialog() }

        b.btnSendForms.setOnClickListener { showSendFormDialog() }



        b.btnReviewTeam.setOnClickListener {

            findNavController().navigate(R.id.action_coachHome_to_coachTeam)

        }



        refreshCoachTeamCache {

            startRosterListener()

        }

    }



    private fun setupLists() {

        athletesAdapter = CoachEmailAdapter()

        flagsAdapter = CoachEmailAdapter()

        wellnessAdapter = CoachEmailAdapter()



        b.rvAthletes.layoutManager = LinearLayoutManager(requireContext())

        b.rvFlags.layoutManager = LinearLayoutManager(requireContext())

        b.rvWellness.layoutManager = LinearLayoutManager(requireContext())



        b.rvAthletes.adapter = athletesAdapter

        b.rvFlags.adapter = flagsAdapter

        b.rvWellness.adapter = wellnessAdapter

    }



    private fun refreshCoachTeamCache(onDone: (() -> Unit)? = null) {

        val uid = auth.currentUser?.uid ?: run {

            onDone?.invoke()

            return

        }



        db.collection("users").document(uid)

            .get()

            .addOnSuccessListener { snap ->

                cachedTeamId = snap.getString("teamId")

                cachedTeamName = snap.getString("teamName")

                onDone?.invoke()

            }

            .addOnFailureListener {

                onDone?.invoke()

            }

    }



    private fun startRosterListener() {

        rosterListener?.remove()

        val teamId = cachedTeamId

        if (teamId.isNullOrBlank()) {

            val empty = listOf("No athletes assigned")

            athletesAdapter.submit(empty)

            flagsAdapter.submit(empty)

            wellnessAdapter.submit(empty)

            return

        }



        rosterListener = db.collection("teams").document(teamId)

            .collection("roster")

            .addSnapshotListener { snap, _ ->

                val ids = snap?.documents?.map { it.id }?.distinct().orEmpty()

                if (ids.isEmpty()) {

                    val empty = listOf("No athletes assigned")

                    athletesAdapter.submit(empty)

                    flagsAdapter.submit(empty)

                    wellnessAdapter.submit(empty)

                    return@addSnapshotListener

                }



                fetchEmailsForUids(ids) { emails ->

                    val list = if (emails.isEmpty()) listOf("No athletes assigned") else emails

                    athletesAdapter.submit(list)

                    flagsAdapter.submit(list)

                    wellnessAdapter.submit(list)

                }

            }

    }



    private fun fetchEmailsForUids(uids: List<String>, onDone: (List<String>) -> Unit) {

        val results = mutableListOf<String>()

        var remaining = uids.size



        uids.forEach { uid ->

            db.collection("users").document(uid)

                .get()

                .addOnSuccessListener { doc ->

                    val email = doc.getString("email") ?: doc.getString("userEmail")

                    if (!email.isNullOrBlank()) results.add(email)

                    remaining -= 1

                    if (remaining == 0) onDone(results.sorted())

                }

                .addOnFailureListener {

                    remaining -= 1

                    if (remaining == 0) onDone(results.sorted())

                }

        }

    }



    private fun fetchRosterAthletes(onDone: (List<RosterAthlete>) -> Unit) {

        val teamId = cachedTeamId

        if (teamId.isNullOrBlank()) {

            onDone(emptyList())

            return

        }



        db.collection("teams").document(teamId)

            .collection("roster")

            .get()

            .addOnSuccessListener { rosterSnap ->

                val ids = rosterSnap.documents.map { it.id }.distinct()

                if (ids.isEmpty()) {

                    onDone(emptyList())

                    return@addOnSuccessListener

                }



                val athletes = mutableListOf<RosterAthlete>()

                var remaining = ids.size



                ids.forEach { uid ->

                    db.collection("users").document(uid)

                        .get()

                        .addOnSuccessListener { doc ->

                            val email = doc.getString("email") ?: doc.getString("userEmail")

                            if (!email.isNullOrBlank()) {

                                athletes.add(RosterAthlete(uid, email))

                            }

                            remaining -= 1

                            if (remaining == 0) onDone(athletes.sortedBy { it.email })

                        }

                        .addOnFailureListener {

                            remaining -= 1

                            if (remaining == 0) onDone(athletes.sortedBy { it.email })

                        }

                }

            }

            .addOnFailureListener {

                onDone(emptyList())

            }

    }



    private fun showSendFormDialog() {

        refreshCoachTeamCache {

            val teamId = cachedTeamId

            if (teamId.isNullOrBlank()) {

                Toast.makeText(requireContext(), "Create a team first.", Toast.LENGTH_SHORT).show()

                return@refreshCoachTeamCache

            }



            fetchRosterAthletes { athletes ->

                if (athletes.isEmpty()) {

                    Toast.makeText(requireContext(), "No athletes assigned to send forms to.", Toast.LENGTH_SHORT).show()

                    return@fetchRosterAthletes

                }



                val formOptions = arrayOf("Academic Check", "Wellness Check", "At-Risk Alert")



                MaterialAlertDialogBuilder(requireContext())

                    .setTitle("Send Form")

                    .setItems(formOptions) { _, which ->

                        val formType = when (which) {

                            0 -> "academic"

                            1 -> "wellness"

                            else -> "atRisk"

                        }

                        showRecipientModeDialog(formType, athletes)

                    }

                    .show()

            }

        }

    }



    private fun showRecipientModeDialog(formType: String, athletes: List<RosterAthlete>) {

        val options = arrayOf("Send to all players", "Send to selected players")



        MaterialAlertDialogBuilder(requireContext())

            .setTitle("Choose Recipients")

            .setItems(options) { _, which ->

                when (which) {

                    0 -> showSendMetaDialog(formType, athletes)

                    1 -> showSpecificPlayersDialog(formType, athletes)

                }

            }

            .show()

    }



    private fun showSpecificPlayersDialog(formType: String, athletes: List<RosterAthlete>) {

        val labels = athletes.map { it.email }.toTypedArray()

        val checkedItems = BooleanArray(labels.size)



        MaterialAlertDialogBuilder(requireContext())

            .setTitle("Select Players")

            .setMultiChoiceItems(labels, checkedItems) { _, which, isChecked ->

                checkedItems[which] = isChecked

            }

            .setNegativeButton("Cancel", null)

            .setPositiveButton("Next") { _, _ ->

                val selected = athletes.filterIndexed { index, _ -> checkedItems[index] }

                if (selected.isEmpty()) {

                    Toast.makeText(requireContext(), "Select at least one player.", Toast.LENGTH_SHORT).show()

                    return@setPositiveButton

                }

                showSendMetaDialog(formType, selected)

            }

            .show()

    }



    private fun showSendMetaDialog(formType: String, recipients: List<RosterAthlete>) {

        val dueDateInput = EditText(requireContext()).apply {

            hint = "Due date (ex: Mar 21, 5:00 PM)"

        }



        val instructionsInput = EditText(requireContext()).apply {

            hint = "Instructions / notes for athlete"

            minLines = 3

        }



        val container = LinearLayout(requireContext()).apply {

            orientation = LinearLayout.VERTICAL

            val pad = (16 * resources.displayMetrics.density).toInt()

            setPadding(pad, pad, pad, pad)

            addView(dueDateInput)

            addView(instructionsInput)

        }



        MaterialAlertDialogBuilder(requireContext())

            .setTitle("Form Request Details")

            .setView(container)

            .setNegativeButton("Cancel", null)

            .setPositiveButton("Send") { _, _ ->

                val dueDate = dueDateInput.text.toString().trim()

                val instructions = instructionsInput.text.toString().trim()



                if (dueDate.isBlank()) {

                    Toast.makeText(requireContext(), "Please enter a due date.", Toast.LENGTH_SHORT).show()

                    return@setPositiveButton

                }



                createFormRequests(formType, recipients, dueDate, instructions)

            }

            .show()

    }



    private fun createFormRequests(

        formType: String,

        recipients: List<RosterAthlete>,

        dueDate: String,

        instructions: String

    ) {

        val teamId = cachedTeamId

        val teamName = cachedTeamName.orEmpty()

        val coachId = auth.currentUser?.uid

        val coachEmail = auth.currentUser?.email.orEmpty()



        if (teamId.isNullOrBlank() || coachId.isNullOrBlank()) {

            Toast.makeText(requireContext(), "Missing coach/team info.", Toast.LENGTH_SHORT).show()

            return

        }



        val batch = db.batch()



        recipients.forEach { athlete ->

            val docRef = db.collection("forms").document()

            val doc = hashMapOf<String, Any>(

                "formType" to formType,

                "userId" to athlete.uid,

                "userEmail" to athlete.email,

                "teamId" to teamId,

                "teamName" to teamName,

                "createdAt" to Timestamp.now(),

                "requestedAt" to Timestamp.now(),

                "requestedByCoachId" to coachId,

                "requestedByCoachEmail" to coachEmail,

                "dueDate" to dueDate,

                "requestInstructions" to instructions,

                "status" to "requested",

                "coachNote" to "",

                "isCoachRequested" to true,

                "data" to emptyMap<String, Any>()

            )

            batch.set(docRef, doc)

        }



        batch.commit()

            .addOnSuccessListener {

                Toast.makeText(requireContext(), "Form request sent ✓", Toast.LENGTH_SHORT).show()

            }

            .addOnFailureListener { e ->

                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()

            }

    }



    private fun showManageTeamDialog() {

        refreshCoachTeamCache {

            val hasTeam = !cachedTeamId.isNullOrBlank()



            val options = if (hasTeam) {

                arrayOf("Add athlete to team", "Create a new team")

            } else {

                arrayOf("Create a team")

            }



            MaterialAlertDialogBuilder(requireContext())

                .setTitle(if (hasTeam) "Manage Team" else "Team Setup")

                .setItems(options) { _, which ->

                    if (!hasTeam) {

                        showCreateTeamDialog()

                        return@setItems

                    }

                    when (which) {

                        0 -> showAddAthleteDialog()

                        1 -> showCreateTeamDialog()

                    }

                }

                .show()

        }

    }



    private fun showCreateTeamDialog() {

        val input = EditText(requireContext()).apply {

            hint = "Enter team name (ex: Baseball)"

        }



        MaterialAlertDialogBuilder(requireContext())

            .setTitle("Create Team")

            .setView(input)

            .setNegativeButton("Cancel", null)

            .setPositiveButton("Create") { _, _ ->

                val teamName = input.text.toString().trim()

                if (teamName.isBlank()) {

                    Toast.makeText(requireContext(), "Team name is required", Toast.LENGTH_SHORT).show()

                    return@setPositiveButton

                }

                createTeam(teamName)

            }

            .show()

    }



    private fun createTeam(teamName: String) {

        val coachId = auth.currentUser?.uid

        if (coachId.isNullOrBlank()) {

            Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show()

            return

        }



        val teamRef = db.collection("teams").document()



        val teamData = hashMapOf(

            "teamId" to teamRef.id,

            "teamName" to teamName,

            "coachId" to coachId,

            "createdAt" to Timestamp.now()

        )



        teamRef.set(teamData)

            .addOnSuccessListener {

                val coachUpdate = hashMapOf(

                    "teamId" to teamRef.id,

                    "teamName" to teamName,

                    "updatedAt" to Timestamp.now()

                )



                db.collection("users").document(coachId)

                    .set(coachUpdate, SetOptions.merge())

                    .addOnSuccessListener {

                        cachedTeamId = teamRef.id

                        cachedTeamName = teamName

                        startRosterListener()

                        Toast.makeText(requireContext(), "Team created: $teamName", Toast.LENGTH_SHORT).show()

                    }

            }

            .addOnFailureListener { e ->

                Toast.makeText(requireContext(), "Failed to create team: ${e.message}", Toast.LENGTH_LONG).show()

            }

    }



    private fun showAddAthleteDialog() {

        val teamId = cachedTeamId

        val teamName = cachedTeamName

        val coachId = auth.currentUser?.uid



        if (coachId.isNullOrBlank() || teamId.isNullOrBlank()) {

            Toast.makeText(requireContext(), "Create a team first.", Toast.LENGTH_SHORT).show()

            return

        }



        val input = EditText(requireContext()).apply { hint = "Athlete UID" }



        MaterialAlertDialogBuilder(requireContext())

            .setTitle("Add Athlete")

            .setMessage("Assign an athlete to ${teamName ?: "your team"}")

            .setView(input)

            .setNegativeButton("Cancel", null)

            .setPositiveButton("Assign") { _, _ ->

                val athleteUid = input.text.toString().trim()

                if (athleteUid.isBlank()) return@setPositiveButton

                assignAthleteByUid(athleteUid, teamId, teamName, coachId)

            }

            .show()

    }



    private fun showCreateAnnouncementDialog() {

        refreshCoachTeamCache {

            val teamId = cachedTeamId

            val coachId = auth.currentUser?.uid

            if (coachId.isNullOrBlank() || teamId.isNullOrBlank()) {

                Toast.makeText(requireContext(), "Create a team first.", Toast.LENGTH_SHORT).show()

                return@refreshCoachTeamCache

            }



            val titleInput = EditText(requireContext()).apply {

                hint = "Announcement title"

            }



            val msgInput = EditText(requireContext()).apply {

                hint = "Team message"

                minLines = 4

            }



            val container = LinearLayout(requireContext()).apply {

                orientation = LinearLayout.VERTICAL

                val pad = (16 * resources.displayMetrics.density).toInt()

                setPadding(pad, pad, pad, pad)

                addView(titleInput)

                addView(msgInput)

            }



            MaterialAlertDialogBuilder(requireContext())

                .setTitle("Create Announcement")

                .setView(container)

                .setNegativeButton("Cancel", null)

                .setPositiveButton("Post") { _, _ ->

                    val title = titleInput.text.toString().trim()

                    val message = msgInput.text.toString().trim()



                    if (title.isBlank() || message.isBlank()) {

                        Toast.makeText(requireContext(), "Title and message are required.", Toast.LENGTH_SHORT).show()

                        return@setPositiveButton

                    }



                    val doc = hashMapOf(

                        "teamId" to teamId,

                        "coachId" to coachId,

                        "type" to "announcement",

                        "title" to title,

                        "message" to message,

                        "createdAt" to Timestamp.now(),

                        "seenBy" to emptyList<String>()

                    )



                    db.collection("announcements")

                        .add(doc)

                        .addOnSuccessListener {

                            Toast.makeText(requireContext(), "Announcement posted ✓", Toast.LENGTH_SHORT).show()

                        }

                        .addOnFailureListener { e ->

                            Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()

                        }

                }

                .show()

        }

    }



    private fun showCreateEventDialog() {

        refreshCoachTeamCache {

            val teamId = cachedTeamId

            val coachId = auth.currentUser?.uid

            if (coachId.isNullOrBlank() || teamId.isNullOrBlank()) {

                Toast.makeText(requireContext(), "Create a team first.", Toast.LENGTH_SHORT).show()

                return@refreshCoachTeamCache

            }



            val titleInput = EditText(requireContext()).apply {

                hint = "Event title"

            }



            val eventTypeSpinner = Spinner(requireContext())

            val eventTypes = listOf("Game", "Practice", "Team Dinner", "Meeting", "Other")

            eventTypeSpinner.adapter = ArrayAdapter(

                requireContext(),

                android.R.layout.simple_spinner_item,

                eventTypes

            ).also {

                it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            }



            val dateInput = EditText(requireContext()).apply {

                hint = "Date (ex: Apr 14)"

            }



            val timeInput = EditText(requireContext()).apply {

                hint = "Time (ex: 6:00 PM)"

            }



            val locationInput = EditText(requireContext()).apply {

                hint = "Location"

            }



            val attireInput = EditText(requireContext()).apply {

                hint = "Attire / jersey / what to wear"

            }



            val detailsInput = EditText(requireContext()).apply {

                hint = "Event details"

                minLines = 3

            }



            val container = LinearLayout(requireContext()).apply {

                orientation = LinearLayout.VERTICAL

                val pad = (16 * resources.displayMetrics.density).toInt()

                setPadding(pad, pad, pad, pad)

                addView(titleInput)

                addView(eventTypeSpinner)

                addView(dateInput)

                addView(timeInput)

                addView(locationInput)

                addView(attireInput)

                addView(detailsInput)

            }



            MaterialAlertDialogBuilder(requireContext())

                .setTitle("Add Team Event")

                .setView(container)

                .setNegativeButton("Cancel", null)

                .setPositiveButton("Create") { _, _ ->

                    val title = titleInput.text.toString().trim()

                    val eventType = eventTypeSpinner.selectedItem?.toString().orEmpty()

                    val eventDate = dateInput.text.toString().trim()

                    val eventTime = timeInput.text.toString().trim()

                    val location = locationInput.text.toString().trim()

                    val attire = attireInput.text.toString().trim()

                    val details = detailsInput.text.toString().trim()



                    if (title.isBlank() || eventDate.isBlank() || eventTime.isBlank()) {

                        Toast.makeText(

                            requireContext(),

                            "Title, date, and time are required.",

                            Toast.LENGTH_SHORT

                        ).show()

                        return@setPositiveButton

                    }



                    val doc = hashMapOf(

                        "teamId" to teamId,

                        "coachId" to coachId,

                        "type" to "event",

                        "title" to title,

                        "eventType" to eventType,

                        "eventDate" to eventDate,

                        "eventTime" to eventTime,

                        "location" to location,

                        "attire" to attire,

                        "details" to details,

                        "createdAt" to Timestamp.now(),

                        "seenBy" to emptyList<String>()

                    )



                    db.collection("teamEvents")

                        .add(doc)

                        .addOnSuccessListener {

                            Toast.makeText(requireContext(), "Event created ✓", Toast.LENGTH_SHORT).show()

                        }

                        .addOnFailureListener { e ->

                            Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()

                        }

                }

                .show()

        }

    }



    private fun assignAthleteByUid(

        athleteUid: String,

        teamId: String,

        teamName: String?,

        coachId: String

    ) {

        val userRef = db.collection("users").document(athleteUid)



        userRef.get()

            .addOnSuccessListener { snap ->

                if (!snap.exists()) {

                    Toast.makeText(requireContext(), "No user doc for UID: $athleteUid", Toast.LENGTH_LONG).show()

                    return@addOnSuccessListener

                }



                val update = hashMapOf<String, Any>(

                    "teamId" to teamId,

                    "assignedAt" to Timestamp.now(),

                    "assignedBy" to coachId,

                    "updatedAt" to Timestamp.now()

                )

                if (!teamName.isNullOrBlank()) update["teamName"] = teamName



                userRef.set(update, SetOptions.merge())

                    .addOnSuccessListener {

                        val rosterData = hashMapOf(

                            "teamId" to teamId,

                            "userId" to athleteUid,

                            "assignedAt" to Timestamp.now(),

                            "assignedBy" to coachId

                        )



                        db.collection("teams").document(teamId)

                            .collection("roster").document(athleteUid)

                            .set(rosterData, SetOptions.merge())

                            .addOnSuccessListener {

                                Toast.makeText(requireContext(), "Athlete assigned!", Toast.LENGTH_SHORT).show()

                            }

                    }

            }

    }



    override fun onDestroyView() {

        rosterListener?.remove()

        rosterListener = null

        super.onDestroyView()

        _b = null

    }

}


