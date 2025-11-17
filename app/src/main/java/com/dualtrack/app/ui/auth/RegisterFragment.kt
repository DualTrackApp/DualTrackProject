package com.dualtrack.app.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.dualtrack.app.R
import com.dualtrack.app.databinding.FragmentRegisterBinding
import com.google.firebase.auth.FirebaseAuth

class RegisterFragment : Fragment() {

    private var _b: FragmentRegisterBinding? = null
    private val b get() = _b!!
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _b = FragmentRegisterBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val roles = listOf("Athlete", "Coach")
        val roleAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, roles)
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        b.spRoleRegister.adapter = roleAdapter

        val teams = listOf("Team 1", "Team 2", "Team 3")
        val teamAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, teams)
        teamAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        b.spTeamRegister.adapter = teamAdapter

        b.btnContinueRegister.setOnClickListener { registerUser() }

        b.btnBackRegister.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun registerUser() {
        val email = b.etEmailRegister.text.toString().trim()
        val password = b.etPasswordRegister.text.toString().trim()
        val confirmPassword = b.etConfirmPasswordRegister.text.toString().trim()
        val role = b.spRoleRegister.selectedItem?.toString() ?: ""

        if (email.isEmpty()) {
            b.etEmailRegister.error = "Required"
            return
        }
        if (!email.endsWith("@vsu.edu")) {
            b.etEmailRegister.error = "Use your VSU email"
            return
        }
        if (password.isEmpty()) {
            b.etPasswordRegister.error = "Required"
            return
        }
        if (password.length < 6) {
            b.etPasswordRegister.error = "Minimum 6 characters"
            return
        }
        if (confirmPassword != password) {
            b.etConfirmPasswordRegister.error = "Passwords do not match"
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                if (role == "Coach") {
                    findNavController().navigate(R.id.action_register_to_coachHome)
                } else {
                    findNavController().navigate(R.id.action_register_to_athleteHome)
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
