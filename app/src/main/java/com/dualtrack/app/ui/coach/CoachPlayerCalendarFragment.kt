package com.dualtrack.app.ui.coach

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playerUid = arguments?.getString("playerUid").orEmpty()
        playerEmail = arguments?.getString("playerEmail").orEmpty()
        playerName = arguments?.getString("playerName").orEmpty()
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
            findNavController().navigateUp()
        }

        b.rvWeek.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        b.tvStatusLabel.text = "Status (tap to update)"

        b.layoutStatusCard.setOnClickListener {
            if (playerUid.isBlank()) return@setOnClickListener
            showStatusOptionsDialog()
        }

        setInitialHeader()
        loadPlayerProfile()
        loadWeek()
    }

    private fun setInitialHeader() {
        b.tvPlayerTitle.text =
            if (playerName.isBlank()) "Player Weekly Calendar" else "$playerName Weekly Calendar"

        b.tvPlayerEmail.text = playerEmail
        applyStatusToUi("Green", "On track", false)
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

                b.tvPlayerTitle.text = "$displayName Weekly Calendar"
                b.tvPlayerEmail.text = displayEmail

                val automaticStatus = calculateAutomaticStatus(
                    gpa = doc.getDouble("gpa"),
                    missedPractices = doc.getLong("missedPractices")?.toInt() ?: 0,
                    missedAssignments = doc.getLong("missedAssignments")?.toInt() ?: 0,
                    attendanceIssues = doc.getLong("attendanceIssues")?.toInt() ?: 0
                )

                val manualStatus = doc.getString("manualStatus").orEmpty()
                val manualReason = doc.getString("manualStatusReason").orEmpty()

                if (manualStatus.isNotBlank()) {
                    applyStatusToUi(
                        status = manualStatus,
                        reason = manualReason.ifBlank { "Coach override" },
                        isManual = true
                    )
                } else {
                    applyStatusToUi(
                        status = automaticStatus.label,
                        reason = automaticStatus.reason,
                        isManual = false
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
            val pad = (16 * resources.displayMetrics.density).toInt()
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
                applyStatusToUi(
                    status = status,
                    reason = reason.ifBlank { "Coach override" },
                    isManual = true
                )
                Toast.makeText(requireContext(), "Status updated", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Could not update status: ${e.message}", Toast.LENGTH_LONG).show()
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
                Toast.makeText(requireContext(), "Could not clear override: ${e.message}", Toast.LENGTH_LONG).show()
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

            HomeCard(
                title = label,
                subtitle = subtitle
            )
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
