package com.dualtrack.app.ui.coach

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.dualtrack.app.databinding.FragmentCoachDashboardBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class CoachDashboardFragment : Fragment() {

    private var _b: FragmentCoachDashboardBinding? = null
    private val b get() = _b!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var cachedTeamId: String? = null
    private var cachedTeamName: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _b = FragmentCoachDashboardBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        refreshCoachTeamCache()

        b.btnManageTeam.setOnClickListener {
            showManageTeamDialog()
        }
    }


    private fun refreshCoachTeamCache(onDone: (() -> Unit)? = null) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { snap ->
                cachedTeamId = snap.getString("teamId")
                cachedTeamName = snap.getString("teamName")
                onDone?.invoke()
            }
            .addOnFailureListener {
                // not fatal, but means manage options might show only create
                onDone?.invoke()
            }
    }


    private fun showManageTeamDialog() {
        refreshCoachTeamCache {
            val hasTeam = !cachedTeamId.isNullOrBlank()

            val options = if (hasTeam) {
                arrayOf("Add athlete to team", "Create a new team")
            } else {
                arrayOf("Create a team")
            }

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(if (hasTeam) "Manage Team" else "Team Setup")
                .setItems(options) { _, which ->
                    if (!hasTeam) {
                        showCreateTeamDialog()
                        return@setItems
                    }
                    when (which) {
                        0 -> showAddAthleteDialog()
                        1 -> showCreateTeamDialog()
                    }
                }
                .show()
        }
    }

    private fun showCreateTeamDialog() {
        val input = EditText(requireContext()).apply {
            hint = "Enter team name (ex: Baseball)"
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Create Team")
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Create") { _, _ ->
                val teamName = input.text.toString().trim()
                if (teamName.isBlank()) {
                    Toast.makeText(requireContext(), "Team name is required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                createTeam(teamName)
            }
            .show()
    }

    /**
     * Requirement:
     * teams/{teamId} -> teamName, coachId, createdAt
     */
    private fun createTeam(teamName: String) {
        val coachId = auth.currentUser?.uid
        if (coachId.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val teamRef = db.collection("teams").document()

        val teamData = hashMapOf(
            "teamId" to teamRef.id,
            "teamName" to teamName,
            "coachId" to coachId,
            "createdAt" to Timestamp.now()
        )

        teamRef.set(teamData)
            .addOnSuccessListener {
                // store coach’s teamId/teamName on user doc for quick lookup
                val coachUpdate = hashMapOf(
                    "teamId" to teamRef.id,
                    "teamName" to teamName,
                    "updatedAt" to Timestamp.now()
                )

                db.collection("users").document(coachId)
                    .set(coachUpdate, SetOptions.merge())
                    .addOnSuccessListener {
                        cachedTeamId = teamRef.id
                        cachedTeamName = teamName
                        Toast.makeText(requireContext(), "Team created: $teamName", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Team created, but coach update failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to create team: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    /**
     * Coach assigns athlete by email or UID.
     * - If input has "@" -> search users by email
     * - else treat as UID
     */
    private fun showAddAthleteDialog() {
        val teamId = cachedTeamId
        val teamName = cachedTeamName
        val coachId = auth.currentUser?.uid

        if (coachId.isNullOrBlank() || teamId.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Create a team first.", Toast.LENGTH_SHORT).show()
            return
        }

        val input = EditText(requireContext()).apply {
            hint = "Athlete email or UID"
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add Athlete")
            .setMessage("Assign an athlete to ${teamName ?: "your team"}")
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Assign") { _, _ ->
                val key = input.text.toString().trim()
                if (key.isBlank()) return@setPositiveButton

                if (key.contains("@")) {
                    assignAthleteByEmail(
                        email = key,
                        teamId = teamId,
                        teamName = teamName,
                        coachId = coachId
                    )
                } else {
                    assignAthleteByUid(
                        athleteUid = key,
                        teamId = teamId,
                        teamName = teamName,
                        coachId = coachId
                    )
                }
            }
            .show()
    }

    private fun assignAthleteByEmail(
        email: String,
        teamId: String,
        teamName: String?,
        coachId: String
    ) {
        db.collection("users")
            .whereEqualTo("email", email)
            .limit(1)
            .get()
            .addOnSuccessListener { qs ->
                val doc = qs.documents.firstOrNull()
                if (doc == null) {
                    Toast.makeText(
                        requireContext(),
                        "No athlete found for $email (they must register first)",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addOnSuccessListener
                }

                assignAthleteByUid(
                    athleteUid = doc.id,
                    teamId = teamId,
                    teamName = teamName,
                    coachId = coachId
                )
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Lookup failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    /**
     * Requirement:
     * When coach adds athlete -> create assignment:
     * - update users/{athleteUid} with teamId
     * - AND create teams/{teamId}/roster/{athleteUid} doc
     */
    private fun assignAthleteByUid(
        athleteUid: String,
        teamId: String,
        teamName: String?,
        coachId: String
    ) {
        val userRef = db.collection("users").document(athleteUid)

        userRef.get()
            .addOnSuccessListener { snap ->
                if (!snap.exists()) {
                    Toast.makeText(requireContext(), "No user doc for UID: $athleteUid", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                // update athlete user doc
                val update = hashMapOf<String, Any>(
                    "teamId" to teamId,
                    "assignedAt" to Timestamp.now(),
                    "assignedBy" to coachId,
                    "updatedAt" to Timestamp.now()
                )
                if (!teamName.isNullOrBlank()) update["teamName"] = teamName

                userRef.set(update, SetOptions.merge())
                    .addOnSuccessListener {
                        // write roster doc under team
                        val rosterData = hashMapOf(
                            "teamId" to teamId,
                            "userId" to athleteUid,
                            "assignedAt" to Timestamp.now(),
                            "assignedBy" to coachId
                        )

                        db.collection("teams").document(teamId)
                            .collection("roster").document(athleteUid)
                            .set(rosterData, SetOptions.merge())
                            .addOnSuccessListener {
                                Toast.makeText(requireContext(), "Athlete assigned!", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    requireContext(),
                                    "Assigned user, but roster write failed: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Assign failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Read failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}