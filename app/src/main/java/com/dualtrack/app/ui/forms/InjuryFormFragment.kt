package com.dualtrack.app.ui.forms

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.dualtrack.app.databinding.FragmentInjuryFormBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class InjuryFormFragment : Fragment() {

    private var _b: FragmentInjuryFormBinding? = null
    private val b get() = _b!!

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = FragmentInjuryFormBinding.inflate(inflater, container, false)

        b.btnBack.setOnClickListener { findNavController().navigateUp() }
        b.btnSubmitInjury.setOnClickListener { submit() }

        return b.root
    }

    private fun submit() {
        val injury = b.etInjury.text.toString().trim()
        val severity = b.etSeverity.text.toString().trim()

        if (injury.isBlank() || severity.isBlank()) {
            Toast.makeText(requireContext(), "Please enter injury and severity.", Toast.LENGTH_SHORT).show()
            return
        }

        val user = auth.currentUser ?: run {
            Toast.makeText(requireContext(), "Not logged in.", Toast.LENGTH_SHORT).show()
            return
        }

        b.btnSubmitInjury.isEnabled = false
        b.btnSubmitInjury.text = "Submitting..."

        db.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { snap ->
                val teamId = snap.getString("teamId") ?: "TEMP_TEAM_ID"
                val teamName = snap.getString("teamName") ?: ""

                val data = hashMapOf(
                    "formType" to "injury",
                    "userId" to user.uid,
                    "userEmail" to user.email,
                    "teamId" to teamId,
                    "teamName" to teamName,
                    "createdAt" to Timestamp.now(),
                    "status" to "pending",
                    "coachNote" to "",
                    "data" to mapOf(
                        "injury" to injury,
                        "severity" to severity
                    )
                )

                db.collection("forms").add(data)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Submitted ✓", Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    }
                    .addOnFailureListener {
                        b.btnSubmitInjury.isEnabled = true
                        b.btnSubmitInjury.text = "Submit Injury Report"
                        Toast.makeText(requireContext(), "Error submitting.", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                b.btnSubmitInjury.isEnabled = true
                b.btnSubmitInjury.text = "Submit Injury Report"
                Toast.makeText(requireContext(), "Could not load team.", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}