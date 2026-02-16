package com.dualtrack.app.ui.forms

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
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

        setupSubmitButton()

        return b.root
    }

    private fun setupSubmitButton() {
        b.btnSubmitAbsence.setOnClickListener {
            submitAbsenceForm()
        }
    }

    private fun submitAbsenceForm() {
        val reason = b.etReason.text.toString().trim()
        val date = b.etDate.text.toString().trim()
        val notes = b.etNotes.text.toString().trim()

        val user = auth.currentUser
        if (user == null) {
            // User not logged in → do nothing or show error
            return
        }

        val formData = hashMapOf(
            "formType" to "absence",
            "athleteId" to user.uid,
            "teamId" to "TEMP_TEAM_ID", // TODO: replace once roster works
            "submittedAt" to Timestamp.now(),
            "data" to mapOf(
                "reason" to reason,
                "date" to date,
                "notes" to notes
            )
        )

        db.collection("com/dualtrack/app/ui/forms")
            .add(formData)
            .addOnSuccessListener {
                b.btnSubmitAbsence.text = "Submitted ✓"
            }
            .addOnFailureListener {
                b.btnSubmitAbsence.text = "Error"
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
