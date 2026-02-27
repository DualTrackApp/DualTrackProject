package com.dualtrack.app.ui.forms

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.dualtrack.app.databinding.FragmentCoachFormsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class CoachFormsFragment : Fragment() {

    private var _b: FragmentCoachFormsBinding? = null
    private val b get() = _b!!

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = FragmentCoachFormsBinding.inflate(inflater, container, false)

        b.btnBack.setOnClickListener { findNavController().navigateUp() }
        b.recyclerView.layoutManager = LinearLayoutManager(requireContext())

        loadTeamForms()

        return b.root
    }

    private fun loadTeamForms() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { snap ->
                val teamId = snap.getString("teamId") ?: "TEMP_TEAM_ID"

                db.collection("forms")
                    .whereEqualTo("teamId", teamId)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshot, _ ->
                        if (snapshot == null) return@addSnapshotListener

                        val items = snapshot.documents.map {
                            CoachFormItem(
                                id = it.id,
                                athleteEmail = it.getString("userEmail") ?: "",
                                formType = it.getString("formType") ?: "",
                                status = it.getString("status") ?: "pending"
                            )
                        }

                        b.recyclerView.adapter = CoachFormsAdapter(items)
                    }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}