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
import com.google.firebase.auth.UserProfileChangeRequest

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
            b.etEmailRegister.error = "Email is required"
            return
        }
        if (!isValidVSUEmail(email)) {
            b.etEmailRegister.error = "Use your VSU email (@vsu.edu or @students.vsu.edu)"
            return
        }

        if (password.isEmpty()) {
            b.etPasswordRegister.error = "Password is required"
            return
        }
        if (!isStrongPassword(password)) {
            b.etPasswordRegister.error =
                "Password must be 8+ chars with upper, lower, number, and symbol"
            return
        }

        if (confirmPassword.isEmpty()) {
            b.etConfirmPasswordRegister.error = "Confirm your password"
            return
        }
        if (confirmPassword != password) {
            b.etConfirmPasswordRegister.error = "Passwords do not match"
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                val user = auth.currentUser

                // Store the role in the user's Firebase Auth profile
                if (user != null && role.isNotBlank()) {
                    val updates = UserProfileChangeRequest.Builder()
                        .setDisplayName(role)
                        .build()
                    user.updateProfile(updates)
                }

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

    private fun isValidVSUEmail(email: String): Boolean {
        return email.endsWith("@vsu.edu", ignoreCase = true) ||
                email.endsWith("@students.vsu.edu", ignoreCase = true)
    }

    private fun isStrongPassword(password: String): Boolean {
        if (password.length < 8) return false

        var hasUpper = false
        var hasLower = false
        var hasDigit = false
        var hasSymbol = false

        for (c in password) {
            when {
                c.isUpperCase() -> hasUpper = true
                c.isLowerCase() -> hasLower = true
                c.isDigit() -> hasDigit = true
                !c.isLetterOrDigit() -> hasSymbol = true
            }
        }

        return hasUpper && hasLower && hasDigit && hasSymbol
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
