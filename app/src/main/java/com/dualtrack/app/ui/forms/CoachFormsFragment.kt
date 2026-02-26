package com.dualtrack.app.ui.forms

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.dualtrack.app.databinding.FragmentCoachFormsBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class CoachFormsFragment : Fragment() {

    private var _b: FragmentCoachFormsBinding? = null
    private val b get() = _b!!

    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _b = FragmentCoachFormsBinding.inflate(inflater, container, false)

        b.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        loadTeamForms()

        return b.root
    }

    private fun loadTeamForms() {
        db.collection("forms")
            .whereEqualTo("teamId", "TEMP_TEAM_ID")
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

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}