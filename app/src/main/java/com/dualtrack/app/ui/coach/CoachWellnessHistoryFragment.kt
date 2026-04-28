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
import com.dualtrack.app.databinding.FragmentCoachWellnessHistoryBinding
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class CoachWellnessHistoryFragment : Fragment() {

    private var _b: FragmentCoachWellnessHistoryBinding? = null
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
        _b = FragmentCoachWellnessHistoryBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        b.btnBack.setOnClickListener { findNavController().navigateUp() }
        b.tvPlayerTitle.text =
            if (playerName.isBlank()) "Wellness History" else "$playerName Wellness History"
        b.tvPlayerEmail.text = playerEmail
        loadWellnessHistory()
    }

    private fun loadWellnessHistory() {
        db.collection("forms")
            .whereEqualTo("userId", playerUid)
            .whereEqualTo("formType", "wellness")
            .get()
            .addOnSuccessListener { qs ->
                val docs = qs.documents
                    .filter { it.getString("status") != "requested" }
                    .sortedByDescending {
                        it.getTimestamp("submittedAt")?.toDate()?.time
                            ?: it.getTimestamp("createdAt")?.toDate()?.time
                            ?: 0L
                    }

                renderWellness(docs)
            }
    }

    private fun renderWellness(docs: List<DocumentSnapshot>) {
        b.layoutLatestWellness.removeAllViews()
        b.layoutPastWellness.removeAllViews()

        if (docs.isEmpty()) {
            addCard(
                b.layoutLatestWellness,
                "Latest Wellness",
                listOf("No wellness submissions found.")
            )
            return
        }

        val latest = docs.first()
        val past = docs.drop(1)

        addCard(
            b.layoutLatestWellness,
            "Latest Wellness Submission",
            buildWellnessLines(latest)
        )

        if (past.isEmpty()) {
            addCard(
                b.layoutPastWellness,
                "Past Wellness",
                listOf("No older wellness submissions.")
            )
        } else {
            past.forEachIndexed { index, doc ->
                addCard(
                    b.layoutPastWellness,
                    "Past Wellness #${index + 1}",
                    buildWellnessLines(doc)
                )
            }
        }
    }

    private fun buildWellnessLines(doc: DocumentSnapshot): List<String> {
        val data = doc.get("data") as? Map<*, *>

        val mood = doc.getString("mood").orEmpty()
            .ifBlank { data?.get("mood")?.toString().orEmpty() }
        val energy = doc.getString("energyLevel").orEmpty()
            .ifBlank { data?.get("energyLevel")?.toString().orEmpty() }
        val stress = doc.getString("stressLevel").orEmpty()
            .ifBlank { data?.get("stressLevel")?.toString().orEmpty() }
        val sleep = doc.getString("sleepHours").orEmpty()
            .ifBlank { data?.get("sleepHours")?.toString().orEmpty() }
        val soreness = doc.getString("sorenessLevel").orEmpty()
            .ifBlank { data?.get("sorenessLevel")?.toString().orEmpty() }
        val notes = doc.getString("notes").orEmpty()
            .ifBlank { data?.get("notes")?.toString().orEmpty() }

        return listOf(
            "Submitted: ${formatTimestamp(doc)}",
            "Mood: ${mood.ifBlank { "—" }}",
            "Energy: ${energy.ifBlank { "—" }}",
            "Stress: ${stress.ifBlank { "—" }}",
            "Sleep Hours: ${sleep.ifBlank { "—" }}",
            "Soreness: ${soreness.ifBlank { "—" }}",
            "Notes: ${notes.ifBlank { "—" }}"
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
