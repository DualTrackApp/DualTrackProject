package com.dualtrack.app.ui.forms

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.dualtrack.app.databinding.FragmentWellnessFormBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class WellnessFormFragment : Fragment() {

    private var _b: FragmentWellnessFormBinding? = null
    private val b get() = _b!!

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _b = FragmentWellnessFormBinding.inflate(inflater, container, false)

        setupSpinners()

        b.btnBack.setOnClickListener { findNavController().navigateUp() }
        b.btnSubmitWellness.setOnClickListener { submit() }

        return b.root
    }

    private fun setupSpinners() {
        val moodOptions = listOf(
            "Select mood",
            "Good",
            "Okay",
            "Excited",
            "Tired",
            "Drained",
            "Stressed"
        )

        val energyOptions = listOf(
            "Select energy",
            "High",
            "Medium",
            "Low"
        )

        val stressOptions = listOf(
            "Select stress",
            "Low",
            "Medium",
            "High"
        )

        val sorenessOptions = listOf(
            "Select body feeling",
            "None",
            "Mild",
            "Moderate",
            "High"
        )

        b.spMood.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            moodOptions
        ).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        b.spEnergyLevel.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            energyOptions
        ).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        b.spStressLevel.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            stressOptions
        ).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        b.spSorenessLevel.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            sorenessOptions
        ).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    private fun submit() {
        val mood = b.spMood.selectedItem?.toString()?.trim().orEmpty()
        val energy = b.spEnergyLevel.selectedItem?.toString()?.trim().orEmpty()
        val stress = b.spStressLevel.selectedItem?.toString()?.trim().orEmpty()
        val soreness = b.spSorenessLevel.selectedItem?.toString()?.trim().orEmpty()
        val sleepHours = b.etSleepHours.text.toString().trim()
        val notes = b.etNotes.text.toString().trim()

        if (mood == "Select mood" ||
            energy == "Select energy" ||
            stress == "Select stress" ||
            soreness == "Select body feeling" ||
            sleepHours.isBlank()
        ) {
            Toast.makeText(requireContext(), "Please complete all required wellness fields.", Toast.LENGTH_SHORT).show()
            return
        }

        val user = auth.currentUser ?: run {
            Toast.makeText(requireContext(), "Not logged in.", Toast.LENGTH_SHORT).show()
            return
        }

        val requestId = arguments?.getString("requestId").orEmpty()

        b.btnSubmitWellness.isEnabled = false
        b.btnSubmitWellness.text = "Submitting..."

        db.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { snap ->
                val teamId = snap.getString("teamId").orEmpty()
                val teamName = snap.getString("teamName").orEmpty()

                val wellnessSummary = "$mood • $energy energy • $sleepHours hrs sleep"

                val formDoc = hashMapOf<String, Any>(
                    "formType" to "wellness",
                    "status" to "pending",
                    "submittedAt" to Timestamp.now(),
                    "createdAt" to Timestamp.now(),
                    "userEmail" to (user.email ?: ""),
                    "userId" to user.uid,
                    "teamId" to teamId,
                    "teamName" to teamName,
                    "coachNote" to "",
                    "athleteStatusSeen" to false,
                    "wellnessSummary" to wellnessSummary,
                    "mood" to mood,
                    "energyLevel" to energy,
                    "stressLevel" to stress,
                    "sleepHours" to sleepHours,
                    "sorenessLevel" to soreness,
                    "notes" to notes,
                    "data" to mapOf(
                        "mood" to mood,
                        "energyLevel" to energy,
                        "stressLevel" to stress,
                        "sleepHours" to sleepHours,
                        "sorenessLevel" to soreness,
                        "notes" to notes
                    )
                )

                if (requestId.isNotBlank()) {
                    db.collection("forms").document(requestId)
                        .set(formDoc, SetOptions.merge())
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Wellness check submitted ✓", Toast.LENGTH_SHORT).show()
                            findNavController().navigateUp()
                        }
                        .addOnFailureListener {
                            b.btnSubmitWellness.isEnabled = true
                            b.btnSubmitWellness.text = "Submit Wellness Check"
                            Toast.makeText(requireContext(), "Error submitting.", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    db.collection("forms")
                        .add(formDoc)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Wellness check submitted ✓", Toast.LENGTH_SHORT).show()
                            findNavController().navigateUp()
                        }
                        .addOnFailureListener {
                            b.btnSubmitWellness.isEnabled = true
                            b.btnSubmitWellness.text = "Submit Wellness Check"
                            Toast.makeText(requireContext(), "Error submitting.", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener {
                b.btnSubmitWellness.isEnabled = true
                b.btnSubmitWellness.text = "Submit Wellness Check"
                Toast.makeText(requireContext(), "Could not load team.", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
