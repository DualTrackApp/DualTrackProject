package com.dualtrack.app.ui.forms

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.dualtrack.app.databinding.FragmentWellnessFormBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class WellnessFormFragment : Fragment() {

    private var _b: FragmentWellnessFormBinding? = null
    private val b get() = _b!!

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = FragmentWellnessFormBinding.inflate(inflater, container, false)

        b.btnBack.setOnClickListener { findNavController().navigateUp() }
        b.btnSubmitWellness.setOnClickListener { submit() }

        return b.root
    }

    private fun submit() {
        val mood = b.etMood.text.toString().trim()
        val energy = b.etEnergy.text.toString().trim()

        if (mood.isBlank() || energy.isBlank()) {
            Toast.makeText(requireContext(), "Please enter mood and energy.", Toast.LENGTH_SHORT).show()
            return
        }

        val user = auth.currentUser ?: run {
            Toast.makeText(requireContext(), "Not logged in.", Toast.LENGTH_SHORT).show()
            return
        }

        b.btnSubmitWellness.isEnabled = false
        b.btnSubmitWellness.text = "Submitting..."

        db.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { snap ->
                val teamId = snap.getString("teamId") ?: "TEMP_TEAM_ID"
                val teamName = snap.getString("teamName") ?: ""

                val data = hashMapOf(
                    "formType" to "wellness",
                    "userId" to user.uid,
                    "userEmail" to user.email,
                    "teamId" to teamId,
                    "teamName" to teamName,
                    "createdAt" to Timestamp.now(),
                    "status" to "pending",
                    "coachNote" to "",
                    "data" to mapOf(
                        "mood" to mood,
                        "energy" to energy
                    )
                )

                db.collection("forms").add(data)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Submitted ✓", Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    }
                    .addOnFailureListener {
                        b.btnSubmitWellness.isEnabled = true
                        b.btnSubmitWellness.text = "Submit Wellness Check"
                        Toast.makeText(requireContext(), "Error submitting.", Toast.LENGTH_SHORT).show()
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