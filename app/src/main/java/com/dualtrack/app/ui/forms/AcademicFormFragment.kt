package com.dualtrack.app.ui.forms

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.dualtrack.app.databinding.FragmentAcademicFormBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AcademicFormFragment : Fragment() {

    private var _b: FragmentAcademicFormBinding? = null
    private val b get() = _b!!

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = FragmentAcademicFormBinding.inflate(inflater, container, false)
        b.btnSubmitAcademic.setOnClickListener { submit() }
        return b.root
    }

    private fun submit() {
        val classes = b.etClasses.text.toString().trim()
        val gpa = b.etGpa.text.toString().trim()

        val user = auth.currentUser ?: return

        val data = hashMapOf(
            "formType" to "academic",
            "userId" to user.uid,
            "userEmail" to user.email,
            "teamId" to "TEMP_TEAM_ID",
            "createdAt" to Timestamp.now(),
            "status" to "pending",
            "data" to mapOf("classes" to classes, "gpa" to gpa)
        )

        db.collection("forms").add(data).addOnSuccessListener {
            b.btnSubmitAcademic.text = "Submitted ✓"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}