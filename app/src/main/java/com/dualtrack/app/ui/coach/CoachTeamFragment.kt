package com.dualtrack.app.ui.coach

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.dualtrack.app.R
import com.dualtrack.app.databinding.FragmentCoachTeamBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CoachTeamFragment : Fragment() {

    private var _b: FragmentCoachTeamBinding? = null
    private val b get() = _b!!

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private lateinit var adapter: CoachEmailAdapter
    private val rosterAthletes = mutableListOf<RosterAthlete>()

    private data class RosterAthlete(
        val uid: String,
        val email: String
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _b = FragmentCoachTeamBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        b.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        adapter = CoachEmailAdapter { email ->
            val athlete = rosterAthletes.firstOrNull { it.email == email } ?: return@CoachEmailAdapter
            openPlayerCalendar(athlete)
        }

        b.rvPlayers.layoutManager = LinearLayoutManager(requireContext())
        b.rvPlayers.adapter = adapter

        loadPlayers()
    }

    private fun loadPlayers() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid).get()
            .addOnSuccessListener { snap ->
                val teamId = snap.getString("teamId") ?: return@addOnSuccessListener

                db.collection("teams").document(teamId)
                    .collection("roster")
                    .get()
                    .addOnSuccessListener { rosterSnap ->
                        val ids = rosterSnap.documents.map { it.id }.distinct()

                        if (ids.isEmpty()) {
                            rosterAthletes.clear()
                            adapter.submit(listOf("No players"))
                            return@addOnSuccessListener
                        }

                        val loaded = mutableListOf<RosterAthlete>()
                        var remaining = ids.size

                        ids.forEach { athleteUid ->
                            db.collection("users").document(athleteUid)
                                .get()
                                .addOnSuccessListener { userDoc ->
                                    val email = userDoc.getString("email")
                                        ?: userDoc.getString("userEmail")
                                        ?: ""

                                    if (email.isNotBlank()) {
                                        loaded.add(RosterAthlete(athleteUid, email))
                                    }

                                    remaining--
                                    if (remaining == 0) {
                                        rosterAthletes.clear()
                                        rosterAthletes.addAll(loaded.sortedBy { it.email })
                                        adapter.submit(rosterAthletes.map { it.email })
                                    }
                                }
                                .addOnFailureListener {
                                    remaining--
                                    if (remaining == 0) {
                                        rosterAthletes.clear()
                                        rosterAthletes.addAll(loaded.sortedBy { it.email })
                                        adapter.submit(rosterAthletes.map { it.email })
                                    }
                                }
                        }
                    }
            }
    }

    private fun openPlayerCalendar(athlete: RosterAthlete) {
        val bundle = Bundle().apply {
            putString("playerUid", athlete.uid)
            putString("playerEmail", athlete.email)
        }
        findNavController().navigate(R.id.action_coachTeam_to_playerCalendar, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
