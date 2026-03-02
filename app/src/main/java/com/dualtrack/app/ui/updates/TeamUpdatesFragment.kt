package com.dualtrack.app.ui.updates

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.dualtrack.app.databinding.FragmentTeamUpdatesBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class TeamUpdatesFragment : Fragment() {

    private var _b: FragmentTeamUpdatesBinding? = null
    private val b get() = _b!!

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private var annReg: ListenerRegistration? = null
    private var evReg: ListenerRegistration? = null

    private val adapter = TeamUpdatesAdapter()

    private var annList: List<TeamUpdate> = emptyList()
    private var evList: List<TeamUpdate> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = FragmentTeamUpdatesBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        b.btnBack.setOnClickListener { findNavController().navigateUp() }

        b.rvUpdates.layoutManager = LinearLayoutManager(requireContext())
        b.rvUpdates.adapter = adapter

        listen()
    }

    private fun listen() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid).get()
            .addOnSuccessListener { snap ->
                val teamId = snap.getString("teamId") ?: return@addOnSuccessListener

                annReg?.remove()
                evReg?.remove()

                annReg = db.collection("announcements")
                    .whereEqualTo("teamId", teamId)
                    .addSnapshotListener { qs, _ ->
                        annList = qs?.documents.orEmpty().mapNotNull { d ->
                            val title = d.getString("title") ?: return@mapNotNull null
                            val msg = d.getString("message") ?: ""
                            TeamUpdate(d.id, "announcement", title, msg, d.getTimestamp("createdAt"))
                        }
                        render()
                    }

                evReg = db.collection("teamEvents")
                    .whereEqualTo("teamId", teamId)
                    .addSnapshotListener { qs, _ ->
                        evList = qs?.documents.orEmpty().mapNotNull { d ->
                            val title = d.getString("title") ?: return@mapNotNull null
                            val details = d.getString("details") ?: ""
                            val date = d.getString("eventDate") ?: ""
                            val subtitle = listOf(date, details).filter { it.isNotBlank() }.joinToString(" • ")
                            TeamUpdate(d.id, "event", title, subtitle, d.getTimestamp("createdAt"))
                        }
                        render()
                    }
            }
    }

    private fun render() {
        val combined = (annList + evList)
            .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }

        adapter.submit(combined)

        val empty = combined.isEmpty()
        b.tvEmpty.visibility = if (empty) View.VISIBLE else View.GONE
        b.rvUpdates.visibility = if (empty) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        annReg?.remove()
        evReg?.remove()
        annReg = null
        evReg = null
        _b = null
    }
}