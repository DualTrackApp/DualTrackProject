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

data class RosterAthlete(
    val uid: String,
    val email: String,
    val firstName: String,
    val lastName: String
) {
    fun fullName(): String {
        val full = "$firstName $lastName".trim()
        return if (full.isBlank()) email else full
    }
}

class CoachTeamFragment : Fragment() {

    private var _b: FragmentCoachTeamBinding? = null
    private val b get() = _b!!

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private lateinit var adapter: CoachPlayerAdapter
    private val rosterAthletes = mutableListOf<RosterAthlete>()

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

        adapter = CoachPlayerAdapter { athlete ->
            openPlayerCalendar(athlete)
        }

        b.rvPlayers.layoutManager = LinearLayoutManager(requireContext())
        b.rvPlayers.adapter = adapter

        loadPlayers()
    }

    private fun loadPlayers() {
        val coachUid = auth.currentUser?.uid ?: return

        db.collection("users").document(coachUid).get()
            .addOnSuccessListener { coachSnap ->
                val teamId = coachSnap.getString("teamId") ?: return@addOnSuccessListener

                db.collection("teams").document(teamId)
                    .collection("roster")
                    .get()
                    .addOnSuccessListener { rosterSnap ->
                        val ids = rosterSnap.documents.map { it.id }.distinct()

                        if (ids.isEmpty()) {
                            rosterAthletes.clear()
                            adapter.submit(emptyList())
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

                                    val firstName = userDoc.getString("firstName") ?: ""
                                    val lastName = userDoc.getString("lastName") ?: ""

                                    if (email.isNotBlank()) {
                                        loaded.add(
                                            RosterAthlete(
                                                uid = athleteUid,
                                                email = email,
                                                firstName = firstName,
                                                lastName = lastName
                                            )
                                        )
                                    }

                                    remaining--
                                    if (remaining == 0) {
                                        showLoadedPlayers(loaded)
                                    }
                                }
                                .addOnFailureListener {
                                    remaining--
                                    if (remaining == 0) {
                                        showLoadedPlayers(loaded)
                                    }
                                }
                        }
                    }
            }
    }

    private fun showLoadedPlayers(loaded: List<RosterAthlete>) {
        rosterAthletes.clear()
        rosterAthletes.addAll(
            loaded.sortedBy { it.fullName().lowercase() }
        )
        adapter.submit(rosterAthletes)
    }

    private fun openPlayerCalendar(athlete: RosterAthlete) {
        val bundle = Bundle().apply {
            putString("playerUid", athlete.uid)
            putString("playerEmail", athlete.email)
            putString("playerName", athlete.fullName())
        }
        findNavController().navigate(R.id.action_coachTeam_to_playerCalendar, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
