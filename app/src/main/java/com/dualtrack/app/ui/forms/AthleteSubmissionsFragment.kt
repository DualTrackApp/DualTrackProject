package com.dualtrack.app.ui.forms

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.dualtrack.app.databinding.FragmentAthleteSubmissionsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class AthleteSubmissionsFragment : Fragment() {

    private var _b: FragmentAthleteSubmissionsBinding? = null
    private val b get() = _b!!

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _b = FragmentAthleteSubmissionsBinding.inflate(inflater, container, false)

        b.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        loadSubmissions()

        return b.root
    }

    private fun loadSubmissions() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("forms")
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener

                val items = snapshot.documents.map {
                    FormItem(
                        id = it.id,
                        formType = it.getString("formType") ?: "",
                        status = it.getString("status") ?: "pending",
                        createdAt = it.getTimestamp("createdAt")?.toDate().toString()
                    )
                }

                b.recyclerView.adapter = AthleteFormsAdapter(items)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}