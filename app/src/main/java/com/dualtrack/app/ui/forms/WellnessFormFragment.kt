package com.dualtrack.app.ui.forms

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
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
        b.btnSubmitWellness.setOnClickListener { submit() }
        return b.root
    }

    private fun submit() {
        val mood = b.etMood.text.toString().trim()
        val energy = b.etEnergy.text.toString().trim()

        val user = auth.currentUser ?: return

        val data = hashMapOf(
            "formType" to "wellness",
            "userId" to user.uid,
            "userEmail" to user.email,
            "teamId" to "TEMP_TEAM_ID",
            "createdAt" to Timestamp.now(),
            "status" to "pending",
            "data" to mapOf("mood" to mood, "energy" to energy)
        )

        db.collection("forms").add(data).addOnSuccessListener {
            b.btnSubmitWellness.text = "Submitted ✓"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}