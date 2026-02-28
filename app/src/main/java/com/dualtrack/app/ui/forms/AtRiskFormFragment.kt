package com.dualtrack.app.ui.forms

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.dualtrack.app.databinding.FragmentAtRiskFormBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AtRiskFormFragment : Fragment() {

    private var _b: FragmentAtRiskFormBinding? = null
    private val b get() = _b!!

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _b = FragmentAtRiskFormBinding.inflate(inflater, container, false)

        b.btnBack.setOnClickListener { findNavController().navigateUp() }
        b.btnSubmitAtRisk.setOnClickListener { submitAtRisk() }

        return b.root
    }

    private fun submitAtRisk() {
        val alertType = b.spAlertType.selectedItem?.toString()?.trim().orEmpty()
        val message = b.etMessage.text.toString().trim()

        if (alertType.isBlank() || alertType == "Select type") {
            Toast.makeText(requireContext(), "Pick an alert type.", Toast.LENGTH_SHORT).show()
            return
        }
        if (message.isBlank()) {
            Toast.makeText(requireContext(), "Enter a message.", Toast.LENGTH_SHORT).show()
            return
        }

        val user = auth.currentUser ?: run {
            Toast.makeText(requireContext(), "Not logged in.", Toast.LENGTH_SHORT).show()
            return
        }

        b.btnSubmitAtRisk.isEnabled = false
        b.btnSubmitAtRisk.text = "Submitting..."

        // Pull teamId like your other forms do
        db.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { snap ->
                val teamId = snap.getString("teamId") ?: "TEMP_TEAM_ID"
                val teamName = snap.getString("teamName") ?: ""

                val formData = hashMapOf(
                    "formType" to "atRisk",
                    "status" to "pending",
                    "createdAt" to Timestamp.now(),
                    "userEmail" to user.email,
                    "userId" to user.uid,
                    "teamId" to teamId,
                    "teamName" to teamName,
                    "coachNote" to "",
                    "data" to mapOf(
                        "alertType" to alertType,
                        "message" to message
                    )
                )

                db.collection("forms")
                    .add(formData)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "At-Risk Alert submitted ✓", Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    }
                    .addOnFailureListener { e ->
                        b.btnSubmitAtRisk.isEnabled = true
                        b.btnSubmitAtRisk.text = "Submit Alert"
                        Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                b.btnSubmitAtRisk.isEnabled = true
                b.btnSubmitAtRisk.text = "Submit Alert"
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}