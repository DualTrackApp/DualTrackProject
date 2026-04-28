package com.dualtrack.app.ui.coach

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.dualtrack.app.R
import com.dualtrack.app.databinding.FragmentCoachPlayerCalendarBinding
import com.dualtrack.app.ui.home.HomeCard
import com.dualtrack.app.ui.home.HomeCardAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CoachPlayerCalendarFragment : Fragment() {

    private var _b: FragmentCoachPlayerCalendarBinding? = null
    private val b get() = _b!!

    private val db by lazy { FirebaseFirestore.getInstance() }

    private var playerUid: String = ""
    private var playerEmail: String = ""
    private var playerName: String = ""
    private var selectedFocus: String = "overview"

    private var showingWellnessHistory = false
    private var showingRiskHistory = false
    private var showingAllForms = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playerUid = arguments?.getString("playerUid").orEmpty()
        playerEmail = arguments?.getString("playerEmail").orEmpty()
        playerName = arguments?.getString("playerName").orEmpty()
        selectedFocus = arguments?.getString("selectedFocus").orEmpty().ifBlank { "overview" }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _b = FragmentCoachPlayerCalendarBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        b.btnBack.setOnClickListener {
            findNavController().navigate(R.id.action_global_coachHome)
        }

        b.rvWeek.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        b.tvStatusLabel.text = "Status (tap to update)"

        b.layoutStatusCard.setOnClickListener {
            if (playerUid.isBlank()) return@setOnClickListener
            showStatusOptionsDialog()
        }

        b.tvToggleWellnessHistory.isClickable = true
        b.tvToggleWellnessHistory.isFocusable = true
        b.tvToggleRiskHistory.isClickable = true
        b.tvToggleRiskHistory.isFocusable = true
        b.tvToggleAllForms.isClickable = true
        b.tvToggleAllForms.isFocusable = true

        b.tvToggleWellnessHistory.setOnClickListener {
            showingWellnessHistory = !showingWellnessHistory
            syncToggleSections()
        }

        b.tvToggleRiskHistory.setOnClickListener {
            showingRiskHistory = !showingRiskHistory
            syncToggleSections()
        }

        b.tvToggleAllForms.setOnClickListener {
            showingAllForms = !showingAllForms
            syncToggleSections()
        }

        setInitialHeader()
        applyFocusHeader()
        applyDefaultExpandedState()
        syncToggleSections()
        loadPlayerProfile()
        loadPlayerForms()
        loadWeek()
    }

    private fun setInitialHeader() {
        b.tvPlayerTitle.text =
            if (playerName.isBlank()) "Player Overview" else "$playerName Athlete Overview"
        b.tvPlayerEmail.text = playerEmail
        applyStatusToUi("Green", "On track", false)
    }

    private fun applyFocusHeader() {
        when (selectedFocus) {
            "wellness" -> {
                b.tvFocusHeader.text = "Wellness Focus"
                b.tvFocusSubtitle.text =
                    "Latest wellness check-in and recent wellness history for this athlete."
            }
            "risk" -> {
                b.tvFocusHeader.text = "At-Risk Focus"
                b.tvFocusSubtitle.text =
                    "Current standing, latest at-risk details, and recent at-risk history for this athlete."
            }
            else -> {
                b.tvFocusHeader.text = "Athlete Assignment Overview"
                b.tvFocusSubtitle.text =
                    "Full athlete profile including standing, calendar, wellness, risk history, and recent submitted forms."
            }
        }
    }

    private fun applyDefaultExpandedState() {
        when (selectedFocus) {
            "wellness" -> {
                showingWellnessHistory = true
                showingRiskHistory = false
                showingAllForms = false
            }
            "risk" -> {
                showingWellnessHistory = false
                showingRiskHistory = true
                showingAllForms = false
            }
            else -> {
                showingWellnessHistory = false
                showingRiskHistory = false
                showingAllForms = false
            }
        }
    }

    private fun syncToggleSections() {
        b.layoutPastWellnessSection.visibility =
            if (showingWellnessHistory) View.VISIBLE else View.GONE
        b.layoutPastRiskSection.visibility =
            if (showingRiskHistory) View.VISIBLE else View.GONE
        b.layoutAllFormsSection.visibility =
            if (showingAllForms) View.VISIBLE else View.GONE

        b.tvToggleWellnessHistory.text =
            if (showingWellnessHistory) "Hide Wellness History" else "View Wellness History"
        b.tvToggleRiskHistory.text =
            if (showingRiskHistory) "Hide At-Risk History" else "View At-Risk History"
        b.tvToggleAllForms.text =
            if (showingAllForms) "Hide All Forms" else "View All Forms"
    }

    private fun loadPlayerProfile() {
        if (playerUid.isBlank()) return

        db.collection("users").document(playerUid)
            .get()
            .addOnSuccessListener { doc ->
                val firstName = doc.getString("firstName").orEmpty()
                val lastName = doc.getString("lastName").orEmpty()
                val fullName = "$firstName $lastName".trim()

                val displayName = when {
                    fullName.isNotBlank() -> fullName
                    playerName.isNotBlank() -> playerName
                    playerEmail.isNotBlank() -> playerEmail
                    else -> "Player"
                }

                val displayEmail = doc.getString("email").orEmpty().ifBlank { playerEmail }

                b.tvPlayerTitle.text = "$displayName Athlete Overview"
                b.tvPlayerEmail.text = displayEmail

                val automaticStatus = calculateAutomaticStatus(
                    doc.getDouble("gpa"),
                    doc.getLong("missedPractices")?.toInt() ?: 0,
                    doc.getLong("missedAssignments")?.toInt() ?: 0,
                    doc.getLong("attendanceIssues")?.toInt() ?: 0
                )

                val manualStatus = doc.getString("manualStatus").orEmpty()
                val manualReason = doc.getString("manualStatusReason").orEmpty()

                if (manualStatus.isNotBlank()) {
                    applyStatusToUi(
                        manualStatus,
                        manualReason.ifBlank { "Coach override" },
                        true
                    )
                } else {
                    applyStatusToUi(
                        automaticStatus.label,
                        automaticStatus.reason,
                        false
                    )
                }
            }
            .addOnFailureListener {
                setInitialHeader()
            }
    }

    private fun showStatusOptionsDialog() {
        val options = arrayOf(
            "Set Green",
            "Set Yellow",
            "Set Red",
            "Clear manual override"
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Update Athlete Status")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showReasonDialogForStatus("Green")
                    1 -> showReasonDialogForStatus("Yellow")
                    2 -> showReasonDialogForStatus("Red")
                    3 -> clearManualStatus()
                }
            }
            .show()
    }

    private fun showReasonDialogForStatus(status: String) {
        val reasonInput = EditText(requireContext()).apply {
            hint = "Reason"
        }

        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            val pad = dp(16)
            setPadding(pad, pad, pad, pad)
            addView(reasonInput)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Set $status Status")
            .setView(container)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                val reason = reasonInput.text.toString().trim()
                saveManualStatus(status, reason)
            }
            .show()
    }

    private fun saveManualStatus(status: String, reason: String) {
        val data = hashMapOf<String, Any>(
            "manualStatus" to status,
            "manualStatusReason" to reason,
            "manualStatusUpdatedAt" to Timestamp.now()
        )

        db.collection("users").document(playerUid)
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                applyStatusToUi(status, reason.ifBlank { "Coach override" }, true)
                Toast.makeText(requireContext(), "Status updated", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Could not update status: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun clearManualStatus() {
        val data = hashMapOf<String, Any>(
            "manualStatus" to "",
            "manualStatusReason" to "",
            "manualStatusUpdatedAt" to Timestamp.now()
        )

        db.collection("users").document(playerUid)
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                loadPlayerProfile()
                Toast.makeText(requireContext(), "Manual override cleared", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Could not clear override: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun applyStatusToUi(status: String, reason: String, isManual: Boolean) {
        val statusText = if (isManual) {
            "$status - Coach Set"
        } else {
            when (status) {
                "Green" -> "Green - Good Standing"
                "Yellow" -> "Yellow - At Risk"
                "Red" -> "Red - High Risk"
                else -> status
            }
        }

        val colorRes = when (status) {
            "Green" -> android.R.color.holo_green_light
            "Yellow" -> android.R.color.holo_orange_light
            "Red" -> android.R.color.holo_red_light
            else -> android.R.color.darker_gray
        }

        b.tvStatusValue.text = statusText
        b.tvStatusReason.text = reason
        b.tvStatusValue.setTextColor(ContextCompat.getColor(requireContext(), colorRes))
    }

    private fun calculateAutomaticStatus(
        gpa: Double?,
        missedPractices: Int,
        missedAssignments: Int,
        attendanceIssues: Int
    ): StatusInfo {
        return when {
            missedPractices >= 3 ||
                    missedAssignments >= 3 ||
                    attendanceIssues >= 2 ||
                    ((gpa ?: 0.0) > 0.0 && (gpa ?: 0.0) < 2.5) -> {
                StatusInfo(
                    "Red",
                    when {
                        missedPractices >= 3 -> "3 or more missed practices"
                        missedAssignments >= 3 -> "3 or more missed assignments"
                        attendanceIssues >= 2 -> "Multiple attendance issues"
                        else -> "GPA below 2.5"
                    }
                )
            }

            missedPractices >= 2 ||
                    missedAssignments >= 2 ||
                    attendanceIssues >= 1 ||
                    ((gpa ?: 0.0) > 0.0 && (gpa ?: 0.0) < 3.0) -> {
                StatusInfo(
                    "Yellow",
                    when {
                        missedPractices >= 2 -> "2 missed practices"
                        missedAssignments >= 2 -> "2 missed assignments"
                        attendanceIssues >= 1 -> "Attendance concern"
                        else -> "GPA below 3.0"
                    }
                )
            }

            else -> StatusInfo("Green", "On track")
        }
    }

    private fun loadPlayerForms() {
        if (playerUid.isBlank()) return

        db.collection("forms")
            .whereEqualTo("userId", playerUid)
            .get()
            .addOnSuccessListener { qs ->
                val docs = qs.documents

                val wellnessDocs = docs
                    .filter {
                        it.getString("formType") == "wellness" &&
                                it.getString("status") != "requested"
                    }
                    .sortedByDescending {
                        it.getTimestamp("submittedAt")?.toDate()?.time
                            ?: it.getTimestamp("createdAt")?.toDate()?.time
                            ?: 0L
                    }

                val riskDocs = docs
                    .filter {
                        it.getString("formType") == "atRisk" &&
                                it.getString("status") != "requested"
                    }
                    .sortedByDescending {
                        it.getTimestamp("submittedAt")?.toDate()?.time
                            ?: it.getTimestamp("createdAt")?.toDate()?.time
                            ?: 0L
                    }

                val allSubmittedDocs = docs
                    .filter { it.getString("status") != "requested" }
                    .sortedByDescending {
                        it.getTimestamp("submittedAt")?.toDate()?.time
                            ?: it.getTimestamp("createdAt")?.toDate()?.time
                            ?: 0L
                    }

                val recentSubmittedDocs = allSubmittedDocs.take(3)

                renderWellnessSection(wellnessDocs)
                renderRiskSection(riskDocs)
                renderRecentFormsSection(recentSubmittedDocs)
                renderAllFormsSection(allSubmittedDocs)
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Could not load player forms: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun renderWellnessSection(wellnessDocs: List<DocumentSnapshot>) {
        b.layoutLatestWellness.removeAllViews()
        b.layoutPastWellness.removeAllViews()

        if (wellnessDocs.isEmpty()) {
            b.tvWellnessSummary.text = "No wellness check-ins submitted yet."
            addInfoRow(b.layoutLatestWellness, "Latest Wellness", "No wellness check-in found.")
            addInfoRow(b.layoutPastWellness, "Past Wellness", "No older wellness check-ins yet.")
            return
        }

        val latest = wellnessDocs.first()
        val past = wellnessDocs.drop(1).take(3)

        val mood = latest.getString("mood").orEmpty()
        val energy = latest.getString("energyLevel").orEmpty()
        val stress = latest.getString("stressLevel").orEmpty()
        val sleepHours = latest.getString("sleepHours").orEmpty()
        val soreness = latest.getString("sorenessLevel").orEmpty()
        val notes = latest.getString("notes").orEmpty()

        val label = calculateWellnessLabel(mood, energy, stress, soreness, sleepHours)
        b.tvWellnessSummary.text = "Latest check-in: ${formatTimestamp(latest)} • $label"

        addInfoRow(b.layoutLatestWellness, "Submitted", formatTimestamp(latest))
        addInfoRow(
            b.layoutLatestWellness,
            "Coach Summary",
            buildWellnessCoachSummary(label, mood, energy, stress, sleepHours, soreness)
        )
        addInfoRow(b.layoutLatestWellness, "Mood", mood)
        addInfoRow(b.layoutLatestWellness, "Energy", energy)
        addInfoRow(b.layoutLatestWellness, "Stress", stress)
        addInfoRow(b.layoutLatestWellness, "Sleep Hours", sleepHours)
        addInfoRow(b.layoutLatestWellness, "Body Feeling / Soreness", soreness)
        addInfoRow(b.layoutLatestWellness, "Notes", notes.ifBlank { "—" })

        if (past.isEmpty()) {
            addInfoRow(b.layoutPastWellness, "Past Wellness", "No older wellness check-ins yet.")
        } else {
            past.forEachIndexed { index, doc ->
                addCardBlock(
                    b.layoutPastWellness,
                    "Past Wellness #${index + 1}",
                    listOf(
                        "Submitted: ${formatTimestamp(doc)}",
                        "Mood: ${doc.getString("mood").orEmpty().ifBlank { "—" }}",
                        "Energy: ${doc.getString("energyLevel").orEmpty().ifBlank { "—" }}",
                        "Stress: ${doc.getString("stressLevel").orEmpty().ifBlank { "—" }}",
                        "Sleep Hours: ${doc.getString("sleepHours").orEmpty().ifBlank { "—" }}",
                        "Soreness: ${doc.getString("sorenessLevel").orEmpty().ifBlank { "—" }}",
                        "Notes: ${doc.getString("notes").orEmpty().ifBlank { "—" }}"
                    )
                )
            }
        }
    }

    private fun renderRiskSection(riskDocs: List<DocumentSnapshot>) {
        b.layoutLatestRisk.removeAllViews()
        b.layoutPastRisk.removeAllViews()

        if (riskDocs.isEmpty()) {
            b.tvRiskSummary.text = "No at-risk form submissions found."
            addInfoRow(b.layoutLatestRisk, "Latest Risk Submission", "No at-risk submission found.")
            addInfoRow(b.layoutPastRisk, "Past At-Risk", "No older at-risk submissions yet.")
            return
        }

        val latest = riskDocs.first()
        val past = riskDocs.drop(1).take(3)
        val data = latest.get("data") as? Map<*, *>

        val impact = data?.get("impactLevel")?.toString().orEmpty()
        val concernArea = data?.get("concernArea")?.toString().orEmpty()
        val alertType = data?.get("alertType")?.toString().orEmpty()
        val followUp = data?.get("followUpNeeded")?.toString().orEmpty()
        val observed = data?.get("observedBehavior")?.toString().orEmpty()
        val actions = data?.get("actionsTaken")?.toString().orEmpty()
        val message = data?.get("summaryMessage")?.toString().orEmpty()

        b.tvRiskSummary.text = "Latest risk submission: ${formatTimestamp(latest)}"

        addInfoRow(b.layoutLatestRisk, "Submitted", formatTimestamp(latest))
        addInfoRow(b.layoutLatestRisk, "Alert Type", alertType.ifBlank { "—" })
        addInfoRow(b.layoutLatestRisk, "Concern Area", concernArea.ifBlank { "—" })
        addInfoRow(b.layoutLatestRisk, "Impact Level", impact.ifBlank { "—" })
        addInfoRow(b.layoutLatestRisk, "Observed Behavior", observed.ifBlank { "—" })
        addInfoRow(b.layoutLatestRisk, "Actions Taken", actions.ifBlank { "—" })
        addInfoRow(b.layoutLatestRisk, "Follow-Up Needed", followUp.ifBlank { "—" })
        addInfoRow(b.layoutLatestRisk, "Summary", message.ifBlank { "—" })

        if (past.isEmpty()) {
            addInfoRow(b.layoutPastRisk, "Past At-Risk", "No older at-risk submissions yet.")
        } else {
            past.forEachIndexed { index, doc ->
                val map = doc.get("data") as? Map<*, *>
                addCardBlock(
                    b.layoutPastRisk,
                    "Past At-Risk #${index + 1}",
                    listOf(
                        "Submitted: ${formatTimestamp(doc)}",
                        "Alert Type: ${map?.get("alertType")?.toString().orEmpty().ifBlank { "—" }}",
                        "Concern Area: ${map?.get("concernArea")?.toString().orEmpty().ifBlank { "—" }}",
                        "Impact Level: ${map?.get("impactLevel")?.toString().orEmpty().ifBlank { "—" }}",
                        "Follow-Up Needed: ${map?.get("followUpNeeded")?.toString().orEmpty().ifBlank { "—" }}",
                        "Summary: ${map?.get("summaryMessage")?.toString().orEmpty().ifBlank { "—" }}"
                    )
                )
            }
        }
    }

    private fun renderRecentFormsSection(docs: List<DocumentSnapshot>) {
        b.layoutRecentForms.removeAllViews()

        if (docs.isEmpty()) {
            addInfoRow(b.layoutRecentForms, "Recent Forms", "No submitted forms yet.")
            return
        }

        docs.forEachIndexed { index, doc ->
            val type = prettifyFormType(doc.getString("formType").orEmpty())
            val status = doc.getString("status").orEmpty().replace("_", " ")
            addCardBlock(
                b.layoutRecentForms,
                "Submission #${index + 1}",
                listOf(
                    "Type: $type",
                    "Status: $status",
                    "Submitted: ${formatTimestamp(doc)}"
                )
            )
        }
    }

    private fun renderAllFormsSection(docs: List<DocumentSnapshot>) {
        b.layoutAllForms.removeAllViews()

        if (docs.isEmpty()) {
            addInfoRow(b.layoutAllForms, "All Forms", "No submitted forms yet.")
            return
        }

        docs.forEachIndexed { index, doc ->
            val type = prettifyFormType(doc.getString("formType").orEmpty())
            val status = doc.getString("status").orEmpty().replace("_", " ")
            addCardBlock(
                b.layoutAllForms,
                "Submission #${index + 1}",
                listOf(
                    "Type: $type",
                    "Status: $status",
                    "Submitted: ${formatTimestamp(doc)}"
                )
            )
        }
    }

    private fun calculateWellnessLabel(
        mood: String,
        energy: String,
        stress: String,
        soreness: String,
        sleepHours: String
    ): String {
        var concernPoints = 0

        if (mood.equals("Drained", true) || mood.equals("Stressed", true)) concernPoints += 2
        else if (mood.equals("Tired", true) || mood.equals("Okay", true)) concernPoints += 1

        if (energy.equals("Low", true)) concernPoints += 2
        else if (energy.equals("Medium", true)) concernPoints += 1

        if (stress.equals("High", true)) concernPoints += 2
        else if (stress.equals("Medium", true)) concernPoints += 1

        if (soreness.equals("High", true)) concernPoints += 2
        else if (soreness.equals("Moderate", true)) concernPoints += 1

        val sleepValue = sleepHours.toDoubleOrNull()
        if (sleepValue != null) {
            if (sleepValue < 5.5) concernPoints += 2
            else if (sleepValue < 7.0) concernPoints += 1
        }

        return when {
            concernPoints >= 5 -> "Concern"
            concernPoints >= 2 -> "Watch"
            else -> "Good"
        }
    }

    private fun buildWellnessCoachSummary(
        label: String,
        mood: String,
        energy: String,
        stress: String,
        sleepHours: String,
        soreness: String
    ): String {
        val lead = when (label) {
            "Concern" -> "Coach should follow up."
            "Watch" -> "Keep an eye on this athlete."
            else -> "Athlete looks fine right now."
        }

        return "$lead Mood: $mood. Energy: $energy. Stress: $stress. Sleep: $sleepHours. Soreness: $soreness."
    }

    private fun addInfoRow(container: LinearLayout, label: String, value: String) {
        val block = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_spinner_white)
            val inner = dp(12)
            setPadding(inner, inner, inner, inner)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(10)
            }
        }

        val title = TextView(requireContext()).apply {
            text = label
            textSize = 13f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.dt_black))
        }

        val body = TextView(requireContext()).apply {
            text = value.ifBlank { "—" }
            textSize = 15f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.dt_black))
            setPadding(0, dp(4), 0, 0)
        }

        block.addView(title)
        block.addView(body)
        container.addView(block)
    }

    private fun addCardBlock(container: LinearLayout, title: String, lines: List<String>) {
        val block = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_spinner_white)
            val inner = dp(12)
            setPadding(inner, inner, inner, inner)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(10)
            }
        }

        val titleView = TextView(requireContext()).apply {
            text = title
            textSize = 15f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.dt_black))
        }
        block.addView(titleView)

        lines.forEach { line ->
            val textView = TextView(requireContext()).apply {
                text = line
                textSize = 14f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.dt_black))
                setPadding(0, dp(4), 0, 0)
            }
            block.addView(textView)
        }

        container.addView(block)
    }

    private fun formatTimestamp(doc: DocumentSnapshot): String {
        val ts = doc.getTimestamp("submittedAt") ?: doc.getTimestamp("createdAt")
        val date = ts?.toDate() ?: return "No date"
        val fmt = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.US)
        return fmt.format(date)
    }

    private fun prettifyFormType(value: String): String {
        return when (value) {
            "academic" -> "Academic Check"
            "wellness" -> "Wellness Check"
            "atRisk" -> "At-Risk Alert"
            "absence" -> "Absence Form"
            "injury" -> "Injury Report"
            else -> value.replaceFirstChar { it.uppercase() }
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun loadWeek() {
        if (playerUid.isBlank()) {
            renderWeek(emptyList())
            return
        }

        db.collection("users")
            .document(playerUid)
            .collection("events")
            .get()
            .addOnSuccessListener { qs ->
                renderWeek(qs.documents)
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Could not load player calendar: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                renderWeek(emptyList())
            }
    }

    private fun renderWeek(docs: List<DocumentSnapshot>) {
        val startCal = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }

        val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

        val cards = labels.mapIndexed { index, label ->
            val dayCal = startCal.clone() as Calendar
            dayCal.add(Calendar.DAY_OF_MONTH, index)
            val dayMillis = dayCal.timeInMillis

            val matching = docs.filter { doc ->
                val storedDayMillis = doc.getLong("dayMillis")
                storedDayMillis == dayMillis
            }.sortedBy { it.getString("time").orEmpty() }

            val subtitle = when {
                matching.isEmpty() -> "No events"
                matching.size == 1 -> {
                    val first = matching.first()
                    val time = first.getString("time").orEmpty()
                    val title = first.getString("title").orEmpty()
                    val details = first.getString("details").orEmpty()
                    buildString {
                        if (time.isNotBlank()) append("$time - ")
                        append(title)
                        if (details.isNotBlank()) append(" • $details")
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

            HomeCard(label, subtitle)
        }

        b.rvWeek.adapter = HomeCardAdapter(cards) { card ->
            Toast.makeText(requireContext(), "${card.title}: ${card.subtitle}", Toast.LENGTH_LONG).show()
        }

        b.tvWeekRange.text = buildWeekRangeText(startCal.time)
    }

    private fun buildWeekRangeText(startDate: Date): String {
        val cal = Calendar.getInstance().apply {
            time = startDate
        }
        val endCal = cal.clone() as Calendar
        endCal.add(Calendar.DAY_OF_MONTH, 6)

        val fmt = SimpleDateFormat("MMM d", Locale.US)
        return "${fmt.format(cal.time)} - ${fmt.format(endCal.time)}"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}

private data class StatusInfo(
    val label: String,
    val reason: String
)