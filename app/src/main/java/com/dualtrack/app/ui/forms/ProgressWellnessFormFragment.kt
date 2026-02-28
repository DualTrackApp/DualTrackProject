package com.dualtrack.app.ui.forms

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.dualtrack.app.databinding.FragmentProgressWellnessFormBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProgressWellnessFormFragment : Fragment() {

    private var _b: FragmentProgressWellnessFormBinding? = null
    private val b get() = _b!!

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _b = FragmentProgressWellnessFormBinding.inflate(inflater, container, false)

        val cardTitle = arguments?.getString("cardTitle") ?: "Progress & Wellness"

        b.btnBack.setOnClickListener { findNavController().navigateUp() }

        // Title/subtitle
        b.tvTitle.text = cardTitle
        b.tvSubtitle.text = when (cardTitle) {
            "Completion Chart" -> "Log your weekly completion (quick check-in)"
            "Wellness Diaries" -> "Log a short diary entry for today"
            "Eligibility Flags" -> "Flag anything that might affect eligibility"
            else -> ""
        }

        // Setup spinner only when needed
        if (cardTitle == "Eligibility Flags") {
            b.spType.visibility = View.VISIBLE
            val types = listOf("Select type", "Academic", "Attendance", "Behavior", "Medical", "Other")
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, types)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            b.spType.adapter = adapter
        } else {
            b.spType.visibility = View.GONE
        }

        // Value input used for Completion Chart (optional numeric)
        b.etValue.hint = when (cardTitle) {
            "Completion Chart" -> "Completion % (ex: 85)"
            else -> "Optional number (leave blank)"
        }

        b.btnSubmit.setOnClickListener { submit(cardTitle) }

        return b.root
    }

    private fun submit(cardTitle: String) {
        val user = auth.currentUser ?: run {
            Toast.makeText(requireContext(), "Not logged in.", Toast.LENGTH_SHORT).show()
            return
        }

        val message = b.etMessage.text.toString().trim()
        val value = b.etValue.text.toString().trim()

        if (message.isBlank()) {
            Toast.makeText(requireContext(), "Enter a short note.", Toast.LENGTH_SHORT).show()
            return
        }

        // Eligibility flags needs type chosen
        var selectedType = ""
        if (cardTitle == "Eligibility Flags") {
            selectedType = b.spType.selectedItem?.toString()?.trim().orEmpty()
            if (selectedType.isBlank() || selectedType == "Select type") {
                Toast.makeText(requireContext(), "Pick a flag type.", Toast.LENGTH_SHORT).show()
                return
            }
        }

        b.btnSubmit.isEnabled = false
        b.btnSubmit.text = "Submitting..."

        db.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { snap ->
                val teamId = snap.getString("teamId") ?: "TEMP_TEAM_ID"
                val teamName = snap.getString("teamName") ?: ""

                // Form type names (simple + filterable)
                val formType = when (cardTitle) {
                    "Completion Chart" -> "progressCompletion"
                    "Wellness Diaries" -> "wellnessDiary"
                    "Eligibility Flags" -> "eligibilityFlag"
                    else -> "progressWellness"
                }

                val dataMap = hashMapOf<String, Any>(
                    "cardTitle" to cardTitle,
                    "message" to message
                )
                if (value.isNotBlank()) dataMap["value"] = value
                if (selectedType.isNotBlank()) dataMap["flagType"] = selectedType

                val formDoc = hashMapOf(
                    "formType" to formType,
                    "status" to "pending",
                    "createdAt" to Timestamp.now(),
                    "userEmail" to user.email,
                    "userId" to user.uid,
                    "teamId" to teamId,
                    "teamName" to teamName,
                    "coachNote" to "",
                    "data" to dataMap
                )

                db.collection("forms")
                    .add(formDoc)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Submitted ✓", Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    }
                    .addOnFailureListener { e ->
                        b.btnSubmit.isEnabled = true
                        b.btnSubmit.text = "Submit"
                        Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                b.btnSubmit.isEnabled = true
                b.btnSubmit.text = "Submit"
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}