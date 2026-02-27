package com.dualtrack.app.ui.forms

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.dualtrack.app.databinding.FragmentAbsenceFormBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AbsenceFormFragment : Fragment() {

    private var _b: FragmentAbsenceFormBinding? = null
    private val b get() = _b!!

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _b = FragmentAbsenceFormBinding.inflate(inflater, container, false)

        b.btnBack.setOnClickListener { findNavController().navigateUp() }
        b.btnSubmitAbsence.setOnClickListener { submitAbsenceForm() }

        return b.root
    }

    private fun submitAbsenceForm() {
        val reason = b.etReason.text.toString().trim()
        val date = b.etDate.text.toString().trim()
        val notes = b.etNotes.text.toString().trim()

        if (reason.isBlank() || date.isBlank()) {
            Toast.makeText(requireContext(), "Please enter reason and date.", Toast.LENGTH_SHORT).show()
            return
        }

        val user = auth.currentUser ?: run {
            Toast.makeText(requireContext(), "Not logged in.", Toast.LENGTH_SHORT).show()
            return
        }

        b.btnSubmitAbsence.isEnabled = false
        b.btnSubmitAbsence.text = "Submitting..."

        db.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { snap ->
                val teamId = snap.getString("teamId") ?: "TEMP_TEAM_ID"
                val teamName = snap.getString("teamName") ?: ""

                val formData = hashMapOf(
                    "formType" to "absence",
                    "userId" to user.uid,
                    "userEmail" to user.email,
                    "teamId" to teamId,
                    "teamName" to teamName,
                    "createdAt" to Timestamp.now(),
                    "status" to "pending",
                    "coachNote" to "",
                    "data" to mapOf(
                        "reason" to reason,
                        "date" to date,
                        "notes" to notes
                    )
                )

                db.collection("forms")
                    .add(formData)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Submitted ✓", Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    }
                    .addOnFailureListener {
                        b.btnSubmitAbsence.isEnabled = true
                        b.btnSubmitAbsence.text = "Submit Absence Request"
                        Toast.makeText(requireContext(), "Error submitting.", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                b.btnSubmitAbsence.isEnabled = true
                b.btnSubmitAbsence.text = "Submit Absence Request"
                Toast.makeText(requireContext(), "Could not load team.", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}