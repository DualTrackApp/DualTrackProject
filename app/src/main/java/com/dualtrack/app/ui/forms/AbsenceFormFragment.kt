package com.dualtrack.app.ui.forms

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
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

        setupSpinners()

        b.btnBack.setOnClickListener { findNavController().navigateUp() }
        b.btnSubmitAbsence.setOnClickListener { submitAbsenceForm() }

        return b.root
    }

    private fun setupSpinners() {
        val absenceTypes = listOf(
            "Select absence type",
            "Class Conflict",
            "Medical",
            "Family Emergency",
            "Travel",
            "Personal",
            "Other"
        )

        val urgencyOptions = listOf(
            "Select urgency",
            "Low",
            "Moderate",
            "High"
        )

        b.spAbsenceType.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            absenceTypes
        ).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        b.spUrgency.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            urgencyOptions
        ).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    private fun submitAbsenceForm() {
        val absenceType = b.spAbsenceType.selectedItem?.toString()?.trim().orEmpty()
        val reason = b.etReason.text.toString().trim()
        val startDate = b.etStartDate.text.toString().trim()
        val endDate = b.etEndDate.text.toString().trim()
        val classMissed = b.etClassMissed.text.toString().trim()
        val practiceMissed = b.etPracticeMissed.text.toString().trim()
        val urgency = b.spUrgency.selectedItem?.toString()?.trim().orEmpty()
        val contactMethod = b.etContactMethod.text.toString().trim()
        val additionalNotes = b.etNotes.text.toString().trim()

        if (absenceType == "Select absence type") {
            Toast.makeText(requireContext(), "Please select an absence type.", Toast.LENGTH_SHORT).show()
            return
        }

        if (reason.isBlank() || startDate.isBlank() || endDate.isBlank()) {
            Toast.makeText(requireContext(), "Please complete the required fields.", Toast.LENGTH_SHORT).show()
            return
        }

        if (urgency == "Select urgency") {
            Toast.makeText(requireContext(), "Please select an urgency level.", Toast.LENGTH_SHORT).show()
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
                        "absenceType" to absenceType,
                        "reason" to reason,
                        "startDate" to startDate,
                        "endDate" to endDate,
                        "classMissed" to classMissed,
                        "practiceMissed" to practiceMissed,
                        "urgency" to urgency,
                        "contactMethod" to contactMethod,
                        "additionalNotes" to additionalNotes
                    )
                )

                db.collection("forms")
                    .add(formData)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Absence form submitted ✓", Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    }
                    .addOnFailureListener {
                        b.btnSubmitAbsence.isEnabled = true
                        b.btnSubmitAbsence.text = "Submit Absence Form"
                        Toast.makeText(requireContext(), "Error submitting.", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                b.btnSubmitAbsence.isEnabled = true
                b.btnSubmitAbsence.text = "Submit Absence Form"
                Toast.makeText(requireContext(), "Could not load team.", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
