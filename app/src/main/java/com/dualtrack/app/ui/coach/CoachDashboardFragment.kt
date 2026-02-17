package com.dualtrack.app.ui.coach

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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

        b.btnManageTeam.setOnClickListener {
            showCreateTeamDialog()
        }
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

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
