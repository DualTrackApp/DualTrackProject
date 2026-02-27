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

        loadMyForms()

        b.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        return b.root
    }

    private fun loadMyForms() {
        val user = auth.currentUser ?: return

        db.collection("forms")
            .whereEqualTo("userId", user.uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener

                val items = snapshot.documents.map { doc ->
                    FormItem(
                        id = doc.id,
                        formType = doc.getString("formType") ?: "",
                        status = doc.getString("status") ?: "pending",
                        createdAt = doc.getTimestamp("createdAt")
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
