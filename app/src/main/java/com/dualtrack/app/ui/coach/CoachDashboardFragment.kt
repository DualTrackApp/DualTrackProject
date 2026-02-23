package com.dualtrack.app.ui.coach

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import com.dualtrack.app.databinding.FragmentCoachDashboardBinding
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

    private fun refreshCoachTeamCache() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { snap ->
                cachedTeamId = snap.getString("teamId")
                cachedTeamName = snap.getString("teamName")
            }
    }

    private fun showManageTeamDialog() {
        val hasTeam = !cachedTeamId.isNullOrBlank()

        val options = if (hasTeam) {
            arrayOf("Add athlete to team", "Create a new team")
        } else {
            arrayOf("Create a team")
        }

        AlertDialog.Builder(requireContext())
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

    private fun showCreateTeamDialog() {
        val input = EditText(requireContext())
        input.hint = "Enter team name (ex: Baseball)"

        AlertDialog.Builder(requireContext())
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

    private fun createTeam(teamName: String) {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val teamRef = db.collection("teams").document()

        val teamData = hashMapOf(
            "teamId" to teamRef.id,
            "teamName" to teamName,
            "coachId" to uid,
            "createdAt" to Timestamp.now()
        )

        teamRef.set(teamData)
            .addOnSuccessListener {
                val coachUpdate = hashMapOf(
                    "teamId" to teamRef.id,
                    "teamName" to teamName,
                    "updatedAt" to Timestamp.now()
                )

                db.collection("users").document(uid)
                    .set(coachUpdate, SetOptions.merge())
                    .addOnSuccessListener {
                        cachedTeamId = teamRef.id
                        cachedTeamName = teamName
                        Toast.makeText(requireContext(), "Team created: $teamName", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Team created, but coach user update failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to create team: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showAddAthleteDialog() {
        val teamId = cachedTeamId
        val teamName = cachedTeamName
        val coachId = auth.currentUser?.uid

        if (coachId.isNullOrBlank() || teamId.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Create a team first.", Toast.LENGTH_SHORT).show()
            return
        }

        val input = EditText(requireContext())
        input.hint = "Athlete email or UID"

        AlertDialog.Builder(requireContext())
            .setTitle("Add Athlete")
            .setMessage("Assign an athlete to ${teamName ?: "your team"}.")
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Assign", null)
            .create()
            .also { dialog ->
                dialog.setOnShowListener {
                    val assignBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    assignBtn.isEnabled = false

                    input.doAfterTextChanged {
                        assignBtn.isEnabled = !it.isNullOrBlank()
                    }

                    assignBtn.setOnClickListener {
                        val key = input.text.toString().trim()
                        if (key.isBlank()) return@setOnClickListener

                        if (key.contains("@")) {
                            assignAthleteByEmail(
                                email = key,
                                teamId = teamId,
                                teamName = teamName,
                                coachId = coachId,
                                onDone = { ok -> if (ok) dialog.dismiss() }
                            )
                        } else {
                            assignAthleteByUid(
                                athleteUid = key,
                                teamId = teamId,
                                teamName = teamName,
                                coachId = coachId,
                                onDone = { ok -> if (ok) dialog.dismiss() }
                            )
                        }
                    }
                }
                dialog.show()
            }
    }

    private fun assignAthleteByEmail(
        email: String,
        teamId: String,
        teamName: String?,
        coachId: String,
        onDone: (Boolean) -> Unit
    ) {
        db.collection("users")
            .whereEqualTo("email", email)
            .limit(1)
            .get()
            .addOnSuccessListener { qs ->
                val doc = qs.documents.firstOrNull()
                if (doc == null) {
                    // Optional: invitation pending state
                    createInvite(email, teamId, teamName, coachId)
                    Toast.makeText(requireContext(), "Athlete not found yet. Created invite for $email", Toast.LENGTH_LONG).show()
                    onDone(true)
                    return@addOnSuccessListener
                }

                assignAthleteByUid(doc.id, teamId, teamName, coachId, onDone)
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Lookup failed: ${e.message}", Toast.LENGTH_LONG).show()
                onDone(false)
            }
    }

    private fun assignAthleteByUid(
        athleteUid: String,
        teamId: String,
        teamName: String?,
        coachId: String,
        onDone: (Boolean) -> Unit
    ) {
        val userRef = db.collection("users").document(athleteUid)

        userRef.get()
            .addOnSuccessListener { snap ->
                if (!snap.exists()) {
                    Toast.makeText(requireContext(), "No user doc for UID: $athleteUid", Toast.LENGTH_LONG).show()
                    onDone(false)
                    return@addOnSuccessListener
                }

                val role = snap.getString("role")
                if (!role.isNullOrBlank() && role.lowercase() != "athlete") {
                    Toast.makeText(requireContext(), "That user is not an athlete (role=$role)", Toast.LENGTH_LONG).show()
                    onDone(false)
                    return@addOnSuccessListener
                }

                val update = hashMapOf<String, Any>(
                    "teamId" to teamId,
                    "updatedAt" to Timestamp.now(),
                    "assignedAt" to Timestamp.now(),
                    "assignedBy" to coachId
                )
                if (!teamName.isNullOrBlank()) update["teamName"] = teamName

                userRef.set(update, SetOptions.merge())
                    .addOnSuccessListener {
                        // Also write to teams/{teamId}/roster/{userId}
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
                                onDone(true)
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(requireContext(), "Assigned user, but roster write failed: ${e.message}", Toast.LENGTH_LONG).show()
                                onDone(true)
                            }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Assign failed: ${e.message}", Toast.LENGTH_LONG).show()
                        onDone(false)
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Read failed: ${e.message}", Toast.LENGTH_LONG).show()
                onDone(false)
            }
    }

    private fun createInvite(email: String, teamId: String, teamName: String?, coachId: String) {
        val inviteId = "${teamId}_${email.lowercase()}"
        val data = hashMapOf<String, Any>(
            "inviteId" to inviteId,
            "email" to email,
            "teamId" to teamId,
            "coachId" to coachId,
            "status" to "pending",
            "createdAt" to Timestamp.now()
        )
        if (!teamName.isNullOrBlank()) data["teamName"] = teamName

        db.collection("teamInvites").document(inviteId)
            .set(data, SetOptions.merge())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}