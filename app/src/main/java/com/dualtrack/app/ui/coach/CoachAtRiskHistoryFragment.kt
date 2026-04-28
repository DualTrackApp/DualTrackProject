package com.dualtrack.app.ui.coach

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.dualtrack.app.R
import com.dualtrack.app.databinding.FragmentCoachAtRiskHistoryBinding
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class CoachAtRiskHistoryFragment : Fragment() {

    private var _b: FragmentCoachAtRiskHistoryBinding? = null
    private val b get() = _b!!

    private val db by lazy { FirebaseFirestore.getInstance() }

    private var playerUid = ""
    private var playerEmail = ""
    private var playerName = ""

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
        _b = FragmentCoachAtRiskHistoryBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        b.btnBack.setOnClickListener { findNavController().navigateUp() }
        b.tvPlayerTitle.text =
            if (playerName.isBlank()) "At-Risk History" else "$playerName At-Risk History"
        b.tvPlayerEmail.text = playerEmail
        loadRiskHistory()
    }

    private fun loadRiskHistory() {
        db.collection("forms")
            .whereEqualTo("userId", playerUid)
            .whereEqualTo("formType", "atRisk")
            .get()
            .addOnSuccessListener { qs ->
                val docs = qs.documents
                    .filter { it.getString("status") != "requested" }
                    .sortedByDescending {
                        it.getTimestamp("submittedAt")?.toDate()?.time
                            ?: it.getTimestamp("createdAt")?.toDate()?.time
                            ?: 0L
                    }

                renderRisk(docs)
            }
    }

    private fun renderRisk(docs: List<DocumentSnapshot>) {
        b.layoutLatestRisk.removeAllViews()
        b.layoutPastRisk.removeAllViews()

        if (docs.isEmpty()) {
            addCard(
                b.layoutLatestRisk,
                "Latest At-Risk Submission",
                listOf("No at-risk submissions found.")
            )
            return
        }

        val latest = docs.first()
        val past = docs.drop(1)

        addCard(
            b.layoutLatestRisk,
            "Latest At-Risk Submission",
            buildRiskLines(latest)
        )

        if (past.isEmpty()) {
            addCard(
                b.layoutPastRisk,
                "Past At-Risk",
                listOf("No older at-risk submissions.")
            )
        } else {
            past.forEachIndexed { index, doc ->
                addCard(
                    b.layoutPastRisk,
                    "Past At-Risk #${index + 1}",
                    buildRiskLines(doc)
                )
            }
        }
    }

    private fun buildRiskLines(doc: DocumentSnapshot): List<String> {
        val data = doc.get("data") as? Map<*, *>

        val alertType = data?.get("alertType")?.toString().orEmpty()
        val concernArea = data?.get("concernArea")?.toString().orEmpty()
        val impactLevel = data?.get("impactLevel")?.toString().orEmpty()
        val observedBehavior = data?.get("observedBehavior")?.toString().orEmpty()
        val actionsTaken = data?.get("actionsTaken")?.toString().orEmpty()
        val followUpNeeded = data?.get("followUpNeeded")?.toString().orEmpty()
        val summary = data?.get("summaryMessage")?.toString().orEmpty()

        return listOf(
            "Submitted: ${formatTimestamp(doc)}",
            "Alert Type: ${alertType.ifBlank { "—" }}",
            "Concern Area: ${concernArea.ifBlank { "—" }}",
            "Impact Level: ${impactLevel.ifBlank { "—" }}",
            "Observed Behavior: ${observedBehavior.ifBlank { "—" }}",
            "Actions Taken: ${actionsTaken.ifBlank { "—" }}",
            "Follow-Up Needed: ${followUpNeeded.ifBlank { "—" }}",
            "Summary: ${summary.ifBlank { "—" }}"
        )
    }

    private fun addCard(container: LinearLayout, title: String, lines: List<String>) {
        val block = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_spinner_white)
            val p = dp(14)
            setPadding(p, p, p, p)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(14)
            }
        }

        val titleView = TextView(requireContext()).apply {
            text = title
            textSize = 15f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.dt_black))
        }
        block.addView(titleView)

        lines.forEach { line ->
            val tv = TextView(requireContext()).apply {
                text = line
                textSize = 14f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.dt_black))
                setPadding(0, dp(6), 0, 0)
            }
            block.addView(tv)
        }

        container.addView(block)
    }

    private fun formatTimestamp(doc: DocumentSnapshot): String {
        val ts = doc.getTimestamp("submittedAt") ?: doc.getTimestamp("createdAt")
        val date = ts?.toDate() ?: return "No date"
        return SimpleDateFormat("MMM d, yyyy h:mm a", Locale.US).format(date)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
