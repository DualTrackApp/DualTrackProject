package com.dualtrack.app.ui.forms

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.dualtrack.app.databinding.FragmentInjuryFormBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class InjuryFormFragment : Fragment() {

    private var _b: FragmentInjuryFormBinding? = null
    private val b get() = _b!!

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _b = FragmentInjuryFormBinding.inflate(inflater, container, false)

        b.btnSubmitInjury.setOnClickListener { submit() }
        return b.root
    }

    private fun submit() {
        val injury = b.etInjury.text.toString().trim()
        val severity = b.etSeverity.text.toString().trim()

        val user = auth.currentUser ?: return

        val data = hashMapOf(
            "formType" to "injury",
            "userId" to user.uid,
            "userEmail" to user.email,
            "teamId" to "TEMP_TEAM_ID",
            "createdAt" to Timestamp.now(),
            "status" to "pending",
            "data" to mapOf(
                "injury" to injury,
                "severity" to severity
            )
        )

        db.collection("forms").add(data).addOnSuccessListener {
            b.btnSubmitInjury.text = "Submitted ✓"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}